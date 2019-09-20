import json

from bs4 import BeautifulSoup

import requests

from parsers.osm_restrictions import parse_speeds
from parsers.parse_utils import parse_road_types_table
from parsers.parse_utils import parse_speed_table

WIKI_API_URL = "https://wiki.openstreetmap.org/w/api.php"
WIKI_PAGE = "Default_speed_limits"


def get_page_html(api_url: str, page_name: str) -> str:
    res = requests.get(api_url, {"action": "parse", "page": page_name, "format": "json"})

    return res.json()["parse"]["text"]["*"]


if __name__ == "__main__":
    html_string = get_page_html(WIKI_API_URL, WIKI_PAGE)
    soup = BeautifulSoup(html_string, "html.parser")
    speed_table = soup.find_all("table")[0]
    road_types = parse_road_types_table(soup.find_all("table")[1])

    result = parse_speed_table(speed_table, road_types, parse_speeds)

    print(json.dumps(result, sort_keys=True, indent=4))
