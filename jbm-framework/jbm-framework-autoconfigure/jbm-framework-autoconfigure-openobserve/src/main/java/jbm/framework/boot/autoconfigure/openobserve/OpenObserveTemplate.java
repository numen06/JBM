package jbm.framework.boot.autoconfigure.openobserve;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.usage.paging.PageForm;
import jbm.framework.boot.autoconfigure.openobserve.model.PostLogResult;
import jbm.framework.boot.autoconfigure.openobserve.model.Query;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.Charsets;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.MimeTypeUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wesley
 */
@Slf4j
public class OpenObserveTemplate implements InitializingBean {

    // 共享的 OkHttpClient 实例（推荐单例）
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            // 任务提交和查询都是轻量操作
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build();

    private final OpenObserveProperties openObserveProperties;

    final String credential;

    public OpenObserveTemplate(OpenObserveProperties openObserveProperties) {
        this.openObserveProperties = openObserveProperties;
        credential = HttpUtil.buildBasicAuth(openObserveProperties.getUsername(), openObserveProperties.getPassword(), Charsets.UTF_8);
    }

    /**
     * 发送日志
     *
     * @param log
     * @param stream
     */
    public void postLog(Object log, String stream) {
        if (log instanceof String) {
            this.postLogStr((String) log, stream);
            return;
        }
        postLogs(CollUtil.newArrayList(log), stream);
    }

    /**
     * 发送日志
     *
     * @param logs
     * @param stream
     */
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


    /**
     * 发送日志
     *
     * @param json
     * @param stream
     */
    public void postLogStr(String json, String stream) {
        String firstChar = StrUtil.sub(json, 0, 1);
        StringBuilder sb = new StringBuilder(json);
        //如果不是数组则组成数组
        if ("{".equals(firstChar)) {
            sb.insert(0, "[");
            sb.append("]");
        }
        final String s = StrUtil.isNotEmpty(stream) ? stream : openObserveProperties.getStream();
        UrlBuilder urlBuilder = UrlBuilder.of(openObserveProperties.getUrl())
                .addPath("api/")
                .addPathSegment(openObserveProperties.getOrganization())
                .addPathSegment(StrUtil.toUnderlineCase(s))
                .addPathSegment("_json");
        final String url = urlBuilder.build();
        // 构建 Request Body
        RequestBody requestBody = RequestBody.create(
                sb.toString(),
                MediaType.get(MimeTypeUtils.APPLICATION_JSON_VALUE)
        );
        // 构建请求
        Request request = this.getBaseRequest(url)
                .post(requestBody)
                .build();
        // 同步发送
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // 网络错误、连接失败等
                log.error("发送日志失败（网络错误）: {}", e.getMessage(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("发送日志失败，HTTP状态码: {}, 响应: {}", response.code(), responseBody);
                    // 可选：抛出异常或通知上层
                } else {
                    // 解析成功响应
                    try {
                        PostLogResult postLogResult = JSON.parseObject(responseBody, PostLogResult.class);
                        log.info("发送日志成功: {}条, 失败: {}条。", postLogResult.getAllSuccessful(), postLogResult.getAllFailed());
                    } catch (Exception parseException) {
                        log.error("解析响应失败: {}", responseBody, parseException);
                    }
                }
                response.close();
            }
        });
    }


    private Request.Builder getBaseRequest(String url) {
        // 构建请求
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", credential);
    }


//    public HttpRequest getRequest(String url) {
//        return getRequest(url, Method.POST, ContentType.JSON);
//    }
//
//    public HttpRequest getRequest(String url, Method method, ContentType contentType) {
//        HttpRequest request = HttpUtil.createRequest(method, url);
//        request.contentType(contentType.getValue());
//        try {
//            request.cookie(new HttpCookie("auth_tokens", opobserveTokenManager.getTokenValue()));
//        } catch (Exception e) {
//            log.error("获取token失败", e);
//        }
//        return request;
//    }

    public QueryResult selectLogs(String statement, Map<String, Object> params, PageForm pageForm) {
        return selectLogs(statement, params, null, null, pageForm);
    }

    public QueryResult selectLogs(String statement, Map<String, Object> params, Date beginTime, Date endTime, PageForm pageForm) {
        QueryBean queryBean = QueryBean.defaultQuery(statement, params);
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

    private void initSql(QueryBean queryBean) {
        if (StrUtil.isNotEmpty(queryBean.getQuery().getSql())) {
            return;
        }
        String sql = this.getGeneratedSql(queryBean.getStatement(), queryBean.getParams());
        queryBean.getQuery().setSql(sql);
        queryBean.setStatement(null);
        queryBean.setParams(null);
        if (queryBean.getQuery().getStartTime() != null) {
            queryBean.getQuery().setStartTime(ensureMicrosTimestamp(queryBean.getQuery().getStartTime()));
        }
        if (queryBean.getQuery().getEndTime() != null) {
            queryBean.getQuery().setEndTime(ensureMicrosTimestamp(queryBean.getQuery().getEndTime()));
        }

    }

    private static long ensureMicrosTimestamp(long timestamp) {
        // 获取数字的位数
        int digits = (int) (Math.log10(timestamp) + 1);

        if (timestamp < 0) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        }

        if (digits == 16) {
            // 已经是微秒级（如：1758091852985000）
            return timestamp;
        } else if (digits == 13) {
            // 是毫秒级（如：1758091852985），转为微秒
            return timestamp * 1000L;
        } else if (digits == 10) {
            // 是秒级（如：1758091852），转为微秒
            return timestamp * 1_000_000L;
        } else if (digits == 19) {
            // 是纳秒级（如：1758091852985000000），可选择截断或保留
            // 根据需求决定是否转为微秒（除以 1000）
            return timestamp / 1000;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported timestamp format (digits: " + digits + "). Expected 10, 13, 16, or 19 digits.");
        }
    }

    public QueryResult selectLogs(QueryBean queryBean) {
        // 构建 URL
        UrlBuilder urlBuilder = UrlBuilder.of(openObserveProperties.getUrl())
                .addPath("/api/")
                .addPathSegment(openObserveProperties.getOrganization())
                .addPath("/_search");

        initSql(queryBean);
        // 构建请求体
        String requestBodyStr = JSON.toJSONString(queryBean);

//        log.info("请求信息: {}", requestBodyStr);
        RequestBody requestBody = RequestBody.create(
                requestBodyStr,
                MediaType.get(MimeTypeUtils.APPLICATION_JSON_VALUE)
        );
        // 构建请求
        Request request = getBaseRequest(urlBuilder.build()).post(requestBody).build();
        QueryResult queryResult = call(request, QueryResult.class);
        // 设置扫描记录数
        Long total = selectCount(queryBean);
        queryResult.setScanRecords(total);
        return queryResult;
    }

    private <T> T call(Request request, Class<T> clazz) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw ServiceException.of("响应体为空");
            }
            String responseBody = body.string();
            if (response.isSuccessful()) {
                return JSON.parseObject(responseBody, clazz);
            } else {
                throw ServiceException.of("请求失败: " + response.code() + ", 响应: " + responseBody);
            }
        } catch (IOException e) {
            log.error("网络请求异常: {}", request.url(), e);
            throw ServiceException.of("网络请求异常: " + e.getMessage());
        }
    }

    public Long selectCount(QueryBean queryBean) {
        Query query = BeanUtil.copyProperties(queryBean.getQuery(), Query.class);
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
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(openObserveProperties.getUrl())
                .addPath("/api")
                .addPathSegment(openObserveProperties.getOrganization())
                .addPathSegment("_search");
        QueryBean countQueryBean = new QueryBean();
        countQueryBean.setQuery(query);
        countQueryBean.getQuery().setFrom(0);
        countQueryBean.getQuery().setSize(-1);
        String requestBodyStr = JSON.toJSONString(countQueryBean);

        RequestBody requestBody = RequestBody.create(
                requestBodyStr,
                MediaType.get(MimeTypeUtils.APPLICATION_JSON_VALUE)
        );

        // 构建请求
        Request request = getBaseRequest(urlBuilder.build()).post(requestBody).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                QueryResult queryResult = JSON.parseObject(responseBody, QueryResult.class);
                List<Map<String, Object>> hits = queryResult.getHits();
                Long total = 0L;
                if (CollUtil.isNotEmpty(hits)) {
                    Map<String, Object> map = CollUtil.getFirst(hits);
                    JSONObject jsonObject = new JSONObject(map);
                    total = jsonObject.getLong("zo_sql_num");
                }
                return total == null ? 0L : total;
            } else {
                // ❌ 请求失败
                String errorBody = response.body() != null ? response.body().string() : "未知错误";
                log.error("请求信息: {}", requestBodyStr);
                log.error("错误信息: HTTP {} - {}", response.code(), errorBody);

                return 0L;
            }

        } catch (IOException e) {
            log.error("请求异常: {}", requestBodyStr, e);
            throw ServiceException.of("网络请求异常: " + e.getMessage());
        }
    }


    private SqlSessionFactory sqlSessionFactory;

    public void initMapper() {
        try {
            // 1. 初始化 H2 内存数据库
            String jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
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
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statement).getBoundSql(params);
            return applyParameters(boundSql, params, sqlSession.getConfiguration());
        }
    }

    private String applyParameters(BoundSql boundSql, Map<String, Object> originalParams, Configuration configuration) {
        String sql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (CollUtil.isEmpty(parameterMappings)) {
            return sql;
        }

        Object parameterObject = boundSql.getParameterObject();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        org.apache.ibatis.reflection.MetaObject metaObject =
                parameterObject == null ? null : configuration.newMetaObject(parameterObject);

        for (ParameterMapping parameterMapping : parameterMappings) {
            String propertyName = parameterMapping.getProperty();
            Object value;

            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else if (metaObject != null && metaObject.hasGetter(propertyName)) {
                value = metaObject.getValue(propertyName);
            } else if (originalParams != null && originalParams.containsKey(propertyName)) {
                value = originalParams.get(propertyName);
            } else {
                value = null;
            }

            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(formatParameter(value)));
        }
        return sql;
    }

    private String formatParameter(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Date) {
            return "'" + DateUtil.format((Date) value, DatePattern.NORM_DATETIME_PATTERN) + "'";
        }
        String stringValue = String.valueOf(value).replace("'", "''");
        return "'" + stringValue + "'";
    }

    /**
     * 创建或更新流的保留策略（TTL）
     * OpenObserve会根据保留策略自动删除过期数据
     * 
     * @param streamName 流名称
     * @param retentionDays 保留天数（TTL），null表示永不过期
     * @return 是否成功
     */
    public boolean createOrUpdateStreamRetention(String streamName, Integer retentionDays) {
        try {
            // OpenObserve API: PUT /api/{org}/{stream}
            // 设置流的保留策略（TTL）
            UrlBuilder urlBuilder = UrlBuilder.of(openObserveProperties.getUrl())
                    .addPath("/api/")
                    .addPathSegment(openObserveProperties.getOrganization())
                    .addPathSegment(StrUtil.toUnderlineCase(streamName));
            
            // 构建请求体：设置保留策略
            Map<String, Object> streamConfig = new HashMap<>();
            streamConfig.put("name", StrUtil.toUnderlineCase(streamName));
            streamConfig.put("type", "logs"); // 日志类型
            
            // 如果指定了保留天数，设置TTL（单位：秒）
            if (retentionDays != null && retentionDays > 0) {
                long retentionSeconds = retentionDays * 24L * 60L * 60L;
                streamConfig.put("retention_period", retentionSeconds);
                log.info("设置流 {} 的保留策略为 {} 天（{}秒）", streamName, retentionDays, retentionSeconds);
            } else {
                log.info("设置流 {} 为永不过期", streamName);
            }
            
            String requestBodyStr = JSON.toJSONString(streamConfig);
            RequestBody requestBody = RequestBody.create(
                    requestBodyStr,
                    MediaType.get(MimeTypeUtils.APPLICATION_JSON_VALUE)
            );
            
            Request request = getBaseRequest(urlBuilder.build())
                    .put(requestBody)
                    .build();
            
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("成功创建/更新流 {} 的保留策略", streamName);
                    return true;
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    log.warn("创建/更新流 {} 保留策略失败: HTTP {} - {}", streamName, response.code(), errorBody);
                    // 流可能已存在，这是正常的，不算错误
                    return response.code() == 409 || response.code() == 200;
                }
            }
        } catch (Exception e) {
            log.error("创建/更新流 {} 保留策略异常", streamName, e);
            return false;
        }
    }
    
    /**
     * 确保流存在并配置了正确的保留策略
     * 
     * @param streamName 流名称
     * @param retentionDays 保留天数
     */
    public void ensureStreamWithRetention(String streamName, Integer retentionDays) {
        createOrUpdateStreamRetention(streamName, retentionDays);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initMapper();
    }

}
