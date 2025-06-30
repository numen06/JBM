package jbm.framework.boot.autoconfigure.openobserve;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.usage.paging.PageForm;
import jbm.framework.boot.autoconfigure.openobserve.model.Query;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class OpenObserveTemplate implements InitializingBean {

    private final OpenObserveProperties openObserveProperties;

    public OpenObserveTemplate(OpenObserveProperties openObserveProperties) {
        this.openObserveProperties = openObserveProperties;

    }

    public void postLog(Object log, String stream) {
        if (log instanceof String) {
            this.postLogStr((String) log, stream);
            return;
        }
        postLogs(CollUtil.newArrayList(log), stream);
    }

    public void postLogs(List<?> logs, String stream) {
//        if (logs.size() == 1) {
//            Object log = logs.get(0);
//            if (log instanceof String) {
//                this.postLogStr((String) log);
//                return;
//            }
//        }
        // 配置fastjson
        SerializeConfig config = new SerializeConfig();
        config.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
        // 转换为下划线命名的JSON字符串
        String json = JSON.toJSONString(logs, config);
        this.postLogStr(json, stream);
    }

    private String auth_tokens = null;

    public void login() {
        String url = StrUtil.format("{}/auth/login", openObserveProperties.getUrl());
        HttpRequest request = HttpUtil.createPost(url);
        request.contentType("application/json");
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("name", openObserveProperties.getUsername());
        loginInfo.put("password", openObserveProperties.getPassword());
        request.body(JSON.toJSONString(loginInfo));
        try (HttpResponse response = request.execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw ServiceException.of("登录错误");
            } else {
                String body = response.body();
                JSONObject jsonObject = JSON.parseObject(body);
                boolean status = jsonObject.getBoolean("status");
                if (!status) {
                    throw ServiceException.of("认证失败");
                }
            }

            auth_tokens = response.getCookieValue("auth_tokens");
        }
    }

    public void postLogStr(String json, String stream) {
        String firstChar = StrUtil.sub(json, 0, 1);
        StringBuilder sb = new StringBuilder(json);
        //如果不是数组则组成数组
        if (firstChar.equals("{")) {
            sb.insert(0, "[");
            sb.append("]");
        }
        final String s = StrUtil.isNotEmpty(stream) ? stream : openObserveProperties.getStream();
        final String url = StrUtil.format("{}/api/{}/{}/_json", openObserveProperties.getUrl(), openObserveProperties.getOrganization(), StrUtil.toUnderlineCase(s));
        HttpRequest request = HttpUtil.createPost(url).basicAuth(openObserveProperties.getUsername(), openObserveProperties.getPassword());
        request.contentType("application/json");
        request.body(sb.toString());
        HttpResponse response = request.executeAsync();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            log.error("错误信息:{}", response.body());
            throw ServiceException.of(response.body());
        }
//        log.info("发送成功:{}", response.body());
    }

    public HttpRequest getRequest(String url) {
        return getRequest(url, Method.POST, ContentType.JSON);
    }

    public HttpRequest getRequest(String url, Method method, ContentType contentType) {
        HttpRequest request = HttpUtil.createRequest(method, url);
        request.contentType(contentType.getValue());
        if (auth_tokens == null) {
            login();
        }
        request.cookie(new HttpCookie("auth_tokens", auth_tokens));
        return request;
    }

    public QueryResult selectLogs(String statement, Map<String, Object> params, PageForm pageForm) {
        return selectLogs(statement, params, null, null, pageForm);
    }

    public QueryResult selectLogs(String statement, Map<String, Object> params, Date beginTime, Date endTime, PageForm pageForm) {
        QueryBean queryBean = new QueryBean();
        String sql = getGeneratedSql(statement, params);
        queryBean.getQuery().setSql(sql);
        int from = (pageForm.getCurrPage() - 1) * pageForm.getPageSize();
        queryBean.getQuery().setFrom(Math.max(from, 0));
        queryBean.getQuery().setSize(pageForm.getPageSize());
        // 初始化时间
        if (beginTime == null) {
            beginTime = DateUtil.offsetDay(DateUtil.date(), -1);
        }
        if (endTime == null) {
            endTime = DateUtil.date();
        }
        queryBean.getQuery().setStartTime(beginTime.getTime() * 1000);
        queryBean.getQuery().setEndTime(endTime.getTime() * 1000);
        return selectLogs(queryBean);
    }

    public QueryResult selectLogs(QueryBean queryBean) {
        String url = StrUtil.format("{}/api/{}/_search", openObserveProperties.getUrl(), openObserveProperties.getOrganization());
        HttpRequest request = getRequest(url);
//        JSONObject queryBeanJson = new JSONObject();
//        queryBeanJson.put("query", queryBean);
        String requestBody = JSON.toJSONString(queryBean);
        request.body(requestBody);
        String body;
        try (HttpResponse response = request.execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                log.error("请求信息:{}", requestBody);
                log.error("错误信息:{}", response.body());
                throw ServiceException.of(response.body());
            }
            body = response.body();
        }
        QueryResult queryResult = JSON.parseObject(body, QueryResult.class);
        Long total = this.selectCount(queryBean);
        queryResult.setScanRecords(total);
//        queryBean.getQuery().setSqlMode("full");
//        queryBean.getQuery().setStreamingOutput( true);
//        PartitionResult partitionResult = this.selectCount(queryBean.getQuery());
//        queryResult.setTotal(partitionResult.getRecords());
        return queryResult;
    }

    public Long selectCount(QueryBean queryBean) {
        Query query = queryBean.getQuery();
        // 正则说明：
        // - (?i) 忽略大小写
        // - SELECT\s+ 匹配 SELECT 和后面的一个或多个空格
        // - (.*?) 非贪婪匹配 SELECT 和 FROM 之间的内容
        // - \s+FROM 匹配 FROM 前面可能有的空格和 FROM 关键字
        String regex = "(?i)(SELECT\\s+)(.*?)(\\s+FROM)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(queryBean.getQuery().getSql());

        // 替换 SELECT 后的字段为 COUNT(*)
        String newSql = matcher.replaceFirst("$1COUNT(*) AS zo_sql_num $3");

//        log.info("原始SQL:{}", queryBean.getQuery().getSql());
//        log.info("替换后SQL:{}", newSql);
        query.setSql(newSql);
        String url = StrUtil.format("{}/api/{}/_search", openObserveProperties.getUrl(), openObserveProperties.getOrganization());
        HttpRequest request = getRequest(url);
        queryBean.getQuery().setFrom(0);
        queryBean.getQuery().setSize(-1);
        String requestBody = JSON.toJSONString(queryBean);
        request.body(requestBody);
        String body;
        try (HttpResponse response = request.execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
//                log.error("请求信息:{}", requestBody);
//                log.error("错误信息:{}", response.body());
                throw ServiceException.of(response.body());
            }
            body = response.body();
        }
        QueryResult queryResult = JSON.parseObject(body, QueryResult.class);
        List<Map<String, Object>> hits = queryResult.getHits();
        Long total = 0L;
        if (CollUtil.isNotEmpty(hits)) {
            Map<String, Object> map = CollUtil.getFirst(hits);
            JSONObject jsonObject = new JSONObject(map);
            total = jsonObject.getLong("zo_sql_num");
        }
        return total;
    }


    private SqlSessionFactory sqlSessionFactory;

    public void initMapper() {
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

    private String getGeneratedSql(String statement, Map<String, Object> params) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            // 获取 Mapper 接口的代理对象
//            Object mapper = sqlSession.getMapper(Class.forName(statement.split("\\.")[0]));
//            if( params ==null ){
//                params = new HashMap<>();
//            }
            // 获取 BoundSql 对象，包含生成的 SQL 语句和参数信息
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statement).getBoundSql(params);
            // 返回生成的 SQL 语句
            return boundSql.getSql();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initMapper();
    }
}
