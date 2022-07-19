from parsers.parse_utils import parser, ParseError


def osm_speed_visitor(t):
    """Visitor function that outputs speeds in OSM tag syntax"""
    if t.data in {"normal_speed", "advisory_speed"}:
        tag = "maxspeed" if t.data == "normal_speed" else "maxspeed:advisory"

        speed = osm_speed_visitor(t.children[0])
        conditions = " AND ".join([osm_speed_visitor(child) for child in t.children[1:]])
        if conditions:
            tag += ":conditional"
            speed = f"{speed} @ ({conditions})"

        return {tag: speed}

    # speed
    elif t.data == "mph_speed":
        return f"{t.children[0]} mph"
    elif t.data == "kph_speed":
        return f"{t.children[0]}"
    elif t.data == "walk_speed":
        return "walk"

    # multilane speed
    elif t.data == "multilane_speed":
        return {
            "maxspeed:lanes": "|".join(list(osm_speed_visitor(child).values())[0] for child in t.children),
            **osm_speed_visitor(t.children[0]),
        }

    # restrictions
    elif t.data == "weight_restriction":
        return osm_speed_visitor(t.children[0])
    elif t.data == "weight_rating":
        return f"weightrating>{t.children[0]}{osm_weight_unit(t.children[1])}"
    elif t.data == "qualified_weight_pre":
        return f"{osm_weight_qualifier(t.children[0])}>{t.children[1]}{osm_weight_unit(t.children[2])}"
    elif t.data == "qualified_weight_post":
        return f"{osm_weight_qualifier(t.children[2])}>{t.children[0]}{osm_weight_unit(t.children[1])}"
    elif t.data == "length_restriction":
        return f"length>{t.children[0]}{osm_length_unit(t.children[1])}"
    elif t.data == "seat_restriction":
        return f"seats>={t.children[0]}"
    elif t.data == "axle_restriction":
        return f"axles>={t.children[0]}"
    elif t.data == "trailers_restriction":
        return f"trailers>={t.children[0]}"
    elif t.data == "restriction_conditional":
        return t.children[0]
    elif t.data == "date_time":
        return " ".join([osm_speed_visitor(child) for child in filter(None, t.children)])
    elif t.data == "time_interval":
        return osm_speed_visitor(t.children[0])
    if t.data in {"neg_interval", "weekday_span", "month_span"}:
        return f"{t.children[0]}-{t.children[1]}"
    elif t.data == "pos_interval":
        return f"{t.children[0]}+{t.children[1]}"
    elif t.data == "complex_time_span":
        return f"({osm_speed_visitor(t.children[0])})-({osm_speed_visitor(t.children[1])})"

    else:
        raise ParseError(f'Unexpected token "{t}"')

def osm_weight_unit(weight_unit):
    return "" if weight_unit == "t" else " " + weight_unit

def osm_length_unit(length_unit):
    return "" if length_unit == "m" else " " + length_unit

def osm_weight_qualifier(qualifier_type):
    if qualifier_type == "empty":
        return "emptyweight"
    elif qualifier_type == "capacity":
        return "weightcapacity"
    elif qualifier_type == "trailer":
        return "trailerweight"
    elif qualifier_type == "current":
        return "weight"
    else:
        raise ParseError(f'Unexpected qualifier "{qualifier_type}"')

def parse_speeds(s) -> dict:
    """Parses a speed definition string into a dictionary of OSM tags"""
    parse_tree = parser.parse(s)

    result = {}
    for speed in (osm_speed_visitor(speed_def) for speed_def in parse_tree.children):
        for k, v in speed.items():
            if k in result:
                result[k] += f"; {v}"
            else:
                result[k] = v

    return result
