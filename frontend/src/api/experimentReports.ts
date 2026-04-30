import type {
  ExperimentReportDto,
  ExperimentReportReviewPayload,
  ExperimentReportSavePayload,
  PageData,
} from '../types';
import { ApiError, get, getApiBaseUrl, post, put } from './http';

export interface ExperimentReportQuery {
  pageNum?: number;
  pageSize?: number;
  status?: number;
  keyword?: string;
}

function buildQuery(query: ExperimentReportQuery): string {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.status !== undefined && query.status !== null) {
    params.set('status', String(query.status));
  }
  if (query.keyword) {
    params.set('keyword', query.keyword);
  }
  return params.toString();
}

export function fetchExperimentReports(
  query: ExperimentReportQuery = {},
  token: string,
): Promise<PageData<ExperimentReportDto>> {
  return get<PageData<ExperimentReportDto>>(`/experiment-reports?${buildQuery(query)}`, token);
}

export function fetchExperimentReportById(id: number, token: string): Promise<ExperimentReportDto> {
  return get<ExperimentReportDto>(`/experiment-reports/${id}`, token);
}

export function createExperimentReportDraft(
  payload: ExperimentReportSavePayload,
  token: string,
): Promise<ExperimentReportDto> {
  return post<ExperimentReportDto>('/experiment-reports/draft', payload, token);
}

export function updateExperimentReportDraft(
  id: number,
  payload: ExperimentReportSavePayload,
  token: string,
): Promise<ExperimentReportDto> {
  return put<ExperimentReportDto>(`/experiment-reports/${id}/draft`, payload, token);
}

export function submitExperimentReport(id: number, token: string): Promise<ExperimentReportDto> {
  return post<ExperimentReportDto>(`/experiment-reports/${id}/submit`, undefined, token);
}

export function reviewExperimentReport(
  id: number,
  payload: ExperimentReportReviewPayload,
  token: string,
): Promise<ExperimentReportDto> {
  return post<ExperimentReportDto>(`/experiment-reports/${id}/review`, payload, token);
}

export async function downloadExperimentReportWord(id: number, token: string): Promise<Blob> {
  const response = await fetch(`${getApiBaseUrl()}/experiment-reports/${id}/word`, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(response.status, payload?.message ?? `请求失败：${response.status}`);
  }
  return response.blob();
}
