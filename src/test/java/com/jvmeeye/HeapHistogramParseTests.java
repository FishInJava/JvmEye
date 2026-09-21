package com.jvmeeye;

import com.jvmeeye.diagnostics.HeapHistogramService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** jcmd GC.class_histogram 文本输出解析。 */
class HeapHistogramParseTests {

    private final HeapHistogramService service = new HeapHistogramService(null);

    private static final String SAMPLE = """
                 num     #instances         #bytes  class name (module)
            -------------------------------------------------------
               1:         19878       67770584  [B (java.base@21.0.12),
               2:           555        1886376  [Ljava.lang.Object; (java.base@21.0.12),
               3:          17841         428184  java.lang.String (java.base@21.0.12),
               4:            42          12345  com.example.Foo
            Total        38276        69158389
            """;

    @Test
    void parsesEntriesAndTotal() {
        HeapHistogramService.ParsedHistogram parsed = service.parse(SAMPLE);
        assertEquals(4, parsed.entries().size(), "应解析出 4 个类");
        assertEquals(38276L, parsed.totalInstances());
        assertEquals(69158389L, parsed.totalBytes());
    }

    @Test
    void stripsModuleSuffixAndTrailingComma() {
        HeapHistogramService.ParsedHistogram parsed = service.parse(SAMPLE);
        List<HeapHistogramService.Histogram.Entry> entries = parsed.entries();
        assertEquals("[B (java.base@21.0.12)", entries.get(0).name());
        assertEquals(19878L, entries.get(0).instances());
        assertEquals(67770584L, entries.get(0).bytes());
        assertEquals("com.example.Foo", entries.get(3).name());
    }

    @Test
    void handlesEmptyAndNullInput() {
        assertTrue(service.parse(null).entries().isEmpty());
        assertTrue(service.parse("").entries().isEmpty());
        assertTrue(service.parse("some garbage\nlines").entries().isEmpty());
    }
}
