export interface StudentResponse {
  id: string;
  student_number: string;
  full_name: string;
  email: string | null;
  created_at: string;
}

export interface CourseResponse {
  id: string;
  name: string;
  code: string;
  teacher_id: string | null;
  schedule: Record<string, unknown> | null;
}

export interface AttendanceResponse {
  id: string;
  student_id: string;
  course_id: string;
  checked_in_at: string;
  status: 'present' | 'late' | 'absent';
  confidence_score: number | null;
}

export interface AttendanceSummary {
  course_id: string;
  course_name: string;
  total_students: number;
  present: number;
  late: number;
  absent: number;
  attendance_rate: number;
}

export interface UserResponse {
  id: string;
  email: string;
  full_name: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
}

export interface StudentCreateRequest {
  student_number: string;
  full_name: string;
  email?: string;
  photo?: File;
}

export interface CourseCreateRequest {
  name: string;
  code: string;
  teacher_id?: string;
  schedule?: Record<string, unknown>;
}

export interface EnrollmentRequest {
  student_id: string;
  course_id: string;
}

export interface AttendanceRecord extends AttendanceResponse {
  student_name?: string;
  course_name?: string;
}
