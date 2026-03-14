#!/bin/bash
node tests/fixtures/mock-server.js &
MOCK_PID=$!
PORT=8083 GEMINI_API_KEY=test npm run dev > /dev/null 2>&1 &
SERVER_PID=$!
sleep 3
CRAWLER_URL=http://localhost:8083 npx jest --config jest.e2e.config.js -t "respects the 50-page pagination safety cap"
kill $SERVER_PID
kill $MOCK_PID
