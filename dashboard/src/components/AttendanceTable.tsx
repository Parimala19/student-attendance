import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { formatDateTime } from '@/lib/utils';
import type { AttendanceRecord } from '@/types';

interface AttendanceTableProps {
  data: AttendanceRecord[];
}

export default function AttendanceTable({ data }: AttendanceTableProps) {
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'present':
        return <Badge variant="success">Present</Badge>;
      case 'late':
        return <Badge variant="warning">Late</Badge>;
      case 'absent':
        return <Badge variant="destructive">Absent</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Student Name</TableHead>
          <TableHead>Course</TableHead>
          <TableHead>Check-in Time</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Confidence</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.length === 0 ? (
          <TableRow>
            <TableCell colSpan={5} className="text-center text-muted-foreground">
              No attendance records found
            </TableCell>
          </TableRow>
        ) : (
          data.map((record) => (
            <TableRow key={record.id}>
              <TableCell>{record.student_name || 'Unknown'}</TableCell>
              <TableCell>{record.course_name || 'Unknown'}</TableCell>
              <TableCell>{formatDateTime(record.checked_in_at)}</TableCell>
              <TableCell>{getStatusBadge(record.status)}</TableCell>
              <TableCell>
                {record.confidence_score
                  ? `${(record.confidence_score * 100).toFixed(1)}%`
                  : 'N/A'}
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  );
}
