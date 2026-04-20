import type { ReservationAuditLogDto } from '../types';
import { get } from './http';

export function fetchReservationAuditLogs(id: number, token: string): Promise<ReservationAuditLogDto[]> {
  return get<ReservationAuditLogDto[]>(`/reservations/${id}/audit-logs`, token);
}
