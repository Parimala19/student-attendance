import streamlit as st
import requests
from datetime import datetime, date
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

# Configuration
API_BASE_URL = "http://backend:8000/api/v1"

# Page config
st.set_page_config(
    page_title="Attendance Dashboard",
    page_icon="📊",
    layout="wide",
    initial_sidebar_state="expanded",
)


# Authentication functions
def login(email: str, password: str) -> dict | None:
    try:
        response = requests.post(
            f"{API_BASE_URL}/auth/login",
            json={"email": email, "password": password},
            headers={"Content-Type": "application/json"},
        )
        if response.status_code == 200:
            return response.json()
        return None
    except Exception as e:
        st.error(f"Login failed: {e}")
        return None


def get_current_user(token: str) -> dict | None:
    try:
        response = requests.get(
            f"{API_BASE_URL}/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        if response.status_code == 200:
            return response.json()
        return None
    except Exception:
        return None


# API functions
def api_get(endpoint: str, token: str, params: dict | None = None) -> dict | list | None:
    try:
        response = requests.get(
            f"{API_BASE_URL}{endpoint}",
            headers={"Authorization": f"Bearer {token}"},
            params=params,
        )
        if response.status_code == 200:
            return response.json()
        return None
    except Exception as e:
        st.error(f"API Error: {e}")
        return None


def api_post(endpoint: str, token: str, data: dict) -> dict | None:
    try:
        response = requests.post(
            f"{API_BASE_URL}{endpoint}",
            headers={"Authorization": f"Bearer {token}"},
            json=data,
        )
        if response.status_code in (200, 201):
            return response.json()
        return None
    except Exception as e:
        st.error(f"API Error: {e}")
        return None


def api_delete(endpoint: str, token: str) -> bool:
    try:
        response = requests.delete(
            f"{API_BASE_URL}{endpoint}",
            headers={"Authorization": f"Bearer {token}"},
        )
        return response.status_code == 204
    except Exception as e:
        st.error(f"API Error: {e}")
        return False


# Initialize session state
if "authenticated" not in st.session_state:
    st.session_state.authenticated = False
if "token" not in st.session_state:
    st.session_state.token = None
if "user" not in st.session_state:
    st.session_state.user = None


# Login page
def show_login():
    st.title("🔐 Attendance Dashboard Login")

    col1, col2, col3 = st.columns([1, 2, 1])

    with col2:
        st.markdown("### Sign in to continue")
        email = st.text_input("Email", placeholder="admin@example.com")
        password = st.text_input("Password", type="password")

        if st.button("Sign In", type="primary", use_container_width=True):
            if email and password:
                result = login(email, password)
                if result:
                    st.session_state.token = result["access_token"]
                    user = get_current_user(result["access_token"])
                    if user:
                        st.session_state.user = user
                        st.session_state.authenticated = True
                        st.rerun()
                    else:
                        st.error("Failed to fetch user info")
                else:
                    st.error("Invalid email or password")
            else:
                st.warning("Please enter both email and password")


# Dashboard pages
def show_overview():
    st.title("📊 Dashboard Overview")

    token = st.session_state.token
    today = date.today().isoformat()

    # Fetch data
    students = api_get("/students/", token) or []
    summary = api_get("/reports/summary", token, {"date": today}) or []
    attendance = api_get("/attendance/", token, {"date": today}) or []

    # Calculate metrics
    total_students = len(students)
    total_present = sum(s["present"] for s in summary)
    total_late = sum(s["late"] for s in summary)
    total_absent = sum(s["absent"] for s in summary)
    total_checked = total_present + total_late
    total_enrolled = total_present + total_late + total_absent
    attendance_rate = (total_checked / total_enrolled * 100) if total_enrolled > 0 else 0

    # Display metrics
    col1, col2, col3, col4 = st.columns(4)

    with col1:
        st.metric("Total Students", total_students)

    with col2:
        st.metric("Attendance Rate", f"{attendance_rate:.1f}%")

    with col3:
        st.metric("Present Today", total_checked, delta=None)

    with col4:
        st.metric("Absent Today", total_absent, delta=None)

    # Charts
    col1, col2 = st.columns(2)

    with col1:
        st.subheader("Attendance by Course")
        if summary:
            df = pd.DataFrame(summary)
            fig = go.Figure(data=[
                go.Bar(name="Present", x=df["course_name"], y=df["present"], marker_color="green"),
                go.Bar(name="Late", x=df["course_name"], y=df["late"], marker_color="orange"),
                go.Bar(name="Absent", x=df["course_name"], y=df["absent"], marker_color="red"),
            ])
            fig.update_layout(barmode="group", height=400)
            st.plotly_chart(fig, use_container_width=True)
        else:
            st.info("No attendance data for today")

    with col2:
        st.subheader("Recent Check-ins")
        if attendance:
            recent = attendance[:10]
            for record in recent:
                col_a, col_b = st.columns([3, 1])
                with col_a:
                    st.text(f"Student: {record['student_id'][:8]}... | Course: {record['course_id'][:8]}...")
                with col_b:
                    status_color = {"present": "🟢", "late": "🟡", "absent": "🔴"}
                    st.text(f"{status_color.get(record['status'], '⚪')} {record['status'].title()}")
        else:
            st.info("No check-ins today")


def show_attendance():
    st.title("📋 Attendance Records")

    token = st.session_state.token

    # Filters
    col1, col2, col3 = st.columns(3)

    with col1:
        selected_date = st.date_input("Date", value=date.today())

    with col2:
        courses = api_get("/courses/", token) or []
        course_options = ["All Courses"] + [c["name"] for c in courses]
        selected_course = st.selectbox("Course", course_options)

    with col3:
        search_query = st.text_input("Search Student", placeholder="Student name or ID...")

    # Fetch attendance
    params = {"date": selected_date.isoformat()}
    if selected_course != "All Courses":
        course = next((c for c in courses if c["name"] == selected_course), None)
        if course:
            params["course_id"] = course["id"]

    attendance = api_get("/attendance/", token, params) or []
    students_map = {s["id"]: s for s in (api_get("/students/", token) or [])}
    courses_map = {c["id"]: c for c in courses}

    # Enrich data
    for record in attendance:
        record["student_name"] = students_map.get(record["student_id"], {}).get("full_name", "Unknown")
        record["course_name"] = courses_map.get(record["course_id"], {}).get("name", "Unknown")

    # Filter by search
    if search_query:
        attendance = [
            r for r in attendance
            if search_query.lower() in r["student_name"].lower()
            or search_query.lower() in r["student_id"].lower()
        ]

    # Display table
    st.subheader(f"Found {len(attendance)} records")

    if attendance:
        df = pd.DataFrame(attendance)
        display_df = df[[
            "student_name",
            "course_name",
            "checked_in_at",
            "status",
            "confidence_score"
        ]].copy()
        display_df.columns = ["Student", "Course", "Check-in Time", "Status", "Confidence"]
        display_df["Confidence"] = display_df["Confidence"].apply(
            lambda x: f"{x*100:.1f}%" if x else "N/A"
        )
        st.dataframe(display_df, use_container_width=True, hide_index=True)

        # Export CSV
        if st.button("📥 Export to CSV"):
            csv = display_df.to_csv(index=False)
            st.download_button(
                "Download CSV",
                csv,
                f"attendance_{selected_date}.csv",
                "text/csv",
                key="download-csv"
            )
    else:
        st.info("No attendance records found")


def show_students():
    st.title("👥 Students")

    token = st.session_state.token

    # Add student form
    with st.expander("➕ Add New Student", expanded=False):
        with st.form("add_student"):
            student_number = st.text_input("Student Number", placeholder="STU001")
            full_name = st.text_input("Full Name", placeholder="John Doe")
            email = st.text_input("Email (Optional)", placeholder="john@example.com")
            password = st.text_input("Password", type="password")

            if st.form_submit_button("Create Student", type="primary"):
                if student_number and full_name and password:
                    data = {
                        "student_number": student_number,
                        "full_name": full_name,
                        "password": password,
                    }
                    if email:
                        data["email"] = email

                    result = api_post("/students/register", token, data)
                    if result:
                        st.success(f"Student {full_name} created successfully!")
                        st.rerun()
                    else:
                        st.error("Failed to create student")
                else:
                    st.warning("Please fill in all required fields")

    # List students
    st.subheader("Student List")
    students = api_get("/students/", token) or []

    if students:
        df = pd.DataFrame(students)
        display_df = df[["student_number", "full_name", "email", "created_at"]].copy()
        display_df.columns = ["Student Number", "Full Name", "Email", "Created At"]
        display_df["Email"] = display_df["Email"].fillna("N/A")

        st.dataframe(display_df, use_container_width=True, hide_index=True)
        st.caption(f"Total: {len(students)} students")
    else:
        st.info("No students found")


def show_courses():
    st.title("📚 Courses")

    token = st.session_state.token

    # Add course form
    with st.expander("➕ Create New Course", expanded=False):
        with st.form("add_course"):
            course_code = st.text_input("Course Code", placeholder="CS101")
            course_name = st.text_input("Course Name", placeholder="Introduction to Computer Science")

            if st.form_submit_button("Create Course", type="primary"):
                if course_code and course_name:
                    data = {"code": course_code, "name": course_name}
                    result = api_post("/courses/", token, data)
                    if result:
                        st.success(f"Course {course_code} created successfully!")
                        st.rerun()
                    else:
                        st.error("Failed to create course")
                else:
                    st.warning("Please fill in all fields")

    # List courses
    st.subheader("Course List")
    courses = api_get("/courses/", token) or []

    if courses:
        cols = st.columns(3)
        for idx, course in enumerate(courses):
            with cols[idx % 3]:
                with st.container(border=True):
                    st.markdown(f"**{course['name']}**")
                    st.text(f"Code: {course['code']}")
                    if course.get("teacher_id"):
                        st.text(f"Teacher: {course['teacher_id'][:8]}...")
    else:
        st.info("No courses found")


def show_reports():
    st.title("📈 Reports")

    token = st.session_state.token

    # Date selector
    selected_date = st.date_input("Select Date", value=date.today())

    # Fetch summary
    summary = api_get("/reports/summary", token, {"date": selected_date.isoformat()}) or []

    if summary:
        # Summary table
        st.subheader("Attendance Summary")
        df = pd.DataFrame(summary)
        display_df = df[[
            "course_name",
            "total_students",
            "present",
            "late",
            "absent",
            "attendance_rate"
        ]].copy()
        display_df.columns = ["Course", "Total", "Present", "Late", "Absent", "Rate"]
        display_df["Rate"] = display_df["Rate"].apply(lambda x: f"{x*100:.1f}%")

        st.dataframe(display_df, use_container_width=True, hide_index=True)

        # Charts
        col1, col2 = st.columns(2)

        with col1:
            st.subheader("Attendance Distribution")
            fig = go.Figure(data=[
                go.Bar(name="Present", x=df["course_name"], y=df["present"], marker_color="green"),
                go.Bar(name="Late", x=df["course_name"], y=df["late"], marker_color="orange"),
                go.Bar(name="Absent", x=df["course_name"], y=df["absent"], marker_color="red"),
            ])
            fig.update_layout(barmode="stack", height=400)
            st.plotly_chart(fig, use_container_width=True)

        with col2:
            st.subheader("Attendance Rate by Course")
            fig = px.bar(
                df,
                x="course_name",
                y="attendance_rate",
                labels={"course_name": "Course", "attendance_rate": "Attendance Rate"},
                color="attendance_rate",
                color_continuous_scale="RdYlGn",
            )
            fig.update_layout(height=400, showlegend=False)
            st.plotly_chart(fig, use_container_width=True)
    else:
        st.info("No data available for selected date")


# Main app
def main():
    if not st.session_state.authenticated:
        show_login()
    else:
        # Sidebar
        with st.sidebar:
            st.title("🎓 Attendance App")
            st.markdown("---")

            if st.session_state.user:
                st.write(f"**{st.session_state.user['full_name']}**")
                st.caption(st.session_state.user['email'])
                st.markdown("---")

            page = st.radio(
                "Navigation",
                ["📊 Dashboard", "📋 Attendance", "👥 Students", "📚 Courses", "📈 Reports"],
                label_visibility="collapsed"
            )

            st.markdown("---")
            if st.button("🚪 Logout", use_container_width=True):
                st.session_state.authenticated = False
                st.session_state.token = None
                st.session_state.user = None
                st.rerun()

        # Main content
        if page == "📊 Dashboard":
            show_overview()
        elif page == "📋 Attendance":
            show_attendance()
        elif page == "👥 Students":
            show_students()
        elif page == "📚 Courses":
            show_courses()
        elif page == "📈 Reports":
            show_reports()


if __name__ == "__main__":
    main()
