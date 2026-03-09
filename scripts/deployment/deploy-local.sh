#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

echo "🏗️  Starting Tech Market Backend locally..."

cd "$PROJECT_ROOT/backend" || exit

# Run the Spring Boot application using Gradle
./gradlew bootRun
