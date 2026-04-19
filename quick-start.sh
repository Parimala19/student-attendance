#!/bin/bash

# Quick Start Script for Attendance System
# This script starts all backend services for local testing

set -e

echo "🚀 Starting Attendance System..."
echo ""

# Check if docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop first."
    exit 1
fi

echo "📦 Starting services with Docker Compose..."
echo ""

cd "$(dirname "$0")/infra"

# Stop any existing containers
docker-compose down

# Start services
docker-compose up --build -d

echo ""
echo "⏳ Waiting for services to be ready..."
echo ""

# Wait for database
echo "Waiting for PostgreSQL..."
until docker-compose exec -T db pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo "✅ PostgreSQL is ready"

# Wait for backend
echo "Waiting for Backend API..."
until curl -s http://localhost:8000/health > /dev/null 2>&1; do
    sleep 1
done
echo "✅ Backend API is ready"

# Wait for dashboard
echo "Waiting for Streamlit Dashboard..."
until curl -s http://localhost:8501/_stcore/health > /dev/null 2>&1; do
    sleep 1
done
echo "✅ Streamlit Dashboard is ready"

echo ""
echo "✨ All services are running!"
echo ""
echo "📍 Access points:"
echo "   Backend API:      http://localhost:8000"
echo "   API Docs:         http://localhost:8000/docs"
echo "   Dashboard:        http://localhost:8501"
echo "   PostgreSQL:       localhost:5432"
echo "   Redis:            localhost:6379"
echo ""
echo "📱 For Android app:"
echo "   Emulator API URL: http://10.0.2.2:8000"
echo "   Physical Device:  http://$(ipconfig getifaddr en0 2>/dev/null || echo "YOUR_IP"):8000"
echo ""
echo "👤 Default admin credentials:"
echo "   Email:    admin@test.com"
echo "   Password: password123"
echo ""
echo "📋 Useful commands:"
echo "   View logs:    docker-compose -f infra/docker-compose.yml logs -f"
echo "   Stop all:     docker-compose -f infra/docker-compose.yml down"
echo "   Restart:      docker-compose -f infra/docker-compose.yml restart"
echo ""
echo "📖 See TESTING_GUIDE.md for complete testing instructions"
echo ""
