import type { OptionItem } from '../types';
import { get } from './http';

export function fetchRoleOptions(token?: string): Promise<OptionItem[]> {
  return get<OptionItem[]>('/roles/options', token);
}
