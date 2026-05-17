package pt.isel.http.dto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH = "https://github.com/AfonsoSantos44/IDAP---Final-Project/docs/problems"

sealed class Problem(
    typeUri: URI,
    val title: String,
) {
    @Suppress("unused")
    val type = typeUri.toString()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object InvalidUsername : Problem(
        URI("$PROBLEM_URI_PATH/invalid-username"),
        "Invalid username",
    )

    data object InsecurePassword : Problem(
        URI("$PROBLEM_URI_PATH/insecure-password"),
        "Insecure password",
    )

    data object EmailAlreadyExists : Problem(
        URI("$PROBLEM_URI_PATH/email-already-exists"),
        "Email already exists",
    )

    data object UsernameAlreadyExists : Problem(
        URI("$PROBLEM_URI_PATH/username-already-exists"),
        "Username already exists",
    )

    data object InvalidEmail : Problem(
        URI("$PROBLEM_URI_PATH/invalid-email"),
        "Invalid email",
    )

    data object UserNotFound : Problem(
        URI("$PROBLEM_URI_PATH/user-not-found"),
        "User not found",
    )

    data object InvalidCredentials : Problem(
        URI("$PROBLEM_URI_PATH/invalid-credentials"),
        "Invalid credentials",
    )

    data object InvalidToken : Problem(
        URI("$PROBLEM_URI_PATH/invalid-token"),
        "Invalid token",
    )

    data object ExpiredToken : Problem(
        URI("$PROBLEM_URI_PATH/expired-token"),
        "Expired token",
    )

    data object NoUserLoggedIn : Problem(
        URI("$PROBLEM_URI_PATH/no-user-logged-in"),
        "No user currently logged in",
    )

    data object CaseNotFound : Problem(
        URI("$PROBLEM_URI_PATH/case-not-found"),
        "Case not found",
    )

    data object CaseAccessDenied : Problem(
        URI("$PROBLEM_URI_PATH/case-access-denied"),
        "Case access denied",
    )

    data object InvalidCaseStatus : Problem(
        URI("$PROBLEM_URI_PATH/invalid-case-status"),
        "Invalid case status",
    )

    data object InvalidCaseDescription : Problem(
        URI("$PROBLEM_URI_PATH/invalid-case-description"),
        "Invalid case description",
    )
}
