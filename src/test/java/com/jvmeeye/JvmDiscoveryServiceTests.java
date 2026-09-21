package com.jvmeeye;

import com.jvmeeye.config.JvmEyeProperties;
import com.jvmeeye.discovery.JvmDiscoveryService;
import com.jvmeeye.discovery.dto.JvmProcessInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发现服务:基于真实 Attach API(测试 JVM 自身就是目标之一)。
 *
 * <p>attach 到自身会失败,因此自身条目 attachable=false——这同时验证了异常分支。</p>
 */
class JvmDiscoveryServiceTests {

    private final JvmDiscoveryService service = new JvmDiscoveryService(new JvmEyeProperties());

    @Test
    void listsSelfWhenIncluded() {
        long selfPid = ProcessHandle.current().pid();
        List<JvmProcessInfo> all = service.list(true);
        assertFalse(all.isEmpty(), "至少应发现自身进程");
        JvmProcessInfo self = all.stream()
                .filter(info -> info.pid() == selfPid)
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到自身进程条目"));
        assertTrue(self.self(), "self 标记应为 true");
        assertFalse(self.attachable(), "不能 attach 到自身");
        assertTrue(self.reason() != null && !self.reason().isBlank(), "应给出不可 attach 的原因");
    }

    @Test
    void excludesSelfByDefault() {
        long selfPid = ProcessHandle.current().pid();
        List<JvmProcessInfo> visible = service.list();
        assertTrue(visible.stream().noneMatch(info -> info.pid() == selfPid),
                "默认应从发现列表中排除自身");
        for (JvmProcessInfo info : visible) {
            assertFalse(info.self(), "排除自身后不应再出现 self=true 的条目");
        }
    }
}
