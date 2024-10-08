import json
import sys

import requests
import datetime

from bs4 import BeautifulSoup

from parsers.osm_restrictions import parse_speeds
from parsers.parse_utils import parse_road_types_table
from parsers.parse_utils import parse_speed_table
from parsers.parse_utils import validate_road_types
from parsers.parse_utils import validate_road_types_in_speed_table

WIKI_URL = "https://wiki.openstreetmap.org/wiki/"
WIKI_API_URL = "https://wiki.openstreetmap.org/w/api.php"
WIKI_PAGE = "Default_speed_limits"


if __name__ == "__main__":
    output_file_name = sys.argv[1] if len(sys.argv)>1 else "legal_default_speeds.json"

    parsed = requests.get(WIKI_API_URL, {"action": "parse", "page": WIKI_PAGE, "format": "json"}).json()["parse"]
    html_string = parsed["text"]["*"]
    # (UI editor of) mediawiki sometimes adds crap like this (no-break space)
    html_string_cleaned = html_string.replace("&#160;", " ")
    soup = BeautifulSoup(html_string_cleaned, "html.parser")
    speed_table = soup.find_all("table")[0]
    road_types = parse_road_types_table(soup.find_all("table")[1])

    result = parse_speed_table(speed_table, parse_speeds)
    result["meta"] = {
        "source": WIKI_URL + WIKI_PAGE,
        "revisionId": str(parsed["revid"]),
        "timestamp": datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat(),
        "license": "Creative Commons Attribution-ShareAlike 2.0 license",
        "licenseUrl": "https://wiki.openstreetmap.org/wiki/Wiki_content_license",
    }
    result["roadTypesByName"] = road_types
    result['warnings'] += validate_road_types(road_types)
    result['warnings'] += validate_road_types_in_speed_table(result['speedLimitsByCountryCode'], road_types)

    with open(output_file_name, "w", encoding='utf8') as file:
        file.write(json.dumps(result, sort_keys=True, indent=2))
