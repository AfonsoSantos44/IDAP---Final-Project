
// User Types

export interface User {
  id?: number;
  userId?: number;
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
  userId: number;
  username: string;
  email: string;
}

export interface UserMe {
    userId: number;
    id?: number;
    name: string;
    username?: string;
    email: string;
}
