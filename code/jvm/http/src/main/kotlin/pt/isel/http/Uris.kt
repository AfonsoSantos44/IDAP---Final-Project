package pt.isel.http

object Uris {
    const val PREFIX = "/api"
    const val HOME = PREFIX

    object Users {
        const val CREATE = "$PREFIX/users"
        const val TOKEN = "$PREFIX/users/token"
        const val LOGOUT = "$PREFIX/logout"
        const val GET_BY_ID = "$PREFIX/users/{id}"
    }
}
