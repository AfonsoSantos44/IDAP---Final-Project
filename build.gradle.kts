plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
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