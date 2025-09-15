package jbm.framework.boot.autoconfigure.base.listener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrometheusMetricsPrinter {

    public static void printKeyMetrics(List<Map<String, Object>> metrics) {
//        System.out.println("\n" + "🔥".repeat(50));
        System.out.println("           JVM & 系统运行状态摘要");
//        System.out.println("🔥".repeat(50));

        // CPU
        double systemCpuUsage = getGaugeValue(metrics, "system_cpu_usage");
        double processCpuUsage = getGaugeValue(metrics, "process_cpu_usage");
        double cpuCount = getGaugeValue(metrics, "system_cpu_count");

        System.out.printf("🖥️  CPU 核心数: %.0f 核\n", cpuCount);
        System.out.printf("⏱️  系统 CPU 使用率: %.1f%%\n", systemCpuUsage * 100);
        System.out.printf("⚡ Java 进程 CPU 使用率: %.1f%%\n", processCpuUsage * 100);

        // 内存 - Heap
        double heapUsed = getSumByArea(metrics, "jvm_memory_used_bytes", "heap");
        double heapCommitted = getSumByArea(metrics, "jvm_memory_committed_bytes", "heap");
        double heapMax = getSumByArea(metrics, "jvm_memory_max_bytes", "heap");

        System.out.printf("🧠 JVM 堆内存使用: %.1f MB / %.1f MB (%.1f%%)\n",
                bytesToMB(heapUsed),
                heapMax > 0 ? bytesToMB(heapMax) : bytesToMB(heapCommitted),
                heapMax > 0 ? (heapUsed / heapMax * 100) : (heapUsed / heapCommitted * 100)
        );

        // 非堆内存
        double nonHeapUsed = getSumByArea(metrics, "jvm_memory_used_bytes", "nonheap");
        double nonHeapCommitted = getSumByArea(metrics, "jvm_memory_committed_bytes", "nonheap");

        System.out.printf("📦 JVM 非堆内存使用: %.1f MB / %.1f MB\n",
                bytesToMB(nonHeapUsed), bytesToMB(nonHeapCommitted));

        // 线程
        double liveThreads = getGaugeValue(metrics, "jvm_threads_live_threads");
        double daemonThreads = getGaugeValue(metrics, "jvm_threads_daemon_threads");
        double peakThreads = getGaugeValue(metrics, "jvm_threads_peak_threads");

        System.out.printf("🧵 当前线程数: %.0f (守护线程 %.0f)\n", liveThreads, daemonThreads);
        System.out.printf("📈 历史峰值线程数: %.0f\n", peakThreads);

        // 类加载
        double loadedClasses = getGaugeValue(metrics, "jvm_classes_loaded_classes");
        double unloadedClasses = getCounterValue(metrics, "jvm_classes_unloaded_classes_total");

        System.out.printf("📚 当前加载类数: %.0f\n", loadedClasses);
        System.out.printf("🗑️  已卸载类数: %.0f\n", unloadedClasses);

        // GC 开销
        double gcOverhead = getGaugeValue(metrics, "jvm_gc_overhead_percent");
        System.out.printf("♻️  GC 开销占比: %.2f%%\n", gcOverhead * 100);

        // 磁盘（D盘）
        double diskTotal = getGaugeValueByLabel(metrics, "disk_total_bytes", "path", "D:\\workspaces\\JBM7\\.");
        double diskFree = getGaugeValueByLabel(metrics, "disk_free_bytes", "path", "D:\\workspaces\\JBM7\\.");
        double diskUsed = diskTotal - diskFree;

        if (diskTotal > 0) {
            System.out.printf("💾 D盘使用: %.1f GB / %.1f GB (%.1f%%)\n",
                    bytesToGB(diskUsed), bytesToGB(diskTotal), (diskUsed / diskTotal) * 100);
        }

        // 进程信息
        double uptime = getGaugeValue(metrics, "process_uptime_seconds");
        System.out.printf("🚀 应用运行时间: %.1f 秒 (%.1f 分钟)\n", uptime, uptime / 60);

//        System.out.println("🔥".repeat(50));
    }

    // ==================== 工具方法 ====================

    private static double getGaugeValue(List<Map<String, Object>> metrics, String name) {
        return metrics.stream()
                .filter(m -> name.equals(m.get("name")))
                .mapToDouble(m -> (Double) m.get("value"))
                .findFirst()
                .orElse(0.0);
    }

    private static double getCounterValue(List<Map<String, Object>> metrics, String name) {
        return metrics.stream()
                .filter(m -> name.equals(m.get("name")))
                .mapToDouble(m -> (Double) m.get("value"))
                .sum(); // counter 可能有多个 label
    }

    private static double getSumByArea(List<Map<String, Object>> metrics, String name, String area) {
        return metrics.stream()
                .filter(m -> name.equals(m.get("name")))
                .filter(m -> area.equals(((Map<String, String>) m.get("labels")).get("area")))
                .mapToDouble(m -> (Double) m.get("value"))
                .sum();
    }

    private static double getGaugeValueByLabel(List<Map<String, Object>> metrics, String name, String labelKey, String labelValue) {
        return metrics.stream()
                .filter(m -> name.equals(m.get("name")))
                .filter(m -> labelValue.equals(((Map<String, String>) m.get("labels")).get(labelKey)))
                .mapToDouble(m -> (Double) m.get("value"))
                .findFirst()
                .orElse(0.0);
    }

    private static double bytesToMB(double bytes) {
        return bytes / (1024 * 1024);
    }

    private static double bytesToGB(double bytes) {
        return bytes / (1024 * 1024 * 1024);
    }

}