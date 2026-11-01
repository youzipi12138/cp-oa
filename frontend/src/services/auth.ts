import type { LoginParams, LoginResult, UserInfo } from '@/types';
import { request } from '@/utils/request';

export function login(data: LoginParams) {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data,
  });
}

export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'post',
  });
}

export function getCurrentUser() {
  return request<UserInfo>({
    url: '/auth/me',
    method: 'get',
  });
}
