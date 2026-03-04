import os

FINDINGS_FILE = "/Users/nic/Projects/job-market-reporting/ideas/ats-identification-findings.md"
MASTER_ROSTER_FILE = "/tmp/master_roster.md"

with open(FINDINGS_FILE, 'r') as f:
    lines = f.readlines()

with open(MASTER_ROSTER_FILE, 'r') as f:
    master_roster_lines = f.readlines()

new_lines = []
in_roster = False
added_roster = False

for line in lines:
    if "## Master Roster" in line:
        new_lines.append(line)
        in_roster = True
        continue
    
    if in_roster:
        if line.startswith("| Company Name"):
            new_lines.append(line)
            continue
        if line.startswith("|:---"):
            new_lines.append(line)
            if not added_roster:
                new_lines.extend(master_roster_lines)
                added_roster = True
            continue
        if line.startswith("---") or line.startswith("## Methodology"):
            in_roster = False
            new_lines.append(line)
            continue
        # Skip existing roster lines
        continue
    else:
        new_lines.append(line)

with open(FINDINGS_FILE, 'w') as f:
    f.writelines(new_lines)

