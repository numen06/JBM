CREATE STABLE `gateway_logs`(
    request_time TIMESTAMP,     -- 时间戳（主时间戳）
    access_id VARCHAR(64),      -- 访问ID
    log_level INT,              -- 日志等级（DEBUG=0, INFO=1, WARN=2, ERROR=3）
    path NCHAR(1024),           -- 请求路径
    api_path VARCHAR(256),       -- 接口路径
    request_user_id BIGINT,     -- 请求人ID
    request_real_name VARCHAR(128), -- 请求人姓名
    api_name VARCHAR(256),       -- 接口名称
    operation_type VARCHAR(64),  -- 操作类型
    app_name VARCHAR(128),       -- 应用名称
    method VARCHAR(32),          -- 请求方法（GET/POST等）
    ip VARCHAR(64),              -- 请求IP
    http_status INT,            -- HTTP响应状态码
    response_time TIMESTAMP,    -- 响应时间
    response_body NCHAR(4096),  -- 响应体
    use_time BIGINT,            -- 耗时（毫秒）
    params NCHAR(4096),         -- 请求参数
    headers NCHAR(4096),        -- 请求头
    user_agent NCHAR(1024),     -- 用户代理
    region VARCHAR(64),          -- 区域
    authentication VARCHAR(256),  -- 认证信息
    error NCHAR(256)
)TAGS (
    api_id                      VARCHAR(100),
    app_id                      VARCHAR(100),
    app_key                     VARCHAR(100),
    service_id                  VARCHAR(100)
);
