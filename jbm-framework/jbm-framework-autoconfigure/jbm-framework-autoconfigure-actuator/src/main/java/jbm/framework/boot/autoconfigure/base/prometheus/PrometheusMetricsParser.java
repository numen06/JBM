package jbm.framework.boot.autoconfigure.base.prometheus;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wesley
 */
public class PrometheusMetricsParser {

    // 匹配 HELP 和 TYPE 注释
    private static final Pattern HELP_PATTERN = Pattern.compile("^# HELP (\\S+) (.+)$");
    private static final Pattern TYPE_PATTERN = Pattern.compile("^# TYPE (\\S+) (\\w+)$");
    private static final Pattern METRIC_PATTERN = Pattern.compile("^([\\w_]+)(?:\\{([^}]*)\\})?\\s+([0-9.Ee+-]+)(?:\\s+(\\d+))?$");
    private static final Pattern LABEL_PATTERN = Pattern.compile("(\\w+)=\"((?:[^\"\\\\]|\\\\.)*)\"");

    public static List<Map<String, Object>> parseToMap(String text) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, String> helpMap = new HashMap<>();
        Map<String, String> typeMap = new HashMap<>();

        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("# EXT")) continue;

            // 解析 HELP
            Matcher helpMatcher = HELP_PATTERN.matcher(line);
            if (helpMatcher.matches()) {
                helpMap.put(helpMatcher.group(1), helpMatcher.group(2));
                continue;
            }

            // 解析 TYPE
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.matches()) {
                typeMap.put(typeMatcher.group(1), typeMatcher.group(2));
                continue;
            }

            // 解析指标行
            Matcher metricMatcher = METRIC_PATTERN.matcher(line);
            if (metricMatcher.matches()) {
                String fullName = metricMatcher.group(1); // 如: http_requests_total
                String labelsPart = metricMatcher.group(2); // 如: method="GET",status="200"
                double value = Double.parseDouble(metricMatcher.group(3));
                Long timestamp = metricMatcher.group(4) != null ? Long.parseLong(metricMatcher.group(4)) : null;

                Map<String, Object> metric = new HashMap<>();
                metric.put("name", fullName);
                metric.put("value", value);
                metric.put("timestamp", timestamp != null ? timestamp : System.currentTimeMillis());

                // 解析标签
                Map<String, String> labels = new HashMap<>();
                if (labelsPart != null && !labelsPart.isEmpty()) {
                    Matcher labelMatcher = LABEL_PATTERN.matcher(labelsPart);
                    while (labelMatcher.find()) {
                        String key = labelMatcher.group(1);
                        String valueStr = labelMatcher.group(2);
                        // 处理转义: \" -> ", \\ -> \
                        valueStr = valueStr.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
                        labels.put(key, valueStr);
                    }
                }
                metric.put("labels", labels);

                // 补充 HELP 和 TYPE
                metric.put("help", helpMap.getOrDefault(fullName, ""));
                metric.put("type", typeMap.getOrDefault(fullName, "untyped"));

                result.add(metric);
            }
        }

        return result;
    }



}