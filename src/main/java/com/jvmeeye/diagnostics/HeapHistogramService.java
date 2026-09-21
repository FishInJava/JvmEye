package com.jvmeeye.diagnostics;

import com.jvmeeye.exception.ApiException;
import com.jvmeeye.monitor.JmxConnectionManager;
import com.jvmeeye.monitor.JmxTargetSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类直方图(class histogram)服务。
 *
 * <p>通过 DiagnosticCommand MBean({@code com.sun.management:type=DiagnosticCommand})
 * 调用 {@code gcClassHistogram}——等价于 {@code jcmd <pid> GC.class_histogram}。
 * <strong>注意:该命令会触发一次 Full GC。</strong></p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeapHistogramService {

    private static final Pattern ROW =
            Pattern.compile("^\\s*(\\d+):\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s*$");

    private static final Pattern TOTAL =
            Pattern.compile("^\\s*Total\\s+(\\d+)\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);

    private final JmxConnectionManager connectionManager;

    /** 默认返回前 30 个类。 */
    public static final int DEFAULT_TOP = 30;

    public Histogram histogram(Integer topParam) {
        int top = topParam == null ? DEFAULT_TOP : topParam;
        if (top < 1) {
            throw new IllegalArgumentException("top 必须 >= 1");
        }
        if (top > 500) {
            top = 500;
        }
        JmxTargetSession session = connectionManager.active();
        String output;
        try {
            output = invokeGcClassHistogram(session);
        } catch (IOException e) {
            session.markClosed("类直方图失败: " + e.getMessage());
            throw new ApiException(HttpStatus.CONFLICT.value(),
                    "目标 JVM 连接已断开(pid=" + session.getPid() + "): " + e.getMessage(), e);
        }
        ParsedHistogram parsed = parse(output);
        List<Histogram.Entry> entries = new ArrayList<>(parsed.entries());
        entries.sort(Comparator.comparingLong(Histogram.Entry::bytes).reversed());
        List<Histogram.Entry> topEntries = new ArrayList<>();
        for (int i = 0; i < Math.min(top, entries.size()); i++) {
            Histogram.Entry entry = entries.get(i);
            topEntries.add(new Histogram.Entry(i + 1, entry.name(), entry.instances(), entry.bytes()));
        }
        long totalInstances = parsed.totalInstances();
        long totalBytes = parsed.totalBytes();
        return new Histogram(System.currentTimeMillis(), session.getPid(), session.getDisplayName(),
                top, totalInstances, totalBytes, entries.size(), topEntries);
    }

    /** 调用 DiagnosticCommand.gcClassHistogram。 */
    public String invokeGcClassHistogram(JmxTargetSession session) throws IOException {
        MBeanServerConnection connection = session.getConnection();
        ObjectName name = session.getDiagnosticCommandName();
        boolean registered;
        try {
            registered = connection.isRegistered(name);
        } catch (IOException e) {
            registered = false;
        }
        if (!registered) {
            throw new ApiException(HttpStatus.CONFLICT.value(),
                    "目标 JVM 未提供 DiagnosticCommand MBean(需要 HotSpot JVM,pid=" + session.getPid() + ")");
        }
        try {
            return (String) connection.invoke(name, "gcClassHistogram",
                    new Object[]{new String[0]}, new String[]{String[].class.getName()});
        } catch (javax.management.InstanceNotFoundException e) {
            throw new ApiException(HttpStatus.CONFLICT.value(),
                    "目标 JVM 未提供 DiagnosticCommand MBean(pid=" + session.getPid() + ")", e);
        } catch (javax.management.ReflectionException | javax.management.MBeanException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "调用 gcClassHistogram 失败: " + e.getMessage(), e);
        }
    }

    /** 解析 jcmd GC.class_histogram 文本输出。 */
    public ParsedHistogram parse(String output) {
        List<Histogram.Entry> entries = new ArrayList<>();
        long totalInstances = -1;
        long totalBytes = -1;
        if (output == null) {
            return new ParsedHistogram(entries, totalInstances, totalBytes);
        }
        for (String line : output.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            Matcher totalMatcher = TOTAL.matcher(line);
            if (totalMatcher.matches()) {
                totalInstances = Long.parseLong(totalMatcher.group(1));
                totalBytes = Long.parseLong(totalMatcher.group(2));
                continue;
            }
            Matcher matcher = ROW.matcher(line);
            if (matcher.matches()) {
                long instances = Long.parseLong(matcher.group(2));
                long bytes = Long.parseLong(matcher.group(3));
                String className = matcher.group(4);
                while (className.endsWith(",") || className.endsWith(" ")) {
                    className = className.substring(0, className.length() - 1);
                }
                entries.add(new Histogram.Entry(0, className, instances, bytes));
            }
        }
        return new ParsedHistogram(entries, totalInstances, totalBytes);
    }

    /** 直方图结果。 */
    public record Histogram(long timestamp, long pid, String displayName, int top,
                            long totalInstances, long totalBytes, int totalClasses,
                            List<Entry> entries) {

        public record Entry(int rank, String name, long instances, long bytes) {
        }
    }

    /** 完整解析结果(未截断)。 */
    public record ParsedHistogram(List<Histogram.Entry> entries, long totalInstances, long totalBytes) {
    }
}
