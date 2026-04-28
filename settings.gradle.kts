rootProject.name = "idap-project"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.25"
        kotlin("plugin.spring") version "1.9.25"
        kotlin("plugin.serialization") version "1.9.25"
    }
}

include(
    "app",
    "domain",
    "http",
    "repository", 
    "repository-jdbi",
    "services"
)

project(":app").projectDir = file("code/jvm/app")
project(":domain").projectDir = file("code/jvm/domain")
project(":http").projectDir = file("code/jvm/http")
project(":repository").projectDir = file("code/jvm/repository") 
project(":repository-jdbi").projectDir = file("code/jvm/repository-jdbi")
project(":services").projectDir = file("code/jvm/services")