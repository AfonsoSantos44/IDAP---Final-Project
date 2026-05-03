package pt.isel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdapApp

fun main(args: Array<String>) {
    runApplication<IdapApp>(*args)
}
