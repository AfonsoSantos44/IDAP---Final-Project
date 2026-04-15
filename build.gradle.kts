plugins {
    kotlin("jvm") version "2.0.0" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm") 

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"(kotlin("stdlib"))
        "testImplementation"(kotlin("test"))
    }
}