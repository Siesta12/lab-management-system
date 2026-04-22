import { post } from './http';

export interface CheckinSubmitPayload {
  labIdentifier: string;
  latitude?: number | null;
  longitude?: number | null;
  accuracy?: number | null;
  capturedAt?: string;
  userAgent?: string;
}

export function submitCheckin(payload: CheckinSubmitPayload): Promise<void> {
  return post<void>('/checkin/submit', payload);
}
