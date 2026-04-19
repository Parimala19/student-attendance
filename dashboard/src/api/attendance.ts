import apiClient from './client';
import type { AttendanceResponse } from '@/types';

export const attendanceApi = {
  getByCourse: async (courseId: string, date?: string): Promise<AttendanceResponse[]> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<AttendanceResponse[]>(
      `/attendance/course/${courseId}`,
      { params }
    );
    return response.data;
  },

  getAll: async (params?: { date?: string; course_id?: string; student_id?: string }): Promise<AttendanceResponse[]> => {
    const response = await apiClient.get<AttendanceResponse[]>('/attendance/', { params });
    return response.data;
  },
};
