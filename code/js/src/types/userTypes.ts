
// User Types

export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface UserInput {
  username: string;
  email: string;
  password: string;
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
