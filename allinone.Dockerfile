# ------------------------------------------------------------
# Stage 1: 构建阶段 — 使用 Dragonwell + 手动安装 Maven（国内源）
# ------------------------------------------------------------
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:8-anolis AS builder

WORKDIR /app

# 安装 wget、unzip（用于下载和解压 Maven）
RUN set -xeuo pipefail && \
    dnf makecache --refresh && \
    dnf install -y \
        --noplugins \
        --setopt=install_weak_deps=False \
        --setopt=skip_if_unavailable=True \
        --setopt=tsflags=nodocs \
        --nogpgcheck \
        wget unzip && \
    dnf clean all && \
    rm -rf /var/cache/dnf* /tmp/* /var/tmp/*

# 从阿里云镜像下载 Maven（加速）
ENV MAVEN_VERSION=3.9.2
ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

RUN wget https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip -O /tmp/maven.zip && \
    unzip /tmp/maven.zip -d /opt && \
    mv /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME} && \
    rm /tmp/maven.zip

# 复制代码并构建
COPY . .

# 执行构建，跳过测试（生产环境可移除）指定setting.xml
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -s settings.xml

# ------------------------------------------------------------
# 公共运行基础（使用轻量 Dragonwell JRE）
# 注意：官方未提供 "jre-alpine" 版本，这里使用完整 JDK 但仅运行 jar（可接受）
# 或者改用 eclipse-temurin:jre-alpine 以减小体积（见下方说明）
# ------------------------------------------------------------
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:11-anolis AS base

WORKDIR /app

ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
VOLUME /tmp
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

#安装字体库
RUN yum install -y fontconfig
#安装网络工具
RUN yum install -y lrzsz net-tools vim wget

# ------------------------------------------------------------
# 每个服务 stage：硬编码 JAR 名（零变量依赖，buildx 100% 可识别）
# ------------------------------------------------------------


FROM base AS jbm-cluster-platform-auth
COPY --from=builder /app/dist/jbm-cluster-platform-auth.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-bigscreen
COPY --from=builder /app/dist/jbm-cluster-platform-bigscreen.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-center
COPY --from=builder /app/dist/jbm-cluster-platform-center.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-doc
COPY --from=builder /app/dist/jbm-cluster-platform-doc.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-gateway
COPY --from=builder /app/dist/jbm-cluster-platform-gateway.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-job
COPY --from=builder /app/dist/jbm-cluster-platform-job.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-logs
COPY --from=builder /app/dist/jbm-cluster-platform-logs.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-push
COPY --from=builder /app/dist/jbm-cluster-platform-push.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM base AS jbm-cluster-platform-weixin
COPY --from=builder /app/dist/jbm-cluster-platform-weixin.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]