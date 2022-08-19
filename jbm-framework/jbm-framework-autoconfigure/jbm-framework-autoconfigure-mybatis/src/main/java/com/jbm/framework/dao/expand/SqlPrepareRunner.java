package com.jbm.framework.dao.expand;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Session;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.extra.spring.SpringUtil;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class SqlPrepareRunner {


    private final static String SELECT_INIT_TABLE = "SELECT COUNT(*) FROM information_schema.TABLES WHERE table_name ='sql_initialize';";
    private final static String INSERT_SQL_FILE = "INSERT INTO `sql_initialize` (`file_name`, `create_time`) VALUES (?, ?);";
    private final static String FIND_SQL_FILES = "SELECT * FROM `sql_initialize`";

    private final static String DELETE_SQL_FILE = "DELETE FROM `sql_initialize` WHERE `file_name` = ? ;";


    private final static String SQL_INIT_TABLE = "sql_initialize.sql";
    public final static String SQl_DIR = "classpath*:sql/schema/";

    public final static String BASE_SQl_DIR = SQl_DIR + "/**/**/*.sql";
    private final DataSource ds;


    private final Map<String, SqlInitialize> initializeList = new ConcurrentHashMap<>();

    public SqlPrepareRunner(DataSource ds) {
        this.ds = ds;
    }

    public void ready() {
        this.execute(session -> {
            try {
                ResultSet rs = SqlExecutor.callQuery(session.getConnection(), SELECT_INIT_TABLE);
                int count = 0;
                while (rs.next()) {
                    count = rs.getInt(1);
                }
                if (count < 1) {
                    executeSqlFile(SQL_INIT_TABLE);
                }
            } catch (Exception e) {
                log.error("执行初始化失败", e);
                throw new RuntimeException(e);
            }
        });
        this.execute(session -> {
            try {
                List<Entity> list = session.query(FIND_SQL_FILES);
                list.forEach(entity -> {
                    SqlInitialize sqlInitialize = new SqlInitialize();
                    entity.toBeanWithCamelCase(sqlInitialize);
                    sqlInitialize.setCreateTime(entity.getDate("create_time"));

                    initializeList.put(sqlInitialize.getFileName(), sqlInitialize);
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }


    public void executeSuccess(String sqlFileName) {
        this.execute(session -> {
            try {
                int result = session.execute(INSERT_SQL_FILE, sqlFileName, DateUtil.now());
                if (result < 1) {
                    throw new RuntimeException("插入记录失败");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }


    private String getSqlFileName(Resource resource) throws IOException {
        String baseDir = SpringUtil.getApplicationContext().getResource(SQl_DIR).getURL().toString();
        String filePath = resource.getURL().toString();
        String fileName = StrUtil.removePrefix(filePath, baseDir);
        return fileName;
    }

    private StopWatch stopWatch = new StopWatch("扫描SQL文件");

    /**
     * 扫描SQL语句
     */
    public void scanSqlFiles() throws IOException {
        this.ready();
        Resource[] resources = SpringUtil.getApplicationContext().getResources(BASE_SQl_DIR);
        for (Resource resource : resources) {
            try {
                String fileName = getSqlFileName(resource);
                if (fileName.equalsIgnoreCase(SQL_INIT_TABLE)) {
                    continue;
                }
                //已经存在了直接跳过
                if (initializeList.containsKey(fileName)) {
                    continue;
                }
                executeSqlFile(fileName);
                executeSuccess(fileName);
            } catch (Exception e) {
            }
        }
        log.info("扫描SQL文件结束,用时:{}秒", stopWatch.getTotalTimeSeconds());
        if (stopWatch.getTaskCount() > 0) {
            log.info(stopWatch.prettyPrint(TimeUnit.SECONDS));
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
        } finally {
            session.close();
        }
    }


}
