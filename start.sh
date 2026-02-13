#!/bin/bash

# Define log files
BACKEND_LOG="backend.log"
FRONTEND_LOG="frontend.log"

# Function to check if a process is running
is_running() {
    if [ -f "$1" ]; then
        if ps -p $(cat "$1") > /dev/null; then
            return 0
        fi
    fi
    return 1
}

# Start Backend
if is_running "backend.pid"; then
    echo "Backend is already running (PID: $(cat backend.pid))"
else
    echo "Starting Backend (Spring Boot)..."
    # Clear backend log file
    > $BACKEND_LOG
    cd backend
    ./gradlew bootRun > ../$BACKEND_LOG 2>&1 &
    BACKEND_PID=$!
    cd ..
    echo $BACKEND_PID > backend.pid
    echo "Backend started with PID $BACKEND_PID. Logs: $BACKEND_LOG"
fi

# Start Frontend
if is_running "frontend.pid"; then
    echo "Frontend is already running (PID: $(cat frontend.pid))"
else
    echo "Starting Frontend (Vite)..."
    # Clear frontend log file
    > $FRONTEND_LOG
    cd frontend
    npm run dev > ../$FRONTEND_LOG 2>&1 &
    FRONTEND_PID=$!
    cd ..
    echo $FRONTEND_PID > frontend.pid
    echo "Frontend started with PID $FRONTEND_PID. Logs: $FRONTEND_LOG"
fi

echo "All services started!"
