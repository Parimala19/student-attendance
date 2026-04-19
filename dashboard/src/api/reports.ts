import apiClient from './client';
import type { AttendanceSummary } from '@/types';

export const reportsApi = {
  getSummary: async (date?: string): Promise<AttendanceSummary[]> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<AttendanceSummary[]>('/reports/summary', { params });
    return response.data;
  },
};
