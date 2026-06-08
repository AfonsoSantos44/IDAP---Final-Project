import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
}

group = "pt.isel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    implementation(project(":repository"))
    implementation(project(":domain"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
    jvmToolchain(21)
}
