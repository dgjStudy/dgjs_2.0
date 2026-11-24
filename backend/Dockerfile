FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 레이어를 소스보다 먼저 만들어 코드만 바뀌면 재다운로드를 피함
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# 윈도우에서 체크아웃된 CRLF 줄바꿈이 있어도 실행되도록 정리
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home --shell /usr/sbin/nologin dgjs
COPY --from=build /app/app.jar app.jar
USER dgjs

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
