package com.jbm.cluster.logs.utils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wesley
 */
public class MybatisUtils {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            // 1. 初始化 H2 内存数据库
            String jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
            DataSource dataSource = new UnpooledDataSource("org.h2.Driver", jdbcUrl, null, null);
            // 2. 创建事务工厂
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            // 3. 构建 Environment
            Environment environment = new Environment("h2", transactionFactory, dataSource);
            // 4. 创建 Configuration 对象
            Configuration configuration = new Configuration(environment);
            configuration.setMapUnderscoreToCamelCase(true);
            // 5. 扫描 mapper/*.xml 文件并加载
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:mapper/**/*.xml");
            for (Resource resource : resources) {
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(resource.getInputStream(), configuration, resource.getFilename(), configuration.getSqlFragments());
                xmlMapperBuilder.parse();
            }
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        } catch (IOException e) {
            throw new RuntimeException("加载 MyBatis 配置文件失败", e);
        }
    }

    public static String getGeneratedSql(String statement, Map<String, Object> params) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            // 获取 Mapper 接口的代理对象
//            Object mapper = sqlSession.getMapper(Class.forName(statement.split("\\.")[0]));
            // 获取 BoundSql 对象，包含生成的 SQL 语句和参数信息
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statement).getBoundSql(params);
            // 返回生成的 SQL 语句
            return boundSql.getSql();
        }
    }

    public static void main(String[] args) {
        // 示例：生成查询语句
        Map<String, Object> params = new HashMap<>();
        params.put("appId", "张三");
        String sql = getGeneratedSql("com.jbm.cluster.logs.mapper.GatewayLogsMapper.selectLogs", params);
        System.out.println("生成的 SQL 语句：\n" + sql);
    }
}
