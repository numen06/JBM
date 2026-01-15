package com.jbm.framework.dao.expand;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Session;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SQL文件自动执行运行器
 * 负责扫描、解析、排序和执行SQL文件
 * 
 * @author wesley
 */
@Slf4j
public class SqlPrepareRunner {

    private final static String SQL_INIT_TABLE = "sql_initialize.sql";
    
    private final DataSource ds;
    private final SqlAutoExecuteProperties sqlAutoExecuteProperties;
    private final SqlResourceHelper sqlResourceHelper;
    
    private final Map<String, SqlInitialize> initializeList = new ConcurrentHashMap<>();
    private String currentDbVersion = "0";
    private String moduleName;
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

    public SqlPrepareRunner(DataSource ds, SqlAutoExecuteProperties sqlAutoExecuteProperties, 
                            ResourcePatternResolver resourcePatternResolver, ResourceLoader resourceLoader) {
        this.ds = ds;
        this.sqlAutoExecuteProperties = sqlAutoExecuteProperties;
        this.sqlResourceHelper = new SqlResourceHelper(resourcePatternResolver, resourceLoader);
        // 初始化模块名称
        this.moduleName = determineModuleName();
        log.debug("SqlPrepareRunner 初始化完成，模块名称: {}", this.moduleName);
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
            return sqlAutoExecuteProperties.getModuleName();
        }
        
        // 2. 尝试从 SQL 文件路径中提取模块名
        try {
            String extractedModuleName = sqlResourceHelper.extractModuleNameFromResource();
            if (StrUtil.isNotBlank(extractedModuleName)) {
                return extractedModuleName;
            }
        } catch (Exception e) {
            log.debug("从资源路径提取模块名称失败: {}", e.getMessage());
        }
        
        // 3. 使用默认值
        return "default";
    }

    /**
     * 初始化：检查表结构、加载已执行记录、获取当前版本
     * 使用 SELECT * 查询来检测表是否存在和结构是否正确
     */
    public void ready() {
        log.info("准备SQL执行环境...");
        
        // 尝试查询表，如果失败则创建/重建表
        this.execute(session -> {
            SqlInitializeRepository repository = new SqlInitializeRepository(session);
            
            if (!repository.tryQueryTable()) {
                log.info("sql_initialize 表不存在，开始创建...");
                repository.dropTable();
                try {
                    Resource initTableResource = sqlResourceHelper.getResourceLoader().getResource(SqlResourceHelper.SQl_DIR + SQL_INIT_TABLE);
                    if (initTableResource.exists()) {
                        executeSqlFile(initTableResource, SQL_INIT_TABLE);
                    } else {
                        Resource[] resources = sqlResourceHelper.getResourcePatternResolver().getResources("classpath*:sql/schema/" + SQL_INIT_TABLE);
                        if (resources != null && resources.length > 0 && resources[0].exists()) {
                            executeSqlFile(resources[0], SQL_INIT_TABLE);
                        } else {
                            throw new RuntimeException("找不到 sql_initialize.sql 文件");
                        }
                    }
                } catch (Exception e) {
                    log.error("执行 sql_initialize.sql 失败", e);
                    throw new RuntimeException(e);
                }
                log.info("sql_initialize 表创建成功");
            }
        });
        
        // 获取当前数据库版本号（优先日期格式）
        this.execute(session -> {
            SqlInitializeRepository repository = new SqlInitializeRepository(session);
            currentDbVersion = repository.getMaxVersion();
        });
        
        // 加载已执行的SQL文件列表
        this.execute(session -> {
            SqlInitializeRepository repository = new SqlInitializeRepository(session);
            initializeList.putAll(repository.loadAllRecords());
        });
        
        log.info("SQL执行环境准备完成 - 当前版本: {}, 已执行记录: {} 条", currentDbVersion, initializeList.size());
    }

    /**
     * 记录SQL执行成功
     */
    public void executeSuccess(String sqlFileName, String version, long executionTime) {
        this.executeSuccess(null, sqlFileName, version, executionTime);
    }

    /**
     * 记录SQL执行成功（带Resource对象）
     */
    public void executeSuccess(Resource resource, String sqlFileName, String version, long executionTime) {
        this.execute(session -> {
            SqlInitializeRepository repository = new SqlInitializeRepository(session);
            String fileHash = sqlResourceHelper.calculateFileHash(resource, sqlFileName);
            try {
                repository.insertSuccessRecord(sqlFileName, version, moduleName, fileHash, executionTime);
                
                // 如果有版本号，更新当前数据库版本（内存中）
                if (StrUtil.isNotBlank(version) && SqlVersionParser.compareVersion(version, currentDbVersion) > 0) {
                    currentDbVersion = version;
                }
            } catch (SQLException e) {
                // 插入失败，可能是字段错误，需要重建表
                log.warn("插入记录失败，可能是表结构问题: {}", e.getMessage());
                throw new RuntimeException("表结构可能不正确，需要重建表", e);
            }
        });
    }

    /**
     * 记录SQL执行失败
     */
    public void executeFailed(String sqlFileName, String version, long executionTime, String errorMessage) {
        this.executeFailed(null, sqlFileName, version, executionTime, errorMessage);
    }

    /**
     * 记录SQL执行失败（带Resource对象）
     */
    public void executeFailed(Resource resource, String sqlFileName, String version, long executionTime, String errorMessage) {
        this.execute(session -> {
            SqlInitializeRepository repository = new SqlInitializeRepository(session);
            String fileHash = sqlResourceHelper.calculateFileHash(resource, sqlFileName);
            try {
                repository.insertFailedRecord(sqlFileName, version, moduleName, fileHash, executionTime, errorMessage);
            } catch (SQLException e) {
                // 插入失败，可能是字段错误，需要重建表
                log.warn("插入失败记录失败，可能是表结构问题: {}", e.getMessage());
                throw new RuntimeException("表结构可能不正确，需要重建表", e);
            }
        });
    }

    /**
     * 扫描并执行SQL文件
     */
    public void scanSqlFiles() throws IOException {
        log.info("开始扫描SQL文件...");
        this.ready();
        
        // 使用Spring的ResourcePatternResolver来扫描资源
        Resource[] resources = sqlResourceHelper.getResourcePatternResolver().getResources(SqlResourceHelper.BASE_SQl_DIR);
        
        if (resources == null || resources.length == 0) {
            log.warn("未找到SQL schema文件，扫描路径: {}", SqlResourceHelper.BASE_SQl_DIR);
            return;
        }
        
        log.info("找到 {} 个SQL文件资源，当前数据库版本: {}", resources.length, currentDbVersion);
        
        // 解析所有SQL文件，提取版本号
        List<SqlFileInfo> sqlFiles = parseSqlFiles(resources);
        
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
        List<SqlFileInfo> toExecuteVersioned = filterVersionedFiles(versionedFiles);
        
        // 过滤需要执行的无版本号文件（按文件名检查是否已执行）
        List<SqlFileInfo> toExecuteUnversioned = filterUnversionedFiles(unversionedFiles);
        
        // 排序
        toExecuteVersioned.sort((f1, f2) -> SqlVersionParser.compareVersion(f1.version, f2.version));
        toExecuteUnversioned.sort(Comparator.comparing(f -> f.fileName));
        
        // 合并：先执行版本化文件，再执行无版本号文件
        List<SqlFileInfo> toExecute = new ArrayList<>();
        toExecute.addAll(toExecuteVersioned);
        toExecute.addAll(toExecuteUnversioned);
        
        if (toExecute.isEmpty()) {
            log.info("没有需要执行的SQL文件 - 总计: {} 个（版本化: {}, 无版本号: {}）", 
                sqlFiles.size(), versionedFiles.size(), unversionedFiles.size());
            return;
        }
        
        log.info("需要执行的SQL文件: {} 个（版本化: {}, 无版本号: {}）", 
            toExecute.size(), toExecuteVersioned.size(), toExecuteUnversioned.size());
        
        // 顺序执行
        executeSqlFiles(toExecute, sqlFiles.size());
    }

    /**
     * 解析SQL文件，提取版本号
     */
    private List<SqlFileInfo> parseSqlFiles(Resource[] resources) {
        List<SqlFileInfo> sqlFiles = new ArrayList<>();
        for (Resource resource : resources) {
            try {
                String fileName = sqlResourceHelper.getSqlFileName(resource);
                if (fileName.equalsIgnoreCase(SQL_INIT_TABLE)) {
                    continue;
                }
                
                String version = SqlVersionParser.parseVersionFromPath(fileName, resource.getFilename());
                sqlFiles.add(new SqlFileInfo(resource, fileName, version));
            } catch (Exception e) {
                log.warn("解析SQL文件失败: {}", resource.getFilename(), e);
            }
        }
        return sqlFiles;
    }

    /**
     * 过滤需要执行的版本化文件
     */
    private List<SqlFileInfo> filterVersionedFiles(List<SqlFileInfo> versionedFiles) {
        List<SqlFileInfo> toExecute = new ArrayList<>();
        for (SqlFileInfo fileInfo : versionedFiles) {
            if (SqlVersionParser.compareVersion(fileInfo.version, currentDbVersion) > 0) {
                toExecute.add(fileInfo);
                log.debug("版本化文件需要执行: {} (版本: {})", fileInfo.fileName, fileInfo.version);
            }
        }
        return toExecute;
    }

    /**
     * 过滤需要执行的无版本号文件
     */
    private List<SqlFileInfo> filterUnversionedFiles(List<SqlFileInfo> unversionedFiles) {
        List<SqlFileInfo> toExecute = new ArrayList<>();
        for (SqlFileInfo fileInfo : unversionedFiles) {
            if (!initializeList.containsKey(fileInfo.fileName)) {
                toExecute.add(fileInfo);
                log.debug("无版本号文件需要执行: {}", fileInfo.fileName);
            }
        }
        return toExecute;
    }

    /**
     * 执行SQL文件列表
     */
    private void executeSqlFiles(List<SqlFileInfo> toExecute, int totalCount) {
        int executedCount = 0;
        int totalFiles = toExecute.size();
        
        for (int i = 0; i < toExecute.size(); i++) {
            SqlFileInfo fileInfo = toExecute.get(i);
            long startTime = System.currentTimeMillis();
            
            try {
                if (StrUtil.isNotBlank(fileInfo.version)) {
                    log.info("执行SQL文件 [{}/{}]: {} (版本: {})", i + 1, totalFiles, fileInfo.fileName, fileInfo.version);
                } else {
                    log.info("执行SQL文件 [{}/{}]: {}", i + 1, totalFiles, fileInfo.fileName);
                }
                
                executeSqlFile(fileInfo.resource, fileInfo.fileName);
                long executionTime = System.currentTimeMillis() - startTime;
                executeSuccess(fileInfo.resource, fileInfo.fileName, fileInfo.version, executionTime);
                executedCount++;
                
                log.info("SQL文件执行成功 [{}/{}]: {} (耗时: {}ms)", 
                    i + 1, totalFiles, fileInfo.fileName, executionTime);
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                String errorMessage = e.getMessage();
                log.error("SQL文件执行失败 [{}/{}]: {} (耗时: {}ms)", 
                    i + 1, totalFiles, fileInfo.fileName, executionTime, e);
                
                // 记录失败信息到数据库
                try {
                    executeFailed(fileInfo.resource, fileInfo.fileName, fileInfo.version, executionTime, errorMessage);
                } catch (Exception ex) {
                    log.error("记录失败信息到数据库异常", ex);
                }
                
                // 执行失败，抛出异常阻止启动
                throw new RuntimeException("SQL文件执行失败，应用启动被阻止: " + fileInfo.fileName + " (版本: " + fileInfo.version + ")", e);
            }
        }
        
        log.info("SQL文件执行完成 - 总计: {}, 已执行: {}, 总用时: {}秒", 
            totalCount, executedCount, stopWatch.getTotalTimeSeconds());
    }

    /**
     * 执行SQL文件
     * @param resource Resource对象（优先使用，支持JAR包）
     * @param fileName 文件名（用于日志和备用读取）
     */
    public void executeSqlFile(Resource resource, String fileName) {
        InputStream initializeSql = null;
        try {
            // 优先使用Resource对象，在JAR包中也能正确读取
            if (resource != null && resource.exists()) {
                initializeSql = resource.getInputStream();
            } else if (StrUtil.isNotBlank(fileName)) {
                // 备用方案：使用Spring的ResourceLoader加载资源
                try {
                    Resource fileResource = sqlResourceHelper.getResourceLoader().getResource(SqlResourceHelper.SQl_DIR + fileName);
                    if (fileResource.exists()) {
                        initializeSql = fileResource.getInputStream();
                    } else {
                        Resource[] resources = sqlResourceHelper.getResourcePatternResolver().getResources("classpath*:sql/schema/" + fileName);
                        if (resources != null && resources.length > 0 && resources[0].exists()) {
                            initializeSql = resources[0].getInputStream();
                        }
                    }
                } catch (Exception e) {
                    log.debug("使用ResourceLoader加载资源失败: {}", fileName, e);
                }
            }
            
            if (initializeSql == null) {
                throw new RuntimeException("无法读取SQL文件: " + fileName);
            }
            
            final InputStream sqlStream = initializeSql;
            this.execute((session) -> {
                try {
                    stopWatch.start(StrUtil.format("执行SQL脚本:{}", fileName));
                    ScriptUtils.executeSqlScript(session.getConnection(), new InputStreamResource(sqlStream));
                    stopWatch.stop();
                } catch (Exception e) {
                    log.error("执行SQL脚本失败: {}", fileName, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            log.error("读取SQL文件失败: {}", fileName, e);
            throw new RuntimeException("读取SQL文件失败: " + fileName, e);
        } finally {
            IoUtil.close(initializeSql);
        }
    }

    /**
     * 执行数据库操作（带事务管理）
     */
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
