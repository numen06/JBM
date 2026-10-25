# 登录 IP 属地离线数据

使用 [ip2region](https://github.com/lionsoul2014/ip2region) 的 IPv4、IPv6 XDB，
通过固定提交与 SHA-256 校验下载；不向外部服务发送用户登录 IP。

在 `jbm-python-cluster` 下执行 `python auth/fetch_ip_regions.py` 准备数据。
Docker 构建会执行同一步骤。XDB 不提交 Git，许可证见本目录。

属地只用于提示：比较本账号最近有已知属地的成功登录，未知属地和单纯 IP
变化不会触发异地提醒。VPN、代理、移动网络可能影响判断。失败登录仍记日志。

反向代理须保留并追加 `X-Forwarded-For`。`jbm.auth.login-security.trusted-proxy-networks`
默认只信任回环与 Docker 常用的 `172.16.0.0/12` 网段，生产应按实际代理范围收紧。
`geo-data-directory` 可指定 XDB 目录；`geo-enabled: false` 仅用于无需属地分析的环境。

升级前先执行 Center 的 `20261023_26` 数据库迁移，再启动新版 Auth。
