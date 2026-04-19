import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import AttendanceChart from '@/components/AttendanceChart';
import { reportsApi } from '@/api/reports';

export default function ReportsPage() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  const { data: summary = [] } = useQuery({
    queryKey: ['summary', selectedDate],
    queryFn: () => reportsApi.getSummary(selectedDate),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Reports</h1>
        <p className="text-muted-foreground">View attendance analytics and trends</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Select Date</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="max-w-sm">
            <Label htmlFor="date">Date</Label>
            <Input
              id="date"
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

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
          <CardTitle>Summary Table</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Course</TableHead>
                <TableHead>Total Students</TableHead>
                <TableHead>Present</TableHead>
                <TableHead>Late</TableHead>
                <TableHead>Absent</TableHead>
                <TableHead>Attendance Rate</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {summary.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    No data available for selected date
                  </TableCell>
                </TableRow>
              ) : (
                summary.map((item) => (
                  <TableRow key={item.course_id}>
                    <TableCell>{item.course_name}</TableCell>
                    <TableCell>{item.total_students}</TableCell>
                    <TableCell>{item.present}</TableCell>
                    <TableCell>{item.late}</TableCell>
                    <TableCell>{item.absent}</TableCell>
                    <TableCell>{(item.attendance_rate * 100).toFixed(1)}%</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
