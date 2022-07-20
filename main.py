import json
import sys

from bs4 import BeautifulSoup

import requests

from parsers.osm_restrictions import parse_speeds
from parsers.parse_utils import parse_road_types_table
from parsers.parse_utils import parse_speed_table

WIKI_URL = "https://wiki.openstreetmap.org/wiki/"
WIKI_API_URL = "https://wiki.openstreetmap.org/w/api.php"
WIKI_PAGE = "Default_speed_limits"


if __name__ == "__main__":
    output_file_name = sys.argv[1] if len(sys.argv)>1 else "default_speeds.json"

    parsed = requests.get(WIKI_API_URL, {"action": "parse", "page": WIKI_PAGE, "format": "json"}).json()["parse"]
    html_string = parsed["text"]["*"]
    soup = BeautifulSoup(html_string, "html.parser")
    speed_table = soup.find_all("table")[0]
    road_types = parse_road_types_table(soup.find_all("table")[1])

    result = parse_speed_table(speed_table, road_types, parse_speeds)
    result["meta"] = {
        "source": WIKI_URL + WIKI_PAGE,
        "revision_id": parsed["revid"],
        "license": "Creative Commons Attribution-ShareAlike 2.0 license",
        "license_url": "https://wiki.openstreetmap.org/wiki/Wiki_content_license",
    }

    for warning in result['warnings']:
        print(warning)

    with open(output_file_name, "w", encoding='utf8') as file:
        file.write(json.dumps(result['speed_limits'], sort_keys=True, indent=2))
