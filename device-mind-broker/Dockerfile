# ============================================================
# DeviceMind Broker — MQTT 接入服务
# 构建: docker build -t devicemind/broker -f device-mind-broker/Dockerfile .
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 1. 只拷贝 POM，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY device-mind-common/pom.xml device-mind-common/pom.xml
COPY device-mind-broker/pom.xml device-mind-broker/pom.xml
RUN mvn dependency:go-offline -B

# 2. 拷贝源码并编译
COPY . .
RUN mvn clean package -DskipTests -pl device-mind-broker -am

# 3. 运行镜像
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/device-mind-broker/target/*.jar app.jar

EXPOSE 1883
EXPOSE 1884

ENTRYPOINT ["java", "-jar", "app.jar"]
