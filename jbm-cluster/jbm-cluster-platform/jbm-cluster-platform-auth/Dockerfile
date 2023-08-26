FROM registry.cn-shanghai.aliyuncs.com/numen/spring-boot-dev:dragonwell8
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar
