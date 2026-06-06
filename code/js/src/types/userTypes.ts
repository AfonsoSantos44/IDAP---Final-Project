
// User Types

export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface UserInput {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface LoginInputModel {
  email: string;
  password: string;
}
export interface TokenOutputModel {
  token: string;
}

export interface UserMe {
    id: number;
    name: string;
    email: string;
}
