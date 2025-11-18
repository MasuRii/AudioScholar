#!/bin/bash

# AudioScholar Load Test Monitoring Script
# This script monitors system resources during load testing

echo "Starting AudioScholar Load Test Monitoring..."
echo "Timestamp: $(date)"
echo "=================================================="

# Check if the application is running
echo "Checking application status..."
curl -f -s http://localhost:8080/actuator/health || echo "Application health check failed"
echo ""

# Monitor CPU and Memory usage
echo "System Resource Usage:"
echo "-----------------------"
top -b -n 1 | head -20
echo ""

# Monitor disk usage
echo "Disk Usage:"
echo "-----------"
df -h
echo ""

# Monitor network connections
echo "Network Connections:"
echo "--------------------"
netstat -an | grep :8080 || echo "No connections on port 8080"
echo ""

# Monitor Java processes
echo "Java Processes:"
echo "---------------"
ps aux | grep java | grep -v grep || echo "No Java processes found"
echo ""

# Function to monitor in a loop
monitor_loop() {
    local duration=$1
    local interval=$2
    local end_time=$(($(date +%s) + duration))
    
    echo "Starting ${duration}s monitoring with ${interval}s intervals..."
    echo "Press Ctrl+C to stop monitoring"
    echo ""
    
    while [ $(date +%s) -lt $end_time ]; do
        echo "=== $(date) ==="
        
        # CPU and Memory
        echo "CPU Usage:"
        top -b -n 1 | grep "Cpu(s)" | awk '{print $2 $3}' || echo "CPU info unavailable"
        
        echo "Memory Usage:"
        free -h | grep Mem || echo "Memory info unavailable"
        
        # Application logs (if available)
        echo "Recent Application Logs:"
        if [ -f "application.log" ]; then
            tail -n 5 application.log
        else
            echo "Application log not found"
        fi
        
        echo ""
        sleep $interval
    done
}

# Check if duration is provided
if [ -n "$1" ]; then
    monitor_loop $1 ${2:-10}
else
    echo "Usage: $0 <duration_in_seconds> [interval_in_seconds]"
    echo "Example: $0 300 30  # Monitor for 5 minutes with 30-second intervals"
fi