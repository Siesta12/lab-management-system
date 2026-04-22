import type {
  PageData,
  ReservationApplyResponse,
  ReservationConflictSlotDto,
  ReservationCreatePayload,
  ReservationDto,
  SlotRecommendationItem,
} from '../types';
import { get, post, put } from './http';

export interface ReservationQuery {
  pageNum?: number;
  pageSize?: number;
  status?: number;
  conflictOnly?: boolean;
}

export function fetchReservations(query: ReservationQuery = {}, token?: string): Promise<PageData<ReservationDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.status !== undefined && query.status !== null) {
    params.set('status', String(query.status));
  }
  if (query.conflictOnly) {
    params.set('conflictOnly', 'true');
  }
  return get<PageData<ReservationDto>>(`/reservations?${params.toString()}`, token);
}

export function fetchConflictReservations(
  query: ReservationQuery = {},
  token?: string,
): Promise<PageData<ReservationConflictSlotDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.status !== undefined && query.status !== null) {
    params.set('status', String(query.status));
  }
  return get<PageData<ReservationConflictSlotDto>>(`/reservations/conflicts?${params.toString()}`, token);
}

export function fetchMyReservations(query: ReservationQuery = {}, token: string): Promise<PageData<ReservationDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  return get<PageData<ReservationDto>>(`/reservations/my?${params.toString()}`, token);
}

export function createReservation(payload: ReservationCreatePayload, token: string): Promise<ReservationDto> {
  return post<ReservationDto>('/reservations', payload, token);
}

export function applyReservation(payload: ReservationCreatePayload, token: string): Promise<ReservationApplyResponse> {
  return post<ReservationApplyResponse>('/reservations/apply', payload, token);
}

export function fetchReservationById(id: number, token: string): Promise<ReservationDto> {
  return get<ReservationDto>(`/reservations/${id}`, token);
}

export function cancelReservation(id: number, token: string): Promise<ReservationDto> {
  return put<ReservationDto>(`/reservations/${id}/cancel`, undefined, token);
}

export function approveReservation(id: number, token: string, auditComment?: string): Promise<ReservationDto> {
  return put<ReservationDto>(`/reservations/${id}/approve`, auditComment ? { auditComment } : undefined, token);
}

export function rejectReservation(id: number, token: string, rejectReason: string, auditComment?: string): Promise<ReservationDto> {
  return put<ReservationDto>(`/reservations/${id}/reject`, { rejectReason, auditComment }, token);
}

export function recommendSlots(
  payload: Pick<ReservationCreatePayload, 'labId' | 'participantCount' | 'slots'>,
  token: string,
): Promise<SlotRecommendationItem[]> {
  return post<SlotRecommendationItem[]>('/reservations/recommendations', payload, token);
}
