import type { UserInfo } from '@/types';
import { request } from '@/utils/request';

export interface DepartmentNode {
  id: number;
  name: string;
  feishuDepartmentId: string;
  memberCount: number;
  children: DepartmentNode[];
}

export interface OrgSyncResult {
  departments: number;
  departmentsRemoved: number;
  usersCreated: number;
  usersUpdated: number;
  usersDisabled: number;
  relations: number;
  durationMs: number;
}

export function getOrgTree() {
  return request<DepartmentNode[]>({
    url: '/org/tree',
    method: 'get',
  });
}

export function getDepartmentUsers(departmentId: number) {
  return request<UserInfo[]>({
    url: `/org/departments/${departmentId}/users`,
    method: 'get',
  });
}

export function syncOrg() {
  return request<OrgSyncResult>({
    url: '/org/sync',
    method: 'post',
  });
}
