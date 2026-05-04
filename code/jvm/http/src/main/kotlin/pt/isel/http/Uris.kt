package pt.isel.http

object Uris {
    const val PREFIX = "/api"
    const val HOME = PREFIX

    object Users {
        const val CREATE = "$PREFIX/users"
        const val LIST = "$PREFIX/users"
        const val LOGIN = "$PREFIX/users/login"
        const val ME = "$PREFIX/users/me"
        const val LOGOUT = "$PREFIX/users/logout"
        const val GET_BY_ID = "$PREFIX/users/{id}"
        const val DELETE_BY_ID = "$PREFIX/users/{id}"
    }

    object Cases {
        const val CREATE = "$PREFIX/cases"
        const val LIST = "$PREFIX/cases"
        const val GET_BY_ID = "$PREFIX/cases/{id}"
        const val UPDATE_BY_ID = "$PREFIX/cases/{id}"
        const val DELETE_BY_ID = "$PREFIX/cases/{id}"
        const val LIST_BY_USER_ID = "$PREFIX/users/{userId}/cases"
    }
}
