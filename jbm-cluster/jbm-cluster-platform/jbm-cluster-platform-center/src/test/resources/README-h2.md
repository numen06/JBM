# Center H2 本地验证

```bash
# RBAC 初始化 + 超管菜单权限
mvn test -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am -Dtest=CenterRbacH2IT

# API Key BCrypt 校验（common-mysql 模块）
mvn test -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql -am -Dtest=OAuthClientSecretVerifierH2IT

# 本地启动（profile h2，不依赖 Nacos/MySQL）
mvn -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am spring-boot:run -Dspring-boot.run.profiles=h2
```

默认超管：`admin` / `admin123`（可通过 `jbm.cluster.data-init.root-password` 覆盖）。
