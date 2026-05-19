# IDAP---Final-Project

## Authentication

The backend uses cookie-based session authentication. `POST /api/users/login`
returns a secure random session token in the `idap_session` HttpOnly cookie; the
database stores only the token hash. Protected API routes require that cookie.

Regular users can access their own profile and cases. System administrators can
list/delete users and list/access cases globally. The API does not use JWT or
Bearer authentication.