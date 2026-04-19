# Attendance Dashboard

React-based admin dashboard for the Facial Recognition Student Attendance System.

## Stack

- **React 19** + **TypeScript**
- **Vite** for build tooling
- **React Router v7** for navigation
- **Shadcn/ui** + **Tailwind CSS v4** for UI components
- **Tanstack Query** for data fetching
- **Recharts** for charts
- **Axios** for HTTP client

## Project Structure

```
dashboard/
├── src/
│   ├── api/                   # API client layer
│   │   ├── client.ts         # Axios instance with JWT interceptor
│   │   ├── auth.ts
│   │   ├── students.ts
│   │   ├── courses.ts
│   │   ├── attendance.ts
│   │   └── reports.ts
│   ├── components/
│   │   ├── layout/           # Layout components
│   │   ├── ui/               # Shadcn UI components
│   │   ├── AttendanceTable.tsx
│   │   ├── StudentForm.tsx
│   │   ├── CourseForm.tsx
│   │   └── AttendanceChart.tsx
│   ├── pages/                # Page components
│   ├── hooks/                # Custom hooks
│   ├── lib/                  # Utilities
│   └── types/                # TypeScript types
├── Dockerfile                # Multi-stage build
├── nginx.conf                # Production server config
└── package.json
```

## Features

### Pages

- **Dashboard**: Overview with stats cards and charts
- **Attendance**: Filterable attendance table with CSV export
- **Students**: CRUD operations for student management
- **Courses**: Course creation and management
- **Reports**: Analytics and attendance trends
- **Settings**: System configuration placeholder

### Authentication

- JWT-based authentication with automatic token refresh
- Protected routes with redirect to login
- User context with auth state management

### UI Components

Custom Shadcn/ui components:
- Button, Card, Input, Label
- Table, Badge
- Dialog, Select, Dropdown Menu

## Development

```bash
# Install dependencies
npm install

# Start dev server (with backend proxy)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Backend API

The dashboard connects to the FastAPI backend at `http://localhost:8000` via Vite proxy during development.

API endpoints:
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/me` - Current user info
- `GET /api/v1/students/` - List students
- `POST /api/v1/students/register` - Create student
- `GET /api/v1/courses/` - List courses
- `POST /api/v1/courses/` - Create course
- `GET /api/v1/attendance/` - Get attendance records
- `GET /api/v1/reports/summary` - Attendance summary

## Docker Deployment

```bash
# Build image
docker build -t attendance-dashboard .

# Run container
docker run -p 80:80 attendance-dashboard
```

The Dockerfile uses a multi-stage build:
1. **Build stage**: Node 22 for building the app
2. **Production stage**: Nginx Alpine for serving static files

## Environment

- Development server: `http://localhost:3000`
- Backend proxy: `/api` → `http://localhost:8000`
