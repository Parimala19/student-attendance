import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { CourseCreateRequest } from '@/types';

interface CourseFormProps {
  onSubmit: (data: CourseCreateRequest) => void;
  loading?: boolean;
}

export default function CourseForm({ onSubmit, loading }: CourseFormProps) {
  const [name, setName] = useState('');
  const [code, setCode] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ name, code });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="course_code">Course Code</Label>
        <Input
          id="course_code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="CS101"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="course_name">Course Name</Label>
        <Input
          id="course_name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Introduction to Computer Science"
          required
        />
      </div>
      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? 'Creating...' : 'Create Course'}
      </Button>
    </form>
  );
}
