package pt.isel.http.dto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH = "https://github.com/AfonsoSantos44/IDAP---Final-Project/docs/problems"

sealed class Problem(
    typeUri: URI,
) {
    @Suppress("unused")
    val type = typeUri.toString()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object EmailAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/email-already-exists"))

    data object UsernameAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/username-already-exists"))

    data object InvalidEmail : Problem(URI("$PROBLEM_URI_PATH/invalid-email"))
}
