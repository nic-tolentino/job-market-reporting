#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "Creating .env from .env.example..."
    cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
    echo "Please edit $PROJECT_ROOT/.env with your actual values."
else
    echo ".env file already exists."
fi

# Load variables if they exist
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
    echo "Environment variables loaded from .env"
fi

echo "To manually set these in your current shell, run:"
echo "export \$(grep -v '^#' .env | xargs)"
