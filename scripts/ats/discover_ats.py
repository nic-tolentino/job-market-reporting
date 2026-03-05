import os
import re
import csv
import subprocess

FINDINGS_FILE = "/Users/nic/Projects/job-market-reporting/ideas/ats-identification-findings.md"
WEBSITES_FILE = "/tmp/company_websites.csv"

# Load websites
websites = {}
with open(WEBSITES_FILE, 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        websites[row['name'].strip()] = row['website'].strip()

# Load findings
with open(FINDINGS_FILE, 'r') as f:
    findings_content = f.read()

# Pattern for Master Roster table rows where ATS is NOT NONE (or NONE) to update tokens
table_row_pattern = re.compile(r'^\| ([^|]+) \| ([^|]+) \| ([^|]*) \| (.*)$', re.MULTILINE)

ats_markers = {
    'Greenhouse': {
        'marker': r'boards\.greenhouse\.io|boards-api\.greenhouse\.io|greenhouse\.io',
        'ident_regex': r'boards\.greenhouse\.io/([^/?#]+)'
    },
    'Lever': {
        'marker': r'jobs\.lever\.co|api\.lever\.co',
        'ident_regex': r'jobs\.lever\.co/([^/?#]+)'
    },
    'Ashby': {
        'marker': r'jobs\.ashbyhq\.com|api\.ashbyhq\.com',
        'ident_regex': r'jobs\.ashbyhq\.com/([^/?#]+)'
    },
    'Workday': {
        'marker': r'\.wd[0-9]\.myworkdayjobs\.com|\.workdayjobs\.com',
        'ident_regex': r'//([^.]+)\.wd[0-9]\.myworkdayjobs\.com'
    },
    'SnapHire': {
        'marker': r'\.snaphire\.com|/ajid/',
        'ident_regex': r'//([^.]+)\.snaphire\.com'
    },
    'BambooHR': {
        'marker': r'\.bamboohr\.com',
        'ident_regex': r'//([^.]+)\.bamboohr\.com'
    },
    'Teamtailor': {
        'marker': r'\.teamtailor\.com',
        'ident_regex': r'//([^.]+)\.teamtailor\.com'
    },
    'Workable': {
        'marker': r'apply\.workable\.com',
        'ident_regex': r'apply\.workable\.com/([^/?#]+)'
    },
    'SmartRecruiters': {
        'marker': r'careers\.smartrecruiters\.com',
        'ident_regex': r'smartrecruiters\.com/([^/?#\s]+)'
    },
    'SuccessFactors': {
        'marker': r'\.successfactors\.com|career.*\.successfactors\.com',
        'ident_regex': r'//([^.]+)\.successfactors\.com'
    },
    'JobAdder': {
        'marker': r'\.jobadder\.com|/jobadder/',
        'ident_regex': None # Usually embedded, harder to find slug easily via automated GET
    },
}

def check_url_for_ats(url):
    print(f"  Fetching: {url}")
    try:
        # Get effective URL (after redirects)
        redir_res = subprocess.run(
            ['curl', '-sIL', '-o', '/dev/null', '-w', '%{url_effective}', '-A', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', '--max-time', '5', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        effective_url = redir_res.stdout.strip()
        
        # Check effective URL first
        if effective_url:
            for ats_name, config in ats_markers.items():
                marker = config.get('marker')
                if marker and re.search(marker, effective_url, re.IGNORECASE):
                    token = None
                    ident = config.get('ident_regex')
                    if ident:
                        match = re.search(ident, effective_url, re.IGNORECASE)
                        if match:
                            token = match.group(1).lower()
                    return ats_name, token

        # Then fetch page body
        body_res = subprocess.run(
            ['curl', '-sL', '-A', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', '--max-time', '5', effective_url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        page_source = body_res.stdout
        
        for ats_name, config in ats_markers.items():
            marker = config.get('marker')
            if marker and re.search(marker, page_source, re.IGNORECASE):
                token = None
                # If we didn't find it in the URL, try the body for the token
                ident = config.get('ident_regex')
                if ident:
                    match = re.search(ident, page_source, re.IGNORECASE)
                    if match:
                        token = match.group(1).lower()
                return ats_name, token
    except Exception as e:
        pass
    return None, None

new_findings_content = findings_content

# We only run the script on companies where token is missing or it is NONE
matches = table_row_pattern.findall(findings_content)
for match in matches:
    name = match[0].strip()
    current_ats = match[1].strip()
    current_token = match[2].strip()
    
    # Only scan if NONE or we just identified it but don't have a token
    if current_ats != "NONE" and current_token != "``" and current_token != "":
        continue
        
    website = websites.get(name)
    if not website:
        continue
    if not (website.startswith('http://') or website.startswith('https://')):
        website = 'https://' + website
        
    print(f"\nScanning: {name} (Website: {website})")
    
    found_ats, found_token = None, None
    for path in ["/", "/careers", "/jobs", "/work-with-us", "/about-us/careers"]:
        test_url = website.rstrip('/') + path
        ats, token = check_url_for_ats(test_url)
        if ats:
            found_ats = ats
            found_token = token
            break
            
    if found_ats:
        print(f"==> Identified {found_ats} for {name}! (Token: {found_token})")
        # Update the file content in memory
        pattern = re.compile(r'^\| ' + re.escape(name) + r' \| ([^|]+) \| ([^|]*) \|', re.MULTILINE)
        t_str = f"`{found_token}`" if found_token else "``"
        new_row = f"| {name} | {found_ats} | {t_str} |"
        new_findings_content = pattern.sub(new_row, new_findings_content)
        
        # Write immediately to file so user can track
        with open(FINDINGS_FILE, 'w') as f:
            f.write(new_findings_content)

print("\nScan complete.")


print("\nScan complete.")
