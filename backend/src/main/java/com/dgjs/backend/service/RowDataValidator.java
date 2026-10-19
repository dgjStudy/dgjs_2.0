package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetColumn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RowDataValidator {

    private static final BigDecimal LONG_LIMIT = new BigDecimal("1e15");

    // existing이 null이 아니면(수정) 값이 바뀌지 않은 컬럼은 기존 값을 그대로 유지함 (과거 적재 데이터의 형식 불일치 대응)
    public Map<String, Object> validate(List<DatasetColumn> columns, Map<String, Object> input, Map<String, Object> existing) {
        Map<String, DatasetColumn> columnMap = columns.stream()
                .collect(Collectors.toMap(DatasetColumn::getColumnKey, Function.identity()));

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if ("key".equals(key)) {
                continue;
            }
            DatasetColumn column = columnMap.get(key);
            if (column == null) {
                throw new IllegalArgumentException("스키마에 없는 컬럼입니다: '" + key + "'");
            }
            Object value = entry.getValue();
            if (existing != null && existing.containsKey(key) && isSameValue(existing.get(key), value)) {
                result.put(key, existing.get(key));
            } else {
                result.put(key, convert(column, value));
            }
        }
        return result;
    }

    private boolean isSameValue(Object oldValue, Object newValue) {
        return text(oldValue).equals(text(newValue));
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private Object convert(DatasetColumn column, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return "";
        }
        String type = column.getDataType() == null ? "STRING" : column.getDataType().toUpperCase();
        return switch (type) {
            case "NUMBER" -> toNumber(column, value);
            case "DATE" -> toDate(column, value);
            case "BOOLEAN" -> toBoolean(column, value);
            default -> toText(column, value);
        };
    }

    private Object toText(DatasetColumn column, Object value) {
        if (value instanceof Map || value instanceof Collection || value.getClass().isArray()) {
            throw new IllegalArgumentException("'" + column.getColumnName() + "' 컬럼에는 단순 값만 입력할 수 있습니다.");
        }
        return value.toString();
    }

    private Object toNumber(DatasetColumn column, Object value) {
        BigDecimal number;
        try {
            if (value instanceof Number n) {
                number = new BigDecimal(n.toString());
            } else if (value instanceof String s) {
                number = new BigDecimal(s.trim());
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + column.getColumnName() + "' 컬럼은 숫자만 입력할 수 있습니다.");
        }

        if (number.stripTrailingZeros().scale() <= 0 && number.abs().compareTo(LONG_LIMIT) < 0) {
            return number.longValue();
        }
        double d = number.doubleValue();
        if (Double.isInfinite(d)) {
            throw new IllegalArgumentException("'" + column.getColumnName() + "' 컬럼의 숫자 범위가 너무 큽니다.");
        }
        return d;
    }

    private Object toDate(DatasetColumn column, Object value) {
        String message = "'" + column.getColumnName() + "' 컬럼은 날짜 형식(예: 2026-10-13 또는 2026-10-13T09:30)으로 입력해야 합니다.";
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException(message);
        }
        String text = s.trim();
        try {
            return LocalDate.parse(text).toString();
        } catch (DateTimeParseException ignored) {
            // 날짜시간 형식 시도
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(text.replace(' ', 'T'));
            return dt.toLocalTime().equals(LocalTime.MIDNIGHT) ? dt.toLocalDate().toString() : dt.toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private Object toBoolean(DatasetColumn column, Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            String text = s.trim();
            if (text.equalsIgnoreCase("true")) return true;
            if (text.equalsIgnoreCase("false")) return false;
        }
        throw new IllegalArgumentException("'" + column.getColumnName() + "' 컬럼은 true 또는 false만 입력할 수 있습니다.");
    }
}
