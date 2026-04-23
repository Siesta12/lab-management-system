import type { CheckinResultDto, CheckinSubmitPayload } from '../types';
import { post } from './http';

export function submitCheckin(
  payload: CheckinSubmitPayload,
  token: string,
): Promise<CheckinResultDto> {
  return post<CheckinResultDto>('/checkin/submit', payload, token);
}
