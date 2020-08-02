from bs4 import element
from lark import Lark

from parsers import SPEED_GRAMMAR

parser = Lark(SPEED_GRAMMAR)

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

            tags_filter = table_row_helper.get_td(1).get_text(" ", strip=True)
            fuzzy_tags_filter = table_row_helper.get_td(2).get_text(" ", strip=True)
            result[road_type] = {'filter': tags_filter, 'fuzzy_filter': fuzzy_tags_filter}

    return result


def parse_speed_table(table, road_types: dict, speed_parse_func) -> dict:
    column_names = []
    result = {}
    warnings = []
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
            road_type = table_row_helper.get_td(1).get_text(strip=True)

            road_tags = {}
            for col_idx in range(2, len(column_names)):
                td = table_row_helper.get_td(col_idx)
                speeds = td.get_text(strip=True)

                if speeds:
                    vehicle_type = column_names[col_idx]
                    try:
                        parsed_speeds = speed_parse_func(speeds)
                    except Exception:
                        parsed_speeds = {}
                        warnings.append(f'{country_code}: Unable to parse \'{vehicle_type}\' for \'{road_type}\'')

                    for maxspeed_key, maxspeed_value in parsed_speeds.items():
                        if vehicle_type != "(default)":
                            maxspeed_key = maxspeed_key.replace("maxspeed:", "maxspeed:" + vehicle_type + ":", 1)
                        road_tags[maxspeed_key] = maxspeed_value

            road_filters = road_types[road_type] if road_type in road_types else None

            if not road_type or road_filters:
                if country_code not in result:
                    result[country_code] = []

                road_class = {'tags': road_tags}

                if road_type:
                    road_class['name'] = road_type
                    if road_filters['filter']:
                        road_class['filter'] = road_filters['filter']
                    else:
                        warnings.append(f'{country_code}: There is only a fuzzy filter for \'{road_type}\'')

                    if road_filters['fuzzy_filter']:
                        road_class['fuzzy_filter'] = road_filters['fuzzy_filter']

                result[country_code].append(road_class)
            else:
                warnings.append(f'{country_code}: Unable to map \'{road_type}\'')

    return {'speed_limits': result, 'warnings': warnings}
