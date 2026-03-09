#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

# Load .env (in case some tests depend on environment variables)
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

echo "🧪 Running Backend Unit Tests..."

cd "$PROJECT_ROOT/backend" || exit

# Run the tests using Gradle
./gradlew test

if [ $? -eq 0 ]; then
    echo -e "\n✅ Backend tests passed!"
else
    echo -e "\n❌ [ERROR] Backend tests failed."
    exit 1
fi
