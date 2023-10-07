package com.jbm.util;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作csv文件
 *
 * @author wesley.zhang
 */
public class CsvUtils extends CsvUtil {
    protected static final char FIELD_SEPARATOR = ',';
    protected static final char FIELD_QUOTE = '"';
    protected static final String DOUBLE_QUOTE = "\"\"";
    protected static final String SPECIAL_CHARS = "\r\n";

    /**
     * @param lines
     * @param clazz
     * @param fieldNames
     * @return
     */
    public static <T> List<T> toObjects(List<String> lines, Class<T> clazz, String... fieldNames) {
        List<T> array = new ArrayList<>();
        for (String line : lines) {
            array.add(toObject(line, clazz, fieldNames));
        }
        return array;
    }

    /**
     * @param line
     * @param clazz
     * @param fieldNames
     * @return
     */
    public static <T> T toObject(String line, Class<T> clazz, String... fieldNames) {
        JSONObject row = new JSONObject();

        boolean inQuotedField = false;
        int fieldStart = 0;

        final int len = line.length();
        int index = 0;
        for (int i = 0; i < len; i++) {

            char c = line.charAt(i);
            if (c == FIELD_SEPARATOR) {
                if (!inQuotedField) { // ignore we are quoting
                    index = addField(index, fieldNames, row, line, fieldStart, i, inQuotedField);
                    fieldStart = i + 1;
                }
            } else if (c == FIELD_QUOTE) {
                if (inQuotedField) {
                    if (i + 1 == len || line.charAt(i + 1) == FIELD_SEPARATOR) {
                        index = addField(index, fieldNames, row, line, fieldStart, i, inQuotedField);
                        fieldStart = i + 2;
                        i++; // and skip the comma
                        inQuotedField = false;
                    }
                } else if (fieldStart == i) {
                    inQuotedField = true; // this is a beginning of a quote
                    fieldStart++; // move field start
                }
            }
            if (index >= fieldNames.length)
                break;
        }
        // add last field - but only if string was not empty
        if (len > 0 && fieldStart <= len && index < fieldNames.length) {
            index = addField(index, fieldNames, row, line, fieldStart, len, inQuotedField);
        }
        return row.toJavaObject(clazz);
    }

    private static int addField(int index, String[] fieldNames, JSONObject row, String line, int startIndex, int endIndex, boolean inQuoted) {
        String field = line.substring(startIndex, endIndex);
        if (inQuoted) {
            field = StrUtil.replace(field, DOUBLE_QUOTE, "\"");
        }
        String fieldName = fieldNames[index];
        index++;
        if (fieldName == null)
            return index;
        row.put(fieldName, field);
        return index;
    }
}
