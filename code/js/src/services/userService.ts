import { LoginInputModel , TokenOutputModel, UserInput, UserMe, User } from "../types/userTypes"
import { fetchApi, ApiError,API_BASE_URL } from "./api";

 export const userService = {

  async register(input: UserInput): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/users`, {
      method: "POST",
      credentials: 'include',
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
  if (response.status !== 201) {
        const error = await response
          .json()
          .catch(() => ({ title: "Unknown error" }));
        const errorMessage = error.title || response.statusText;
        throw new ApiError(response.status, errorMessage);
      }
      return response.headers.get("Location") || "";
  },

    async login(
        input: LoginInputModel
    ): Promise<TokenOutputModel> {
     return fetchApi<TokenOutputModel>("/users/login", {
        method: "POST",
         body: JSON.stringify(input),
        });
    },

  async logout(): Promise<void> {
    return fetchApi<void>("/users/logout", {
      method: "POST",
    });
  },

  async getMe(): Promise<UserMe> {
    return fetchApi<UserMe>("/users/me");
  },

  async getUserById(id: number): Promise<any> {
    return fetchApi<any>(`/users/${id}`);
  },

  async getAllUsers(): Promise<User[]> {
    return fetchApi<User[]>('/users', { method: 'GET' });
  },

}