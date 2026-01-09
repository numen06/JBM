package com.jbm.framework.dao.expand;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Session;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import com.jbm.util.bean.Version;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SqlPrepareRunner {


    private final static String SELECT_INIT_TABLE = "SELECT COUNT(*) FROM information_schema.TABLES WHERE table_name ='sql_initialize' AND table_schema = DATABASE();";
    private final static String DROP_INIT_TABLE = "DROP TABLE IF EXISTS `sql_initialize`";
    
    /**
     * 检查列是否存在
     * @param columnName 列名（必须是常量，安全）
     */
    private static String buildCheckColumnSql(String columnName) {
        return "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_name ='sql_initialize' AND column_name ='" + columnName + "' AND table_schema = DATABASE();";
    }
    
    private final static String INSERT_SQL_FILE = "INSERT INTO `sql_initialize` (`file_name`, `version`, `module_name`, `file_hash`, `execute_status`, `error_message`, `execution_time`, `create_time`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    private final static String FIND_SQL_FILES = "SELECT * FROM `sql_initialize`";
    // 获取最大版本号：优先日期格式（8位数字），其次语义化版本
    private final static String GET_MAX_VERSION = "SELECT MAX(CASE WHEN version REGEXP '^[0-9]{8}$' THEN version ELSE '0' END) as max_date, " +
            "MAX(CASE WHEN version NOT REGEXP '^[0-9]{8}$' THEN version ELSE NULL END) as max_version " +
            "FROM `sql_initialize` WHERE version IS NOT NULL AND version != '' AND (execute_status IS NULL OR execute_status = 'SUCCESS')";

    private final static String DELETE_SQL_FILE = "DELETE FROM `sql_initialize` WHERE `file_name` = ? ;";
    
    // 必需的字段列表
    private final static String[] REQUIRED_COLUMNS = {
        "file_name", "version", "module_name", "file_hash", 
        "execute_status", "error_message", "execution_time", "create_time"
    };

    // 版本号解析正则：支持日期格式（优先）和语义化版本（向后兼容）
    // 日期格式：20240101, 2024-01-01, 2024_01_01（统一规范化为8位数字）
    private final static Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}[-_]?\\d{2}[-_]?\\d{2}|\\d{8})");
    // 语义化版本：V1.0.0, 1.0.0, V1, 1（向后兼容）
    private final static Pattern VERSION_PATTERN = Pattern.compile("^[Vv]?(\\d+(?:\\.\\d+)*(?:\\.\\d+)?)");

    private final static String SQL_INIT_TABLE = "sql_initialize.sql";
    public final static String SQl_DIR = "classpath:sql/schema/";

    public final static String BASE_SQl_DIR = SQl_DIR + "/**/**/**.sql";
    private final DataSource ds;


    private final Map<String, SqlInitialize> initializeList = new ConcurrentHashMap<>();
    private String currentDbVersion = "0";
    private String moduleName;
    private SqlAutoExecuteProperties sqlAutoExecuteProperties;

    public SqlPrepareRunner(DataSource ds, SqlAutoExecuteProperties sqlAutoExecuteProperties) {
        this.ds = ds;
        this.sqlAutoExecuteProperties = sqlAutoExecuteProperties;
        // 初始化模块名称
        this.moduleName = determineModuleName();
        log.debug("SQL执行模块名称: {}", this.moduleName);
    }

    /**
     * 确定模块名称
     * 优先级：
     * 1. 配置项 jbm.sql.auto-execute.module-name
     * 2. 从 SQL 文件路径中提取（从 classpath 路径中提取）
     * 3. 默认值 "default"
     */
    private String determineModuleName() {
        // 1. 优先使用配置项
        if (sqlAutoExecuteProperties != null && StrUtil.isNotBlank(sqlAutoExecuteProperties.getModuleName())) {
            String configModuleName = sqlAutoExecuteProperties.getModuleName();
            log.info("使用配置的模块名称: {}", configModuleName);
            return configModuleName;
        }
        
        // 2. 尝试从 SQL 文件路径中提取模块名
        try {
            String extractedModuleName = extractModuleNameFromResource();
            if (StrUtil.isNotBlank(extractedModuleName)) {
                log.info("从资源路径提取的模块名称: {}", extractedModuleName);
                return extractedModuleName;
            }
        } catch (Exception e) {
            log.debug("从资源路径提取模块名称失败", e);
        }
        
        // 3. 使用默认值
        log.info("使用默认模块名称: default");
        return "default";
    }

    /**
     * 从 SQL 资源路径中提取模块名称
     * 例如：从 classpath:jbm-cluster-platform-push/sql/schema/xxx.sql 中提取 jbm-cluster-platform-push
     */
    private String extractModuleNameFromResource() {
        try {
            Resource[] resources = SpringUtil.getApplicationContext().getResources(BASE_SQl_DIR);
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

    public void ready() {
        // 检查并创建/重建 sql_initialize 表（只有当字段不对时才重建）
        this.execute(session -> {
            try {
                // 1. 检查表是否存在
                ResultSet rs = SqlExecutor.callQuery(session.getConnection(), SELECT_INIT_TABLE);
                int tableCount = 0;
                while (rs.next()) {
                    tableCount = rs.getInt(1);
                }
                
                boolean needRebuild = false;
                
                if (tableCount < 1) {
                    // 表不存在，需要创建
                    log.info("sql_initialize 表不存在，开始创建...");
                    needRebuild = true;
                } else {
                    // 表存在，检查必需字段是否存在
                    log.debug("sql_initialize 表已存在，检查表结构...");
                    for (String columnName : REQUIRED_COLUMNS) {
                        String checkColumnSql = buildCheckColumnSql(columnName);
                        rs = SqlExecutor.callQuery(session.getConnection(), checkColumnSql);
                        int columnCount = 0;
                        while (rs.next()) {
                            columnCount = rs.getInt(1);
                        }
                        if (columnCount < 1) {
                            log.warn("sql_initialize 表缺少必需字段: {}，需要重建表", columnName);
                            needRebuild = true;
                            break;
                        }
                    }
                }
                
                if (needRebuild) {
                    // 需要重建表
                    log.info("重建 sql_initialize 表...");
                    session.execute(DROP_INIT_TABLE);
                    executeSqlFile(SQL_INIT_TABLE);
                    log.info("sql_initialize 表重建成功");
                } else {
                    log.debug("sql_initialize 表结构正确，保留现有数据");
                }
            } catch (Exception e) {
                log.error("执行初始化失败", e);
                throw new RuntimeException(e);
            }
        });
        
        // 获取当前数据库版本号（优先日期格式）
        this.execute(session -> {
            try {
                ResultSet rs = SqlExecutor.callQuery(session.getConnection(), GET_MAX_VERSION);
                if (rs.next()) {
                    String maxDate = rs.getString("max_date");
                    String maxVersion = rs.getString("max_version");
                    
                    // 优先使用日期格式的版本号
                    if (StrUtil.isNotBlank(maxDate) && !"0".equals(maxDate)) {
                        currentDbVersion = maxDate;
                        log.info("当前数据库版本（日期）: {}", currentDbVersion);
                    } else if (StrUtil.isNotBlank(maxVersion)) {
                        currentDbVersion = maxVersion;
                        log.info("当前数据库版本（语义化）: {}", currentDbVersion);
                    } else {
                        currentDbVersion = "0";
                        log.info("未找到已执行的SQL文件，使用默认版本: 0");
                    }
                } else {
                    currentDbVersion = "0";
                    log.info("未找到已执行的SQL文件，使用默认版本: 0");
                }
            } catch (Exception e) {
                log.warn("获取数据库版本失败，使用默认版本0", e);
                currentDbVersion = "0";
            }
        });
        
        // 加载已执行的SQL文件列表
        this.execute(session -> {
            try {
                List<Entity> list = session.query(FIND_SQL_FILES);
                list.forEach(entity -> {
                    SqlInitialize sqlInitialize = new SqlInitialize();
                    entity.toBeanWithCamelCase(sqlInitialize);
                    sqlInitialize.setCreateTime(entity.getDate("create_time"));
                    if (entity.get("version") != null) {
                        sqlInitialize.setVersion(entity.getStr("version"));
                    }
                    if (entity.get("module_name") != null) {
                        sqlInitialize.setModuleName(entity.getStr("module_name"));
                    }
                    if (entity.get("file_hash") != null) {
                        sqlInitialize.setFileHash(entity.getStr("file_hash"));
                    }
                    if (entity.get("execute_status") != null) {
                        sqlInitialize.setExecuteStatus(entity.getStr("execute_status"));
                    }
                    if (entity.get("error_message") != null) {
                        sqlInitialize.setErrorMessage(entity.getStr("error_message"));
                    }
                    if (entity.get("execution_time") != null) {
                        sqlInitialize.setExecutionTime(entity.getLong("execution_time"));
                    }

                    initializeList.put(sqlInitialize.getFileName(), sqlInitialize);
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * 记录SQL执行成功
     */
    public void executeSuccess(String sqlFileName, String version, long executionTime) {
        this.execute(session -> {
            try {
                // 计算文件哈希值
                String fileHash = calculateFileHash(sqlFileName);
                
                int result = session.execute(INSERT_SQL_FILE, 
                    sqlFileName, 
                    version, 
                    moduleName,
                    fileHash,
                    "SUCCESS",
                    null, // error_message
                    executionTime,
                    DateUtil.now());
                    
                if (result < 1) {
                    throw new RuntimeException("插入记录失败");
                }
                // 如果有版本号，更新当前数据库版本（内存中）
                if (StrUtil.isNotBlank(version) && compareVersion(version, currentDbVersion) > 0) {
                    currentDbVersion = version;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 记录SQL执行失败
     */
    public void executeFailed(String sqlFileName, String version, long executionTime, String errorMessage) {
        this.execute(session -> {
            try {
                // 计算文件哈希值
                String fileHash = calculateFileHash(sqlFileName);
                
                int result = session.execute(INSERT_SQL_FILE, 
                    sqlFileName, 
                    version, 
                    moduleName,
                    fileHash,
                    "FAILED",
                    errorMessage,
                    executionTime,
                    DateUtil.now());
                    
                if (result < 1) {
                    throw new RuntimeException("插入记录失败");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 计算SQL文件的哈希值（MD5）
     */
    private String calculateFileHash(String fileName) {
        try {
            InputStream inputStream = ResourceUtil.getStreamSafe(SQl_DIR + fileName);
            if (inputStream != null) {
                String hash = DigestUtil.md5Hex(inputStream);
                IoUtil.close(inputStream);
                return hash;
            }
        } catch (Exception e) {
            log.debug("计算文件哈希值失败: {}", fileName, e);
        }
        return null;
    }


    /**
     * 获取SQL文件的完整相对路径（包含文件夹）
     */
    private String getSqlFileName(Resource resource) throws IOException {
        try {
            String baseDir = SpringUtil.getApplicationContext().getResource(SQl_DIR).getURL().toString();
            String filePath = resource.getURL().toString();
            String fileName = StrUtil.removePrefix(filePath, baseDir);
            // 规范化路径，移除开头的斜杠
            if (fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }
            return fileName;
        } catch (Exception e) {
            // 如果获取baseDir失败，尝试直接从resource获取文件名
            String filename = resource.getFilename();
            if (filename != null) {
                return filename;
            }
            // 最后尝试从URI获取
            String uri = resource.getURI().toString();
            int lastSlash = uri.lastIndexOf('/');
            return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
        }
    }

    /**
     * 从文件路径和文件名解析版本号
     * 优先级：1. 从文件夹路径解析 2. 从文件名解析 3. 返回null（无版本号）
     */
    private String parseVersionFromPath(String filePath, String fileName) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        
        // 1. 优先从文件夹路径解析版本号
        // 例如：sql/schema/V1.0.0/webhook_index.sql -> 提取 V1.0.0
        String[] pathParts = filePath.split("/");
        for (String part : pathParts) {
            if (StrUtil.isNotBlank(part)) {
                String version = extractVersion(part);
                if (version != null) {
                    log.debug("从文件夹路径解析版本号: {} -> {}", part, version);
                    return version;
                }
            }
        }
        
        // 2. 从文件名解析版本号
        // 例如：V1.0.0__webhook_index.sql -> 提取 1.0.0
        if (StrUtil.isNotBlank(fileName)) {
            String version = extractVersion(fileName);
            if (version != null) {
                log.debug("从文件名解析版本号: {} -> {}", fileName, version);
                return version;
            }
        }
        
        // 3. 无法解析，返回null（向后兼容）
        return null;
    }

    /**
     * 从字符串中提取版本号
     * 优先级：1. 日期格式（8位数字：YYYYMMDD） 2. 语义化版本（向后兼容）
     * 支持格式：
     * - 日期：20240101, 2024-01-01, 2024_01_01（统一规范化为8位数字：20240101）
     * - 语义化版本：V1.0.0, 1.0.0, V1, 1
     */
    private String extractVersion(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }
        
        // 1. 优先尝试匹配日期格式（8位数字：YYYYMMDD）
        Matcher dateMatcher = DATE_PATTERN.matcher(str);
        if (dateMatcher.find()) {
            String date = dateMatcher.group(1);
            // 规范化日期：移除分隔符，统一为8位数字
            date = date.replaceAll("[-_]", "");
            if (date.length() == 8) {
                return date;
            }
        }
        
        // 2. 尝试匹配语义化版本（向后兼容）
        Matcher versionMatcher = VERSION_PATTERN.matcher(str);
        if (versionMatcher.find()) {
            return versionMatcher.group(1);
        }
        
        return null;
    }

    /**
     * 比较两个版本号
     * 优先级：1. 日期格式（8位数字：YYYYMMDD） 2. 语义化版本 3. 字符串比较
     * @return 负数表示v1 < v2, 0表示相等, 正数表示v1 > v2
     */
    private int compareVersion(String v1, String v2) {
        if (StrUtil.isBlank(v1) && StrUtil.isBlank(v2)) {
            return 0;
        }
        if (StrUtil.isBlank(v1)) {
            return -1;
        }
        if (StrUtil.isBlank(v2)) {
            return 1;
        }
        
        // 1. 如果都是8位数字（日期格式：YYYYMMDD），直接比较
        if (v1.matches("^\\d{8}$") && v2.matches("^\\d{8}$")) {
            return v1.compareTo(v2);
        }
        
        // 2. 如果一个是日期格式，另一个不是，日期格式优先（更大）
        if (v1.matches("^\\d{8}$") && !v2.matches("^\\d{8}$")) {
            return 1; // 日期格式优先
        }
        if (!v1.matches("^\\d{8}$") && v2.matches("^\\d{8}$")) {
            return -1; // 日期格式优先
        }
        
        // 3. 尝试使用语义化版本比较（向后兼容）
        try {
            Version version1 = Version.parse(v1);
            Version version2 = Version.parse(v2);
            return version1.compareTo(version2);
        } catch (Exception e) {
            // 4. 如果无法解析，使用字符串比较（向后兼容）
            log.debug("版本号无法解析为Version对象，使用字符串比较: {} vs {}", v1, v2);
            return v1.compareTo(v2);
        }
    }

    private final StopWatch stopWatch = new StopWatch("扫描SQL文件");

    /**
     * SQL文件信息（用于排序和执行）
     */
    private static class SqlFileInfo {
        Resource resource;
        String fileName;
        String version;
        
        SqlFileInfo(Resource resource, String fileName, String version) {
            this.resource = resource;
            this.fileName = fileName;
            this.version = version;
        }
    }

    /**
     * 扫描SQL语句
     */
    public void scanSqlFiles() throws IOException {
        this.ready();
        Resource[] resources = SpringUtil.getApplicationContext().getResources(BASE_SQl_DIR);
        
        if (resources == null || resources.length == 0) {
            log.info("未找到SQL schema文件，跳过执行");
            return;
        }
        
        log.info("找到 {} 个SQL文件，当前数据库版本: {}", resources.length, currentDbVersion);
        
        // 解析所有SQL文件，提取版本号
        List<SqlFileInfo> sqlFiles = new ArrayList<>();
        for (Resource resource : resources) {
            try {
                String fileName = getSqlFileName(resource);
                if (fileName.equalsIgnoreCase(SQL_INIT_TABLE)) {
                    continue;
                }
                
                String version = parseVersionFromPath(fileName, resource.getFilename());
                sqlFiles.add(new SqlFileInfo(resource, fileName, version));
            } catch (Exception e) {
                log.warn("解析SQL文件失败: {}", resource.getFilename(), e);
            }
        }
        
        // 分离有版本号和无版本号的文件
        List<SqlFileInfo> versionedFiles = new ArrayList<>();
        List<SqlFileInfo> unversionedFiles = new ArrayList<>();
        
        for (SqlFileInfo fileInfo : sqlFiles) {
            if (StrUtil.isNotBlank(fileInfo.version)) {
                versionedFiles.add(fileInfo);
            } else {
                unversionedFiles.add(fileInfo);
            }
        }
        
        // 过滤需要执行的版本化文件（版本号 > 当前数据库版本）
        List<SqlFileInfo> toExecuteVersioned = new ArrayList<>();
        for (SqlFileInfo fileInfo : versionedFiles) {
            if (compareVersion(fileInfo.version, currentDbVersion) > 0) {
                toExecuteVersioned.add(fileInfo);
            } else {
                log.debug("SQL文件版本号 <= 当前数据库版本，跳过: {} (版本: {})", fileInfo.fileName, fileInfo.version);
            }
        }
        
        // 过滤需要执行的无版本号文件（按文件名检查是否已执行）
        List<SqlFileInfo> toExecuteUnversioned = new ArrayList<>();
        for (SqlFileInfo fileInfo : unversionedFiles) {
            if (!initializeList.containsKey(fileInfo.fileName)) {
                toExecuteUnversioned.add(fileInfo);
            } else {
                log.debug("SQL文件已执行过，跳过: {}", fileInfo.fileName);
            }
        }
        
        // 按版本号排序（从小到大）
        toExecuteVersioned.sort((f1, f2) -> compareVersion(f1.version, f2.version));
        
        // 按文件名排序（向后兼容）
        toExecuteUnversioned.sort(Comparator.comparing(f -> f.fileName));
        
        // 合并：先执行版本化文件，再执行无版本号文件
        List<SqlFileInfo> toExecute = new ArrayList<>();
        toExecute.addAll(toExecuteVersioned);
        toExecute.addAll(toExecuteUnversioned);
        
        if (toExecute.isEmpty()) {
            log.info("没有需要执行的SQL文件");
            return;
        }
        
        log.info("需要执行的SQL文件: {} 个（版本化: {}, 无版本号: {}）", 
            toExecute.size(), toExecuteVersioned.size(), toExecuteUnversioned.size());
        
        // 顺序执行
        int executedCount = 0;
        for (SqlFileInfo fileInfo : toExecute) {
            long startTime = System.currentTimeMillis();
            String errorMessage = null;
            
            try {
                if (StrUtil.isNotBlank(fileInfo.version)) {
                    log.info("执行SQL文件: {} (版本: {}, 模块: {})", fileInfo.fileName, fileInfo.version, moduleName);
                } else {
                    log.info("执行SQL文件: {} (模块: {})", fileInfo.fileName, moduleName);
                }
                
                executeSqlFile(fileInfo.fileName);
                long executionTime = System.currentTimeMillis() - startTime;
                executeSuccess(fileInfo.fileName, fileInfo.version, executionTime);
                executedCount++;
                
                if (StrUtil.isNotBlank(fileInfo.version)) {
                    log.info("SQL文件执行成功: {} (版本: {}, 耗时: {}ms)", fileInfo.fileName, fileInfo.version, executionTime);
                } else {
                    log.info("SQL文件执行成功: {} (耗时: {}ms)", fileInfo.fileName, executionTime);
                }
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                errorMessage = e.getMessage();
                log.error("执行SQL文件失败: {} (版本: {}, 模块: {}, 耗时: {}ms)", 
                    fileInfo.fileName, fileInfo.version, moduleName, executionTime, e);
                
                // 记录失败信息到数据库
                try {
                    executeFailed(fileInfo.fileName, fileInfo.version, executionTime, errorMessage);
                } catch (Exception ex) {
                    log.error("记录失败信息到数据库失败", ex);
                }
                
                // 执行失败，抛出异常阻止启动
                throw new RuntimeException("SQL文件执行失败，应用启动被阻止: " + fileInfo.fileName + " (版本: " + fileInfo.version + ")", e);
            }
        }
        
        log.info("SQL文件执行完成 - 总计: {}, 已执行: {}, 用时: {}秒", 
            sqlFiles.size(), executedCount, stopWatch.getTotalTimeSeconds());
        if (stopWatch.getTaskCount() > 0) {
            log.debug(stopWatch.prettyPrint(TimeUnit.SECONDS));
        }
    }

    public void executeSqlFile(String fileName) {
        InputStream initializeSql = ResourceUtil.getStreamSafe(SQl_DIR + fileName);
        this.execute((session) -> {
            try {
                stopWatch.start(StrUtil.format("执行SQL脚本:{}", fileName));
                ScriptUtils.executeSqlScript(session.getConnection(), new InputStreamResource(initializeSql));
                stopWatch.stop();
            } catch (Exception e) {
                log.error("执行SQL文件失败:{}", fileName, e);
                throw new RuntimeException(e);
            }
        });
        IoUtil.close(initializeSql);
    }

    @SneakyThrows
    public void execute(Consumer<Session> consumer) {
        //获取默认数据源
        Session session = Session.create(ds);
        try {
            session.beginTransaction();
            consumer.accept(session);
            if (!session.getConnection().getAutoCommit()) {
                session.commit();
            }
        } catch (Exception e) {
            session.rollback();
            // 重新抛出异常，让调用者处理
            throw e;
        } finally {
            session.close();
        }
    }


}
