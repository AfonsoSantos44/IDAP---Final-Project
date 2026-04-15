rootProject.name = "idap-project"

include(
    "app",
    "domain",
    "http",
    "repository-jdbi",
    "services"
)

project(":app").projectDir = file("code/jvm/app")
project(":domain").projectDir = file("code/jvm/domain")
project(":http").projectDir = file("code/jvm/http")
project(":repository-jdbi").projectDir = file("code/jvm/repository-jdbi")
project(":services").projectDir = file("code/jvm/services")