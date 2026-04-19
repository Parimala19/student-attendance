import apiClient from './client';
import type { StudentResponse, StudentCreateRequest } from '@/types';

export const studentsApi = {
  list: async (): Promise<StudentResponse[]> => {
    const response = await apiClient.get<StudentResponse[]>('/students/');
    return response.data;
  },

  get: async (id: string): Promise<StudentResponse> => {
    const response = await apiClient.get<StudentResponse>(`/students/${id}`);
    return response.data;
  },

  register: async (data: StudentCreateRequest): Promise<StudentResponse> => {
    const formData = new FormData();
    formData.append('student_number', data.student_number);
    formData.append('full_name', data.full_name);
    if (data.email) {
      formData.append('email', data.email);
    }
    if (data.photo) {
      formData.append('photo', data.photo);
    }

    const response = await apiClient.post<StudentResponse>('/students/register', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/students/${id}`);
  },
};
