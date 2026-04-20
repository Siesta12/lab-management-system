import type { DepartmentDto, OptionItem } from '../types';
import { get } from './http';

export function fetchDepartmentOptions(token?: string): Promise<OptionItem[]> {
  return get<OptionItem[]>('/departments/options', token);
}

export function fetchDepartmentById(id: number, token?: string): Promise<DepartmentDto> {
  return get<DepartmentDto>(`/departments/${id}`, token);
}
