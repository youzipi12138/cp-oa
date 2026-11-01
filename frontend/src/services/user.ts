import type { PageResult, UserFormValues, UserInfo } from '@/types';
import { request } from '@/utils/request';

export interface UserQuery {
  current: number;
  pageSize: number;
  keyword?: string;
}

export function pageUsers(params: UserQuery) {
  return request<PageResult<UserInfo>>({
    url: '/users',
    method: 'get',
    params,
  });
}

export function createUser(data: UserFormValues) {
  return request<UserInfo>({
    url: '/users',
    method: 'post',
    data,
  });
}

export function updateUser(id: number, data: Omit<UserFormValues, 'username'>) {
  return request<UserInfo>({
    url: `/users/${id}`,
    method: 'put',
    data,
  });
}

export function deleteUser(id: number) {
  return request<void>({
    url: `/users/${id}`,
    method: 'delete',
  });
}
