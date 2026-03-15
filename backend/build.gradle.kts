import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("org.graalvm.buildtools.native") version "0.9.28"
}

group = "com.techmarket"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudGcpVersion"] = "5.1.0"
extra["gcpCloudTasksVersion"] = "2.41.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.google.cloud:spring-cloud-gcp-starter-bigquery")
    implementation("com.google.cloud:spring-cloud-gcp-starter-storage")
    implementation("com.google.cloud:google-cloud-tasks:${property("gcpCloudTasksVersion")}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.mockk:mockk:1.13.9")
}

dependencyManagement {
    imports {
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${property("springCloudGcpVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = file("../.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotBlank() && !trimmedLine.startsWith("#")) {
                val parts = trimmedLine.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    if (value.startsWith("\"") && value.endsWith("\"") || 
                        value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length - 1)
                    }
                    environment(key, value)
                }
            }
        }
    }
}
