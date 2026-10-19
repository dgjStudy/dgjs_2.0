package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetColumn;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RowDataValidatorTest {

    private final RowDataValidator validator = new RowDataValidator();

    private final List<DatasetColumn> columns = List.of(
            column("col_0", "문자", "STRING"),
            column("col_1", "숫자", "NUMBER"),
            column("col_2", "날짜", "DATE"),
            column("col_3", "참거짓", "BOOLEAN"));

    private DatasetColumn column(String key, String name, String type) {
        return DatasetColumn.builder().columnKey(key).columnName(name).dataType(type).build();
    }

    private Map<String, Object> row(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private Object add(String key, Object value) {
        return validator.validate(columns, row(key, value), null).get(key);
    }

    @Test
    void 스키마_밖_키는_거부한다() {
        assertThatThrownBy(() -> validator.validate(columns, row("col_9", "x"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("col_9");
    }

    @Test
    void key_필드는_무시한다() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("key", 123);
        input.put("col_0", "a");

        assertThat(validator.validate(columns, input, null)).containsOnlyKeys("col_0");
    }

    @Test
    void 빈_값은_모든_타입에서_빈_문자열로_저장한다() {
        for (String key : List.of("col_0", "col_1", "col_2", "col_3")) {
            assertThat(add(key, null)).isEqualTo("");
            assertThat(add(key, "")).isEqualTo("");
            assertThat(add(key, "   ")).isEqualTo("");
        }
    }

    @Test
    void 문자_컬럼은_단순_값만_문자열로_저장한다() {
        assertThat(add("col_0", "abc")).isEqualTo("abc");
        assertThat(add("col_0", 10)).isEqualTo("10");
        assertThat(add("col_0", true)).isEqualTo("true");
        assertThatThrownBy(() -> add("col_0", Map.of("a", 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> add("col_0", List.of(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 숫자_컬럼은_정수는_long_소수는_double로_변환한다() {
        assertThat(add("col_1", "30")).isEqualTo(30L);
        assertThat(add("col_1", 30)).isEqualTo(30L);
        assertThat(add("col_1", "30.0")).isEqualTo(30L);
        assertThat(add("col_1", " -7 ")).isEqualTo(-7L);
        assertThat(add("col_1", "12.5")).isEqualTo(12.5);
        assertThat(add("col_1", 12.5)).isEqualTo(12.5);
        assertThat(add("col_1", "1e3")).isEqualTo(1000L);
        assertThat(add("col_1", "1e20")).isEqualTo(1e20);
    }

    @Test
    void 숫자_컬럼은_숫자가_아닌_값을_거부한다() {
        for (Object bad : List.of("abc", "1,000", "NaN", "1e400", true)) {
            assertThatThrownBy(() -> add("col_1", bad))
                    .as(String.valueOf(bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("숫자");
        }
    }

    @Test
    void 날짜_컬럼은_ISO_형식으로_정규화한다() {
        assertThat(add("col_2", "2026-10-13")).isEqualTo("2026-10-13");
        assertThat(add("col_2", "2026-10-13T09:30")).isEqualTo("2026-10-13T09:30");
        assertThat(add("col_2", "2026-10-13 09:30")).isEqualTo("2026-10-13T09:30");
        assertThat(add("col_2", "2026-10-13 09:30:00")).isEqualTo("2026-10-13T09:30");
        assertThat(add("col_2", "2026-10-13T00:00")).isEqualTo("2026-10-13");
    }

    @Test
    void 날짜_컬럼은_다른_형식을_거부한다() {
        for (Object bad : List.of("2026/10/13", "2026-13-40", "내일", 20261013)) {
            assertThatThrownBy(() -> add("col_2", bad))
                    .as(String.valueOf(bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("날짜");
        }
    }

    @Test
    void 불리언_컬럼은_true_false만_허용한다() {
        assertThat(add("col_3", "TRUE")).isEqualTo(true);
        assertThat(add("col_3", " false ")).isEqualTo(false);
        assertThat(add("col_3", Boolean.TRUE)).isEqualTo(true);
        for (Object bad : List.of("yes", "1", 1)) {
            assertThatThrownBy(() -> add("col_3", bad))
                    .as(String.valueOf(bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("true 또는 false");
        }
    }

    @Test
    void 타입_정보가_없거나_알_수_없으면_문자로_취급한다() {
        List<DatasetColumn> legacy = List.of(column("col_0", "자유", null), column("col_1", "기타", "UNKNOWN"));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("col_0", 5);
        input.put("col_1", "x");

        assertThat(validator.validate(legacy, input, null)).containsEntry("col_0", "5").containsEntry("col_1", "x");
    }

    @Test
    void 오류_메시지에_컬럼명이_들어간다() {
        assertThatThrownBy(() -> add("col_1", "abc")).hasMessageContaining("'숫자'");
    }

    @Test
    void 수정_시_바뀌지_않은_컬럼은_형식이_어긋나도_기존_값을_유지한다() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("col_1", "abc");
        existing.put("col_2", "2026/10/13");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("col_0", "새 값");
        input.put("col_1", "abc");
        input.put("col_2", "2026/10/13");

        Map<String, Object> result = validator.validate(columns, input, existing);

        assertThat(result).containsEntry("col_0", "새 값").containsEntry("col_1", "abc").containsEntry("col_2", "2026/10/13");
    }

    @Test
    void 수정_시_바뀐_컬럼은_검증한다() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("col_1", "abc");

        assertThatThrownBy(() -> validator.validate(columns, row("col_1", "xyz"), existing))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수정_시_숫자와_같은_값의_문자열은_바뀌지_않은_것으로_본다() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("col_1", 30L);

        assertThat(validator.validate(columns, row("col_1", "30"), existing)).containsEntry("col_1", 30L);
    }

    @Test
    void 수정_시_기존_값에_없던_컬럼은_검증한다() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("col_0", "a");

        assertThatThrownBy(() -> validator.validate(columns, row("col_1", "abc"), existing))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수정_시에도_스키마_밖_키는_거부한다() {
        assertThatThrownBy(() -> validator.validate(columns, row("col_7", "x"), new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
