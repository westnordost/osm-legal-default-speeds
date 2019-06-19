import json
import re

from bs4 import BeautifulSoup, element

import requests

WIKI_API_URL = "https://wiki.openstreetmap.org/w/api.php"
WIKI_PAGE = "Default_speed_limits"

# https://regexper.com/#(advisory:)?\s*((?:[0-9]+(?:\s?mph)?)(?:\s?\|\s?(?:[0-9]+(?:\s?mph)?))*|walk)\s*(\(.+\))?
SPEED_REGEX = re.compile(r"(advisory:)?\s*((?:[0-9]+(?:\s?mph)?)(?:\s?\|\s?(?:[0-9]+(?:\s?mph)?))*|walk)\s*(\(.+\))?")


class TableRowHelper:
    """A simplified interface around a set of table rows from bs4.

    This abstracts all the evil stuff like rowspan and colspan so that the data
    can be reasonably parsed.
    """

    def __init__(self):
        self.td_cache = {}

    def set_tds(self, tds: [element.Tag]):
        # Nuke any existing cache entries that "expire" this round (rowspan)
        for k in list(self.td_cache.keys()):
            (remaining, value) = self.td_cache[k]
            if remaining == 1:
                del self.td_cache[k]
            else:
                self.td_cache[k] = (remaining - 1, value)

        # Add new data for this row
        col_idx = 0
        for td in tds:
            rowspan = int(td.get("rowspan", 1))

            while col_idx in self.td_cache:
                col_idx += 1  # Skip cols that are around from a prev iteration due to rowspan

            for _ in range(int(td.get("colspan", 1))):
                self.td_cache[col_idx] = (rowspan, td)
                col_idx += 1

    def get_td(self, idx) -> element.Tag:
        return self.td_cache[idx][1]


def get_page_html(api_url: str, page_name: str) -> str:
    res = requests.get(api_url, {"action": "parse", "page": page_name, "format": "json"})

    return res.json()["parse"]["text"]["*"]


def is_uninteresting(tag: element.Tag):
    return tag.name in {"sup", "img"}


def split_speeds(str) -> list:
    braces = 0
    res = []
    current = []
    for c in (str + ","):
        if c == "," and braces == 0:
            entry = "".join(current).strip()
            if not SPEED_REGEX.fullmatch(entry):
                raise ValueError("Invalid syntax for \"{0}\" in \"{1}\"".format(entry, str))
            res.append(entry)
            current = []
        else:
            if c == "(":
                braces += 1
            elif c == ")":
                braces -= 1
                if braces < 0:
                    raise ValueError("Too many closing braces in \"{0}\"".format(str))
            current.append(c)
    if braces > 0:
        raise ValueError("Too many opening braces in \"{0}\"".format(str))
    return res

def parse_speed_table(table) -> dict:
    column_names = []
    result = {}
    table_row_helper = TableRowHelper()

    # Remove links (footnotes etc), images, etc. that don't serialaze well.
    for junk_tag in table.find_all(is_uninteresting):
        junk_tag.decompose()

    for row in table.find_all("tr"):
        # Handle column names
        th_tags = row.find_all("th")
        if len(th_tags) > 0:
            if len(column_names) == 0:
                for th in th_tags:
                    th_text = th.get_text(strip=True)
                    for _ in range(int(th.get("colspan", 1))):
                        column_names.append(th_text)
            else:
                for (i, th) in enumerate(th_tags):
                    th_text = th.get_text(strip=True)
                    if th_text:
                        for j in range(int(th.get("colspan", 1))):
                            column_names[i + j] = th_text

        # Loop through columns
        tds = row.find_all("td")
        table_row_helper.set_tds(tds)
        if tds:
            country_code = table_row_helper.get_td(0).get_text(strip=True)
            road_type = table_row_helper.get_td(1).get_text(strip=True) or "(default)"

            speeds_by_vehicle_type = {}
            for col_idx in range(2, len(column_names)):
                td = table_row_helper.get_td(col_idx)
                speeds = td.get_text(strip=True)

                if speeds:
                    vehicle_type = column_names[col_idx]
                    try:
                        speeds_list = split_speeds(speeds)
                    except ValueError as e:
                        raise ValueError("Parsing \"{0}\" for \"{1}\" in {2}:\n{3}".format(
                            vehicle_type, road_type, country_code, str(e))
                        )
                    # TODO: Use these groups in the next stage to build a set of proper restrictions
                    speeds_by_vehicle_type[vehicle_type] = speeds_list

            if country_code in result:
                result[country_code][road_type] = speeds_by_vehicle_type
            else:
                result[country_code] = {road_type: speeds_by_vehicle_type}

    return result


if __name__ == "__main__":
    html_string = get_page_html(WIKI_API_URL, WIKI_PAGE)
    soup = BeautifulSoup(html_string, "html.parser")
    speed_table = soup.find_all("table")[0]  # Grab the first table
    result = parse_speed_table(speed_table)

    print(json.dumps(result, sort_keys=True, indent=4))
