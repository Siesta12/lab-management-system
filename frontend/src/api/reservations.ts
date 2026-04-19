import type {
  ConflictCheckData,
  LabRecommendationDto,
  PageData,
  ReservationCreatePayload,
  ReservationDto,
  TimeRecommendationDto,
} from '../types';
import { get, post } from './http';

export interface ReservationQuery {
  pageNum?: number;
  pageSize?: number;
}

export function fetchReservations(query: ReservationQuery = {}, token?: string): Promise<PageData<ReservationDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  return get<PageData<ReservationDto>>(`/reservations?${params.toString()}`, token);
}

export function fetchMyReservations(query: ReservationQuery = {}, token: string): Promise<PageData<ReservationDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  return get<PageData<ReservationDto>>(`/reservations/mine?${params.toString()}`, token);
}

export function createReservation(payload: ReservationCreatePayload, token: string): Promise<ReservationDto> {
  return post<ReservationDto>('/reservations', payload, token);
}

export function checkReservationConflict(
  payload: Pick<ReservationCreatePayload, 'labId' | 'startTime' | 'endTime'>,
  token?: string,
): Promise<ConflictCheckData> {
  return post<ConflictCheckData>('/reservations/conflict-check', payload, token);
}

export function recommendTimes(
  payload: Pick<ReservationCreatePayload, 'labId' | 'reservationDate' | 'startTime' | 'endTime' | 'participantCount'>,
  token?: string,
): Promise<TimeRecommendationDto[]> {
  return post<TimeRecommendationDto[]>('/reservations/recommend-time', payload, token);
}

export function recommendLabs(
  payload: Pick<ReservationCreatePayload, 'labId' | 'reservationDate' | 'startTime' | 'endTime' | 'participantCount'>,
  token?: string,
): Promise<LabRecommendationDto[]> {
  return post<LabRecommendationDto[]>('/reservations/recommend-labs', payload, token);
}
