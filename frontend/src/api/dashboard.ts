import type { AdminDashboardDto } from '../types';
import { get } from './http';

export function fetchAdminDashboard(token: string): Promise<AdminDashboardDto> {
  return get<AdminDashboardDto>('/dashboard/admin-overview', token);
}
