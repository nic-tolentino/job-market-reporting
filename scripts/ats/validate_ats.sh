#!/bin/bash

FINDINGS_FILE="/Users/nic/Projects/job-market-reporting/ideas/ats-identification-findings.md"
TEMP_FILE="/tmp/findings.tmp"

validate_greenhouse() {
  local token=$1
  if [ -z "$token" ]; then echo "❓ Missing Token"; return; fi
  local response=$(curl -L -s "https://boards-api.greenhouse.io/v1/boards/${token}/jobs")
  local count=$(echo "$response" | grep -o "\"id\":" | wc -l | xargs)
  if [ "$count" -gt 0 ]; then
    echo "✅ Valid ($count jobs)"
  else
    echo "❌ Invalid/No Jobs"
  fi
}

validate_lever() {
  local slug=$1
  if [ -z "$slug" ]; then echo "❓ Missing Slug"; return; fi
  local response=$(curl -L -s "https://api.lever.co/v0/postings/${slug}?limit=100")
  local count=$(echo "$response" | grep -o "\"id\":" | wc -l | xargs)
  if [ "$count" -gt 0 ]; then
    echo "✅ Valid ($count jobs)"
  else
    echo "❌ Invalid/No Jobs"
  fi
}

validate_ashby() {
  local slug=$1
  if [ -z "$slug" ]; then echo "❓ Missing Slug"; return; fi
  local response=$(curl -L -s "https://api.ashbyhq.com/posting-api/job-board/${slug}")
  local count=$(echo "$response" | grep -o "\"id\":" | wc -l | xargs)
  if [ "$count" -gt 0 ]; then
    echo "✅ Valid ($count jobs)"
  else
    echo "❌ Invalid/No Jobs"
  fi
}

validate_workday() {
  local slug=$1
  if [ -z "$slug" ]; then echo "❓ Missing Slug"; return; fi
  # Workday doesn't have a simple public API like the others, so we just check if the domain exists
  if curl -s -I "https://${slug}.wd3.myworkdayjobs.com" | grep -q "200\|302"; then
    echo "✅ Domain Active"
  elif curl -s -I "https://${slug}.wd1.myworkdayjobs.com" | grep -q "200\|302"; then
    echo "✅ Domain Active"
  else
    echo "⚠️ Domain Check Failed"
  fi
}

# Process the file
rm -f "$TEMP_FILE"
in_roster=0
while IFS= read -r line; do
  if [[ "$line" == "## Master Roster"* ]]; then
    in_roster=1
    echo "$line" >> "$TEMP_FILE"
    continue
  fi
  
  if [[ $in_roster -eq 1 ]]; then
    if [[ "$line" == "---"* ]] || [[ "$line" == "## Methodology"* ]]; then
      in_roster=0
      echo "$line" >> "$TEMP_FILE"
      continue
    fi
    
    if [[ "$line" =~ ^\|.*\|.*\|.*\|.*\|$ ]]; then
      if [[ "$line" == "| Company Name"* ]] || [[ "$line" == "|:---"* ]]; then
        echo "$line" >> "$TEMP_FILE"
        continue
      fi
      
      company=$(echo "$line" | awk -F'|' '{print $2}' | xargs)
      provider=$(echo "$line" | awk -F'|' '{print $3}' | xargs)
      identifier=$(echo "$line" | awk -F'|' '{print $4}' | sed 's/`//g' | xargs)
      status=$(echo "$line" | awk -F'|' '{print $5}' | xargs)
      
      if [[ "$status" == "TODO" ]]; then
        echo "Validating $provider for $company..."
        case "$provider" in
          "Greenhouse")
            new_status=$(validate_greenhouse "$identifier")
            ;;
          "Lever")
            new_status=$(validate_lever "$identifier")
            ;;
          "Ashby")
            new_status=$(validate_ashby "$identifier")
            ;;
          "Workday")
            new_status=$(validate_workday "$identifier")
            ;;
          "NONE")
            new_status="—"
            ;;
          *)
            new_status="Identified (Pending Validation)"
            ;;
        esac
        echo "| $company | $provider | \`$identifier\` | $new_status |" >> "$TEMP_FILE"
      else
        echo "$line" >> "$TEMP_FILE"
      fi
    else
      echo "$line" >> "$TEMP_FILE"
    fi
  else
    echo "$line" >> "$TEMP_FILE"
  fi
done < "$FINDINGS_FILE"

mv "$TEMP_FILE" "$FINDINGS_FILE"
echo "Validation Process Complete."
