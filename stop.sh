#!/bin/bash

# Function to kill process by PID file
kill_process() {
    PID_FILE=$1
    NAME=$2

    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            echo "Stopping $NAME (PID: $PID)..."
            kill $PID
            rm "$PID_FILE"
            echo "$NAME stopped."
        else
            echo "$NAME is not running (PID file exists but process not found)."
            rm "$PID_FILE"
        fi
    else
        echo "No PID file for $NAME found."
    fi
}

# Kill Backend
kill_process "backend.pid" "Backend"

# Kill Frontend
kill_process "frontend.pid" "Frontend"

echo "All services stopped."
