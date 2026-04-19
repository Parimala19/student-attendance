import apiClient from './client';
import type { CourseResponse, CourseCreateRequest, EnrollmentRequest } from '@/types';

export const coursesApi = {
  list: async (): Promise<CourseResponse[]> => {
    const response = await apiClient.get<CourseResponse[]>('/courses/');
    return response.data;
  },

  get: async (id: string): Promise<CourseResponse> => {
    const response = await apiClient.get<CourseResponse>(`/courses/${id}`);
    return response.data;
  },

  create: async (data: CourseCreateRequest): Promise<CourseResponse> => {
    const response = await apiClient.post<CourseResponse>('/courses/', data);
    return response.data;
  },

  enroll: async (data: EnrollmentRequest): Promise<void> => {
    await apiClient.post('/courses/enroll', data);
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/courses/${id}`);
  },
};
