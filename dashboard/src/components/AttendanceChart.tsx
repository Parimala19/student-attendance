import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import type { AttendanceSummary } from '@/types';

interface AttendanceChartProps {
  data: AttendanceSummary[];
}

export default function AttendanceChart({ data }: AttendanceChartProps) {
  const chartData = data.map((item) => ({
    name: item.course_name,
    Present: item.present,
    Late: item.late,
    Absent: item.absent,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey="Present" fill="#22c55e" />
        <Bar dataKey="Late" fill="#eab308" />
        <Bar dataKey="Absent" fill="#ef4444" />
      </BarChart>
    </ResponsiveContainer>
  );
}
