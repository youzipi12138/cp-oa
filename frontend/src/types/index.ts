export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  email?: string;
  phone?: string;
  employeeNo?: string;
  role: 'admin' | 'user';
  status: number;
  createTime?: string;
}

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  user: UserInfo;
}

export interface UserFormValues {
  username?: string;
  password?: string;
  realName: string;
  email?: string;
  phone?: string;
  role: string;
  status: number;
}
