import org.openapitools.generator.gradle.plugin.tasks.GenerateTask


plugins {
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.openapi.generator") version "7.7.0"
    java
}

group = "com.comrades"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.22")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.liquibase:liquibase-core")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Actuator + метрики Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // UUID v7 генератор
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // КРИТИЧНО: binding для совместной работы Lombok + MapStruct
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT (JJWT)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // TestContainers (для будущих интеграционных тестов)
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Современный способ получения пути к директории сборки (Gradle 8+)
val buildDirPath = layout.buildDirectory.get().asFile.path

// Регистрируем задачу БЕЗ делегирования (без 'by'), чтобы избежать конфликтов типов
tasks.named<GenerateTask>("openApiGenerate") {
    generatorName.set("spring")
    inputSpec.set("$rootDir/../api/openapi/chat-api.yaml")
    outputDir.set("$buildDirPath/generated")
    apiPackage.set("com.comrades.chat.api")
    modelPackage.set("com.comrades.chat.dto")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "documentationProvider" to "none",
        "useTags" to "true",
        "dateLibrary" to "java8",
        "useLombokAnnotations" to "true",
        //"additionalModelTypeAnnotations" to "@lombok.AllArgsConstructor @lombok.Builder"
    ))
}

// Подключаем сгенерированный код к основным исходникам
sourceSets {
    main {
        java {
            srcDir("$buildDirPath/generated/src/main/java")
        }
    }
}

// Заставляем компиляцию Java ждать завершения генерации
tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}