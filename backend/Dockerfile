# ============ Stage 1: Build ============
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn/
COPY pom.xml .

# alpine sed清理windows换行\r
RUN sed -i 's/\r//' mvnw

# 使用 sh 而不是 bash，alpine自带sh
RUN sh mvnw dependency:go-offline -B -q

COPY src src/
RUN sh mvnw package -DskipTests -B -q

# ============ Stage 2: Runtime with JRE 21 ============
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 设置上海时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# 连接信息由 docker-compose 通过环境变量注入；本地私有配置不随镜像分发
ENTRYPOINT ["java", "-jar", "app.jar"]
