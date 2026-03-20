#!/usr/bin/env python3
"""
Remove stale ATS configurations from company manifest files.
These are boards that returned 404 during validation.
"""

import json
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"

# All filenames that failed validation (404 / board not found)
STALE_FILES = [
    # LEVER (33)
    "4mation.json",
    "afterpay.json",
    "aginic.json",
    "ailo.json",
    "ansarada.json",
    "arq-group.json",
    "auth0.json",
    "cover-genius.json",
    "enett.json",
    "ento.json",
    "harrison-ai.json",
    "healthmatch.json",
    "klarna.json",
    "luzia.json",
    "merkle-aotearoa.json",
    "milkrun.json",
    "natobotics.json",
    "novus-recruitment-solutions.json",
    "papercut.json",
    "practifi.json",
    "revolut.json",
    "sai-global.json",
    "sg-consulting-limited.json",
    "shippit.json",
    "sine.json",
    "siteminder.json",
    "t-mapp.json",
    "tiliter.json",
    "till-payments.json",
    "tomtom.json",
    "ventraip.json",
    "versent.json",
    "xpansiv.json",
    # GREENHOUSE (38)
    "a-cloud-guru.json",
    "aiven.json",
    "alignerr.json",
    "antler.json",
    "appscore.json",
    "bending-spoons.json",
    "constructor-tech.json",
    "datto.json",
    "dispensed-global.json",
    "dovetail.json",
    "encompass-technologies.json",
    "glovo.json",
    "great-value-hiring.json",
    "hipages.json",
    "hnry.json",
    "hola-consultores-sl.json",
    "journi.json",
    "lightspeed-commerce.json",
    "meetup.json",
    "mistral-ai.json",
    "mryum.json",
    "neara.json",
    "nep-group-inc.json",
    "noggin.json",
    "notion.json",
    "pathify.json",
    "pra.json",
    "psc-by-rocket-lab.json",
    "re-leased.json",
    "real-time-innovations-rti.json",
    "redbubble.json",
    "sector-alarm-espa-a.json",
    "sector-alarm-group.json",
    "skutopia.json",
    "slalom.json",
    "whispir.json",
    "zinco-ai.json",
    "zipline-io.json",
    # TEAMTAILOR (88)
    "acci-n-contra-el-hambre.json",
    "acento.json",
    "acorn.json",
    "airtificial-intelligent-robots.json",
    "airtificial.json",
    "albany-toyota.json",
    "amazon-web-services-aws.json",
    "apd-global.json",
    "autoguru.json",
    "ayesa-digital.json",
    "b2brouter.json",
    "bcnc-group.json",
    "billigence.json",
    "blackroc-recruitment.json",
    "boon-solutions.json",
    "capgemini-engineering.json",
    "capgemini-invent.json",
    "clearroute.json",
    "destinus.json",
    "duco-limited.json",
    "eftsure.json",
    "energiot.json",
    "epi-company.json",
    "epson-australia.json",
    "evotix.json",
    "flowww.json",
    "fnac-espa-a.json",
    "grupo-de-prado.json",
    "henry-schein-one-asia-pacific.json",
    "henry-schein-one-uk.json",
    "henry-schein-one.json",
    "homa.json",
    "idealista.json",
    "innovamat.json",
    "isentia.json",
    "kapres-technology.json",
    "knowmad-mood.json",
    "lic-nz.json",
    "light-wonder.json",
    "logisfashion.json",
    "marks-sattin.json",
    "mercadona.json",
    "minsait.json",
    "mitre-10-new-zealand-limited.json",
    "momentum-consulting-group.json",
    "netcheck.json",
    "nzme.json",
    "opennebula-systems.json",
    "orbidi.json",
    "orisha-commerce.json",
    "palta.json",
    "pan-pac-forest-products-ltd.json",
    "panel-sistemas-informaticos.json",
    "papernest.json",
    "paymentology.json",
    "performanze.json",
    "plexus-tech.json",
    "powerfleet.json",
    "prima.json",
    "primer-impacto.json",
    "randstad-new-zealand.json",
    "rdt.json",
    "reclut.json",
    "rever-yc-s22.json",
    "saab-australia.json",
    "sacyr.json",
    "safewill.json",
    "sea-to-summit.json",
    "set-europa.json",
    "shipco-it.json",
    "silicon-quantum-computing.json",
    "sinch.json",
    "sita.json",
    "sleek.json",
    "sopra-steria.json",
    "southern-cross-university.json",
    "spawnpoint-media.json",
    "spoki.json",
    "talent-connect.json",
    "tecalliance.json",
    "tether.json",
    "tiktok.json",
    "tradeweb.json",
    "unir-rioja.json",
    "waimakariri-district-council.json",
    "webbeds.json",
    "wise-security-global.json",
    "yapily.json",
    "zepto.json",
    # SMARTRECRUITERS (7)
    "cuscal-limited.json",
    "kpmg-australia.json",
    "lendi.json",
    "morgan-mckinley.json",
    "oceanagold-corporation.json",
    "service-foods.json",
    "z-energy-nz.json",
]


def build_index() -> dict[str, Path]:
    """Build filename → path index for all company manifests."""
    index = {}
    for path in MANIFEST_DIR.rglob("*.json"):
        index[path.name] = path
    return index


def fix_file(path: Path) -> bool:
    """Remove the 'ats' field from a manifest file. Returns True if changed."""
    data = json.loads(path.read_text())
    if "ats" not in data:
        return False
    del data["ats"]
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    return True


def main():
    index = build_index()
    not_found = []
    fixed = []
    already_clean = []

    for filename in STALE_FILES:
        if filename not in index:
            not_found.append(filename)
            continue
        if fix_file(index[filename]):
            fixed.append(filename)
        else:
            already_clean.append(filename)

    print(f"Fixed:         {len(fixed)}")
    print(f"Already clean: {len(already_clean)}")
    if not_found:
        print(f"Not found ({len(not_found)}):")
        for f in not_found:
            print(f"  {f}")


if __name__ == "__main__":
    main()
