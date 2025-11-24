#!/bin/bash

# AudioScholar Spring Boot Launcher for Unix-like systems (Linux/macOS)
# This script loads environment variables from .env and starts the Spring Boot application

set -e  # Exit on any error

# Color codes for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Header
echo "=========================================="
echo -e "${BLUE}AudioScholar Spring Boot Launcher${NC}"
echo "=========================================="
echo ""

# Check if .env file exists
if [ ! -f ".env" ]; then
    print_warning ".env file not found in current directory!"
    echo "Make sure you are running this from the project root directory."
    echo ""
else
    print_success ".env file found"
fi

# Function to export variables from .env file
load_env_file() {
    local env_file=".env"
    
    if [ ! -f "$env_file" ]; then
        print_warning "No .env file found. Using system environment variables."
        return 1
    fi
    
    print_status "Loading .env file..."
    
    # Count total variables to load
    local total_vars=0
    local loaded_vars=0
    
    # First pass: count variables
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        if [[ -n "$line" && ! "$line" =~ ^[[:space:]]*# ]] && [[ "$line" =~ = ]]; then
            ((total_vars++))
        fi
    done < "$env_file"
    
    # Second pass: load variables
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        if [[ -n "$line" && ! "$line" =~ ^[[:space:]]*# ]] && [[ "$line" =~ = ]]; then
            # Extract key and value
            local key="${line%%=*}"
            local value="${line#*=}"
            
            # Remove leading/trailing whitespace from key
            key=$(echo "$key" | xargs)
            
            # Remove leading/trailing whitespace from value and unquote if needed
            value=$(echo "$value" | xargs)
            if [[ "$value" =~ ^\".*\"$ ]] || [[ "$value" =~ ^'.*'$ ]]; then
                value="${value:1:-1}"  # Remove surrounding quotes
            fi
            
            # Export the variable
            export "$key=$value"
            ((loaded_vars++))
            echo "  Set: $key=$value"
        fi
    done < "$env_file"
    
    print_success "Loaded $loaded_vars environment variables from .env file"
    return 0
}

# Load environment variables
if load_env_file; then
    echo ""
else
    print_status "Continuing with system environment variables..."
    echo ""
fi

# Function to check if required tools are available
check_requirements() {
    local missing_tools=()
    
    # Check for Java
    if ! command -v java &> /dev/null; then
        missing_tools+=("Java")
    fi
    
    # Check for Maven (either mvn or mvnw)
    if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
        missing_tools+=("Maven")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        echo ""
        echo "Please install the missing tools and try again."
        echo "Java: https://adoptium.net/ or https://www.oracle.com/java/"
        echo "Maven: https://maven.apache.org/install.html"
        exit 1
    fi
    
    # Check Java version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print_success "Java version: $java_version"
    
    # Check Maven version
    if command -v mvn &> /dev/null; then
        mvn_version=$(mvn -version | head -n1)
        print_success "$mvn_version"
    else
        mvn_version=$(./mvnw -version | head -n1)
        print_success "Maven wrapper: $mvn_version"
    fi
}

# Check requirements
print_status "Checking system requirements..."
check_requirements
echo ""

# Start the application
print_status "Starting Spring Boot application..."
echo "=========================================="
echo ""

# Determine which Maven command to use
if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
    # Make sure mvnw is executable
    chmod +x ./mvnw 2>/dev/null || true
else
    print_error "No Maven command found (neither 'mvn' nor './mvnw' available)"
    exit 1
fi

# Run Spring Boot application with local profile
print_status "Running: $MVN_CMD spring-boot:run -Dspring-boot.run.profiles=local"
echo ""

# Execute the command
if $MVN_CMD spring-boot:run -Dspring-boot.run.profiles=local; then
    print_success "Application stopped successfully"
else
    exit_code=$?
    print_error "Application exited with code $exit_code"
    exit $exit_code
fi

echo ""
print_success "Script completed"