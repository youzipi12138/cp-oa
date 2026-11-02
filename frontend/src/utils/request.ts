import { message } from 'antd';
import axios, { type AxiosRequestConfig } from 'axios';
import type { Result } from '@/types';
import { clearAuth, getToken } from './auth';

const basePath = import.meta.env.BASE_URL;
const loginPath = `${basePath}login`;

const service = axios.create({
  baseURL: `${basePath}api`,
  timeout: 15000,
});

service.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  (response) => {
    const body = response.data as Result<unknown>;
    if (body.code === 0) {
      return body.data as never;
    }
    message.error(body.message || '请求失败');
    return Promise.reject(new Error(body.message || '请求失败'));
  },
  (error) => {
    const status: number | undefined = error.response?.status;
    const msg = (error.response?.data as Result<unknown> | undefined)?.message;

    if (status === 401) {
      clearAuth();
      if (window.location.pathname !== loginPath) {
        message.warning('登录已失效，请重新登录');
        window.location.href = loginPath;
      }
    } else {
      message.error(msg || error.message || '网络异常，请稍后重试');
    }
    return Promise.reject(error);
  },
);

/** 直接返回后端 Result.data，拦截器已解包 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>;
}

export default service;
