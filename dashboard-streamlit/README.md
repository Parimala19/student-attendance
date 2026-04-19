# Attendance Dashboard - Streamlit

Simple Python-based admin dashboard for the Facial Recognition Student Attendance System.

## Features

- **Single File App**: All functionality in one `app.py` file (~450 lines)
- **Authentication**: JWT-based login with session management
- **Dashboard Pages**:
  - Overview: Stats cards, charts, recent check-ins
  - Attendance: Filterable records with CSV export
  - Students: List and create students
  - Courses: List and create courses
  - Reports: Analytics with interactive charts

## Stack

- **Streamlit** - Python web framework
- **Plotly** - Interactive charts
- **Pandas** - Data manipulation
- **Requests** - API client

## Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run locally (update API_BASE_URL to http://localhost:8000)
streamlit run app.py

# Access at http://localhost:8501
```

## Docker Deployment

```bash
# Build image
docker build -t attendance-dashboard .

# Run container
docker run -p 8501:8501 attendance-dashboard
```

## Environment

The app connects to the FastAPI backend at `http://backend:8000` (configured for Docker Compose).

For local development, update `API_BASE_URL` in `app.py`:
```python
API_BASE_URL = "http://localhost:8000/api/v1"
```

## Comparison to React Dashboard

| Metric | React/TypeScript | Streamlit |
|--------|------------------|-----------|
| Files | 77 | 1 |
| Lines of Code | ~3000+ | ~450 |
| Languages | TypeScript, JSX, CSS | Python only |
| Build Time | 2-3 min | 10 sec |
| Learning Curve | High | Low |
| UI Polish | Professional | Clean & functional |

Perfect for demos, prototypes, and internal tools!
