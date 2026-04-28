plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
}

group = "pt.isel"
version = "0.0.1-SNAPSHOT"
description = "Final project"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm") 

    repositories {
        mavenCentral()
    }

}