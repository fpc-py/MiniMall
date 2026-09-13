# ============ Stage 1: Build with Maven wrapper & JDK 21 ============
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# 复制 Maven wrapper 和 pom.xml（利用 Docker 缓存层）
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn/
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q || true

# 复制源码并编译打包
COPY src src/
RUN ./mvnw package -DskipTests -B -q

# ============ Stage 2: Runtime with JRE 21 ============
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=local"]
