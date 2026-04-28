plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":services"))

    implementation("org.springframework:spring-webmvc:6.2.11")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    implementation("org.slf4j:slf4j-api:2.0.16")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    testImplementation("org.springframework:spring-test:6.2.11")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
