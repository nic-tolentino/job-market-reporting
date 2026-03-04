import csv

csv_file = "/tmp/roster.csv"
output_file = "/tmp/master_roster.md"

with open(csv_file, mode='r') as f:
    reader = csv.DictReader(f)
    with open(output_file, mode='w') as out:
        for row in reader:
            name = row['companyName']
            provider = row['provider']
            slug = row['extractedSlug']
            out.write(f"| {name} | {provider} | `{slug}` | TODO |\n")
