# ============================================================
# DeviceMind Agent — AI Agent 服务
# 构建: docker build -t devicemind/agent -f device-mind-agent/Dockerfile .
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 1. 只拷贝 POM，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY device-mind-common/pom.xml device-mind-common/pom.xml
COPY device-mind-agent/pom.xml device-mind-agent/pom.xml
RUN mvn dependency:go-offline -B

# 2. 拷贝源码并编译
COPY . .
RUN mvn clean package -DskipTests -pl device-mind-agent -am

# 3. 运行镜像
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/device-mind-agent/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
