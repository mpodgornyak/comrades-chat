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
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.liquibase:liquibase-core")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

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
        "additionalModelTypeAnnotations" to "@lombok.Builder"
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