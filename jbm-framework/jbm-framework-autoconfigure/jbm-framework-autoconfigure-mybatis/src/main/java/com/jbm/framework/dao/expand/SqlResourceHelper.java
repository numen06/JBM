package com.jbm.framework.dao.expand;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * SQL资源文件处理工具类
 * 负责处理SQL文件的路径解析、哈希计算、模块名提取等
 * 使用Spring的资源机制
 * 
 * @author wesley
 */
@Slf4j
public class SqlResourceHelper {

    public final static String SQl_DIR = "classpath:sql/schema/";
    public final static String BASE_SQl_DIR = "classpath*:sql/schema/**/*.sql";

    private final ResourcePatternResolver resourcePatternResolver;
    private final ResourceLoader resourceLoader;

    public SqlResourceHelper(ResourcePatternResolver resourcePatternResolver, ResourceLoader resourceLoader) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
    }

    public ResourcePatternResolver getResourcePatternResolver() {
        return resourcePatternResolver;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * 获取SQL文件的完整相对路径（包含文件夹）
     * 支持JAR包和文件系统
     * Spring的资源机制已经处理了BOOT-INF/classes/等路径问题
     * 
     * @param resource Spring Resource对象
     * @return 相对路径，例如：20240101/webhook_index.sql
     * @throws IOException IO异常
     */
    public String getSqlFileName(Resource resource) throws IOException {
        try {
            // 优先使用Resource的description，Spring已经处理了JAR包路径
            // 例如: class path resource [sql/schema/20240101/webhook_index.sql]
            String resourceDescription = resource.getDescription();
            if (resourceDescription != null && resourceDescription.contains("sql/schema/")) {
                int startIndex = resourceDescription.indexOf("sql/schema/") + "sql/schema/".length();
                int endIndex = resourceDescription.length();
                if (resourceDescription.contains("]")) {
                    endIndex = resourceDescription.lastIndexOf("]");
                }
                String relativePath = resourceDescription.substring(startIndex, endIndex);
                // 规范化路径，移除开头的斜杠
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                if (StrUtil.isNotBlank(relativePath)) {
                    return relativePath;
                }
            }
            
            // 备用方案：通过baseDir比较来提取相对路径
            // Spring的ResourceLoader已经处理了JAR包内部的路径映射
            try {
                Resource baseDirResource = resourceLoader.getResource(SQl_DIR);
                if (baseDirResource.exists()) {
                    String baseDir = baseDirResource.getURL().toString();
                    String resourceUrl = resource.getURL().toString();
                    if (resourceUrl.startsWith(baseDir)) {
                        String fileName = StrUtil.removePrefix(resourceUrl, baseDir);
                        // 规范化路径，移除开头的斜杠
                        if (fileName.startsWith("/")) {
                            fileName = fileName.substring(1);
                        }
                        if (StrUtil.isNotBlank(fileName)) {
                            return fileName;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("无法通过baseDir提取路径，尝试其他方式", e);
            }
            
            // 最后尝试从URI获取（Spring已经处理了JAR包路径）
            try {
                String uri = resource.getURI().toString();
                int schemaIndex = uri.indexOf("sql/schema/");
                if (schemaIndex >= 0) {
                    String relativePath = uri.substring(schemaIndex + "sql/schema/".length());
                    // 移除查询参数和锚点
                    if (relativePath.contains("?")) {
                        relativePath = relativePath.substring(0, relativePath.indexOf("?"));
                    }
                    if (relativePath.contains("#")) {
                        relativePath = relativePath.substring(0, relativePath.indexOf("#"));
                    }
                    if (StrUtil.isNotBlank(relativePath)) {
                        return relativePath;
                    }
                }
            } catch (Exception e) {
                log.debug("无法从URI提取路径", e);
            }
            
            // 最后的备用方案：使用文件名
            String filename = resource.getFilename();
            return filename != null ? filename : "unknown.sql";
        } catch (Exception e) {
            log.warn("获取SQL文件路径失败，使用默认方式: {}", resource.getFilename(), e);
            String filename = resource.getFilename();
            return filename != null ? filename : "unknown.sql";
        }
    }

    /**
     * 计算SQL文件的哈希值（MD5）
     * 
     * @param resource Resource对象（优先使用，支持JAR包）
     * @param fileName 文件名（备用）
     * @return MD5哈希值，如果计算失败则返回null
     */
    public String calculateFileHash(Resource resource, String fileName) {
        InputStream inputStream = null;
        try {
            // 优先使用Resource对象
            if (resource != null && resource.exists()) {
                inputStream = resource.getInputStream();
            } else if (StrUtil.isNotBlank(fileName)) {
                // 备用方案：使用Spring的ResourceLoader加载资源
                try {
                    Resource fileResource = resourceLoader.getResource(SQl_DIR + fileName);
                    if (fileResource.exists()) {
                        inputStream = fileResource.getInputStream();
                    } else {
                        // 尝试使用classpath*模式
                        Resource[] resources = resourcePatternResolver.getResources("classpath*:sql/schema/" + fileName);
                        if (resources != null && resources.length > 0 && resources[0].exists()) {
                            inputStream = resources[0].getInputStream();
                        }
                    }
                } catch (Exception e) {
                    log.debug("使用ResourceLoader加载资源失败: {}", fileName, e);
                }
            }
            
            if (inputStream != null) {
                String hash = DigestUtil.md5Hex(inputStream);
                return hash;
            }
        } catch (Exception e) {
            log.debug("计算文件哈希值失败: {}", fileName, e);
        } finally {
            IoUtil.close(inputStream);
        }
        return null;
    }

    /**
     * 从 SQL 资源路径中提取模块名称
     * 例如：从 classpath:jbm-cluster-platform-push/sql/schema/xxx.sql 中提取 jbm-cluster-platform-push
     * 
     * @return 模块名称，如果无法提取则返回null
     */
    public String extractModuleNameFromResource() {
        try {
            // 使用Spring的ResourcePatternResolver来扫描资源
            Resource[] resources = resourcePatternResolver.getResources(BASE_SQl_DIR);
            if (resources != null && resources.length > 0) {
                // 使用第一个资源的路径来提取模块名
                Resource firstResource = resources[0];
                String resourceUrl = firstResource.getURL().toString();
                
                // 解析路径，查找模块标识
                // 例如：jar:file:/path/to/jbm-cluster-platform-push-1.0.0.jar!/BOOT-INF/classes!/sql/schema/xxx.sql
                // 或：file:/path/to/jbm-cluster-platform-push/src/main/resources/sql/schema/xxx.sql
                
                if (resourceUrl.contains(".jar!")) {
                    // JAR包中的资源：提取JAR文件名中的模块名
                    String jarPart = StrUtil.subBetween(resourceUrl, "file:", ".jar!");
                    if (StrUtil.isNotBlank(jarPart)) {
                        String jarFileName = jarPart.substring(jarPart.lastIndexOf("/") + 1);
                        // 移除版本号和扩展名，提取模块名
                        // 例如：jbm-cluster-platform-push-1.0.0.jar -> jbm-cluster-platform-push
                        String moduleName = jarFileName.replaceAll("-\\d+.*$", "");
                        if (StrUtil.isNotBlank(moduleName) && !moduleName.equals(jarFileName)) {
                            return moduleName;
                        }
                    }
                } else {
                    // 文件系统中的资源：从路径中提取模块名
                    // 例如：/path/to/jbm-cluster-platform-push/src/main/resources/sql/schema/xxx.sql
                    String[] pathParts = resourceUrl.split("/");
                    for (int i = pathParts.length - 1; i >= 0; i--) {
                        String part = pathParts[i];
                        // 查找包含 jbm- 或 cluster- 等标识的路径段
                        if (StrUtil.isNotBlank(part) && (part.contains("jbm-") || part.contains("cluster-"))) {
                            // 移除可能的后缀（如 -SNAPSHOT, 版本号等）
                            String moduleName = part.replaceAll("-\\d+.*$", "");
                            moduleName = moduleName.replaceAll("-SNAPSHOT$", "");
                            if (StrUtil.isNotBlank(moduleName)) {
                                return moduleName;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从资源路径提取模块名称失败", e);
        }
        return null;
    }
}
