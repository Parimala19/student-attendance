import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { reportsApi } from '@/api/reports';
import { studentsApi } from '@/api/students';
import { attendanceApi } from '@/api/attendance';
import AttendanceChart from '@/components/AttendanceChart';
import { Users, CheckCircle, XCircle, TrendingUp } from 'lucide-react';
import { formatTime } from '@/lib/utils';

export default function DashboardPage() {
  const today = new Date().toISOString().split('T')[0];

  const { data: summary = [] } = useQuery({
    queryKey: ['summary', today],
    queryFn: () => reportsApi.getSummary(today),
  });

  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: studentsApi.list,
  });

  const { data: recentAttendance = [] } = useQuery({
    queryKey: ['attendance', 'recent'],
    queryFn: () => attendanceApi.getAll({ date: today }),
  });

  const totalStudents = students.length;
  const todayPresent = summary.reduce((sum, course) => sum + course.present, 0);
  const todayLate = summary.reduce((sum, course) => sum + course.late, 0);
  const todayAbsent = summary.reduce((sum, course) => sum + course.absent, 0);
  const todayTotal = todayPresent + todayLate + todayAbsent;
  const attendanceRate = todayTotal > 0 ? ((todayPresent + todayLate) / todayTotal) * 100 : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">Overview of today's attendance</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Students</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalStudents}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Attendance Rate</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{attendanceRate.toFixed(1)}%</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Present Today</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{todayPresent + todayLate}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Absent Today</CardTitle>
            <XCircle className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{todayAbsent}</div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Attendance by Course</CardTitle>
        </CardHeader>
        <CardContent>
          <AttendanceChart data={summary} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Recent Check-ins</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {recentAttendance.slice(0, 10).map((record) => (
              <div key={record.id} className="flex items-center justify-between border-b pb-2">
                <div>
                  <p className="font-medium">Student ID: {record.student_id.substring(0, 8)}...</p>
                  <p className="text-sm text-muted-foreground">
                    Course ID: {record.course_id.substring(0, 8)}...
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-sm">{formatTime(record.checked_in_at)}</p>
                  <p className="text-xs text-muted-foreground capitalize">{record.status}</p>
                </div>
              </div>
            ))}
            {recentAttendance.length === 0 && (
              <p className="text-center text-muted-foreground py-4">No check-ins today</p>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
