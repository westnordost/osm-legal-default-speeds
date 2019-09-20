from bs4 import element
from lark import Lark

from parsers import SPEED_GRAMMAR

parser = Lark(SPEED_GRAMMAR)

unmapped_road_types = {
    "(default)",
    "urban road",
    "urban Alberta provincial highway",
    "urban road without center line",
    "urban dual carriageway",
    "urban single carriageway",
    "urban",
    "residence district",
    "business district",
    "non-urban residential",
    "urban main road",
    "urban local road",
    "urban secondary road",
    "urban collector road",
    "urban motorway",
    "urban fast transit road",
    "urban main road with 2 lanes in each direction",
    "urban main road with 3 lanes in each direction",
    "Brunei limit area",
    "school zone",
    "urban school zone",
    "rural school zone",
    "school zones",
    "road without asphalt or concrete surface",
    "living_street",
    "urban public park",
    "Quebec autoroute",
    "unpaved road",
    "perimetral",
    "residential road",
    "rural road",
    "secondary road",
    "main road",
    "fast transit road",
    "dual carriageway with 2 lanes or morein each direction",
    "winding mountain road",
    "Kerala state highway",
    "Kerala national highway",
    "road with 4 or more lanes",
    "Punjab state highway",
    "Punjab state highway with dual carriageway",
    "Punjab national highway",
    "Punjab national highway with dual carriageway",
    "Nicaraguan carretera",
    "hilly road",
    "alley",
    "pedestrian",
    "road with asphalt or concrete surface in an unincorporated area",
    "open mountain road",
    "suburban district",
    "state park and preserve drive",
    "state board institutional road",
    "Kansas county or township highway",
    "single carriageway in residence district",
    "dual carriageway in residence district",
    "trailer park",
    "public park",
    "urban US interstate highway",
    "urban dual carriageway with 2 or more lanes in each direction",
    "urban motorway with 2 or more lanes in each direction",
    "numbered road with 2 lanes",
    "motorway with 2 or more lanes in each direction",
    "Nebraska state highway",
    "Nebraska state expressway",
    "US defense highway",
    "urban residence district",
    "rural residence district",
    "suburban business district",
    "suburban residence district",
    "urban Ohio state route",
    "urban motorway with paved shoulders",
    "national park road",
    "trunk without traffic lights",
    "Oklahoma state park road",
    "urban road which is not numbered",
    "US interstate highway with 2 or more lanes in each direction",
    "Virginia rural rustic road",
    "Virginia state primary highway",
    "suburban",
    "Wisconsin rustic road",
}


class ParseError(Exception):
    pass


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


def is_uninteresting(tag: element.Tag):
    return tag.name in {"sup", "img"}


def parse_road_types_table(table) -> dict:
    result = {}
    table_row_helper = TableRowHelper()

    # Remove links (footnotes etc), images, etc. that don't serialize well.
    for junk_tag in table.find_all(is_uninteresting):
        junk_tag.decompose()

    for row in table.find_all("tr"):
        # Loop through columns
        tds = row.find_all("td")
        table_row_helper.set_tds(tds)
        if tds:
            road_type = table_row_helper.get_td(0).get_text(strip=True)

            td = table_row_helper.get_td(1)
            result[road_type] = td.get_text(strip=True)

    return result


def parse_speed_table(table, road_types: dict, speed_parse_func) -> dict:
    column_names = []
    result = {}
    table_row_helper = TableRowHelper()

    # Remove links (footnotes etc), images, etc. that don't serialize well.
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
                        parsed_speeds = speed_parse_func(speeds)
                    except ParseError as e:
                        raise ParseError(f'Parsing "{vehicle_type}" for "{road_type}" in {country_code}:\n{str(e)}')

                    speeds_by_vehicle_type[vehicle_type] = parsed_speeds

            if road_type not in unmapped_road_types:
                try:
                    road_type = road_types[road_type]
                except KeyError:
                    raise ParseError(f'Unrecognized road type "{road_type}"')

            if country_code in result:
                result[country_code][road_type] = speeds_by_vehicle_type
            else:
                result[country_code] = {road_type: speeds_by_vehicle_type}

    return result
