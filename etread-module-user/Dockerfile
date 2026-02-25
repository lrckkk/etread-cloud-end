# 1. 基础镜像：用 Java 17
FROM openjdk:17-jdk-slim

# 2. 这里的参数指向你 Maven 打包出来的 jar 包
ARG JAR_FILE=target/*.jar

# 3. 把打包好的 jar 复制进容器，改名 app.jar
COPY ${JAR_FILE} app.jar

# 4. 暴露 8081 端口
EXPOSE 8081

# 5. 启动指令
ENTRYPOINT ["java", "-jar", "/app.jar"]