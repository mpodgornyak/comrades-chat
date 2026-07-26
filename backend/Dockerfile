# Этап 1: Сборка приложения
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace/app

# Копируем только gradle wrapper и скрипты для кэширования зависимостей
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Делаем gradlew исполняемым и скачиваем зависимости (этот слой закэшируется)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Копируем исходный код
COPY src src

# Собираем JAR без тестов
RUN ./gradlew bootJar -x test --no-daemon

# Этап 2: Запуск приложения (легкий образ с JRE)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный JAR из этапа builder
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]