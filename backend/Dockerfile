# 多阶段构建：Maven 编译 → JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 8080 5005
HEALTHCHECK --interval=10s --timeout=5s --retries=12 --start-period=60s \
  CMD curl -f http://localhost:8080/api/health/live || exit 1
ENTRYPOINT ["/docker-entrypoint.sh"]
