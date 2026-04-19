import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import AttendanceTable from '@/components/AttendanceTable';
import { attendanceApi } from '@/api/attendance';
import { coursesApi } from '@/api/courses';
import { studentsApi } from '@/api/students';
import { downloadCSV } from '@/lib/utils';
import { Download } from 'lucide-react';
import type { AttendanceRecord } from '@/types';

export default function AttendancePage() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const { data: attendance = [] } = useQuery({
    queryKey: ['attendance', selectedDate, selectedCourse],
    queryFn: () => attendanceApi.getAll({ date: selectedDate, course_id: selectedCourse || undefined }),
  });

  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: coursesApi.list,
  });

  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: studentsApi.list,
  });

  const enrichedAttendance: AttendanceRecord[] = attendance.map((record) => {
    const student = students.find((s) => s.id === record.student_id);
    const course = courses.find((c) => c.id === record.course_id);
    return {
      ...record,
      student_name: student?.full_name,
      course_name: course?.name,
    };
  });

  const filteredAttendance = enrichedAttendance.filter((record) =>
    record.student_name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleExportCSV = () => {
    const headers = ['Student Name', 'Course', 'Check-in Time', 'Status', 'Confidence'];
    const rows = filteredAttendance.map((record) => [
      record.student_name || 'Unknown',
      record.course_name || 'Unknown',
      record.checked_in_at,
      record.status,
      record.confidence_score ? `${(record.confidence_score * 100).toFixed(1)}%` : 'N/A',
    ]);

    const csv = [headers, ...rows].map((row) => row.join(',')).join('\n');
    downloadCSV(csv, `attendance-${selectedDate}.csv`);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Attendance Records</h1>
        <p className="text-muted-foreground">View and filter attendance data</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div className="flex-1 min-w-[200px]">
              <Input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
              />
            </div>
            <div className="flex-1 min-w-[200px]">
              <Select value={selectedCourse} onValueChange={setSelectedCourse}>
                <SelectTrigger>
                  <SelectValue placeholder="All Courses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All Courses</SelectItem>
                  {courses.map((course) => (
                    <SelectItem key={course.id} value={course.id}>
                      {course.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex-1 min-w-[200px]">
              <Input
                placeholder="Search student..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <Button onClick={handleExportCSV} className="gap-2">
              <Download className="h-4 w-4" />
              Export CSV
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="pt-6">
          <AttendanceTable data={filteredAttendance} />
        </CardContent>
      </Card>
    </div>
  );
}
