from parsers.parse_utils import parser, ParseError


def osm_speed_visitor(t):
    """Visitor function that outputs speeds in OSM tag syntax"""
    if t.data in {'normal_speed', 'advisory_speed'}:
        tag = 'maxspeed' if t.data == 'normal_speed' else 'maxspeed:advisory'

        speed = osm_speed_visitor(t.children[0])
        conditions = ' AND '.join([osm_speed_visitor(child) for child in t.children[1:]])
        if conditions:
            tag += ':conditional'
            speed = f'{speed} @ ({conditions})'

        return {
            tag: speed,
        }
    elif t.data == 'mph_speed':
        return f'{t.children[0]} mph'
    elif t.data == 'kph_speed':
        return f'{t.children[0]}'
    elif t.data == 'walk_speed':
        return 'walk'
    elif t.data == 'weight_restriction':
        return osm_speed_visitor(t.children[0])
    elif t.data == 'qualified_restriction':
        return t.children[0].data
    elif t.data == 'normal_weight':
        return f'weightrating>{t.children[0]}'
    elif t.data in {'qualified_weight_pre', 'qualified_weight_post'}:
        qualifier_type = t.children[0 if t.data == 'qualified_weight_pre' else 1].data
        if qualifier_type == 'empty':
            weight_qualifier = 'emptyweight'
        elif qualifier_type == 'capacity':
            weight_qualifier = 'weightcapacity'
        elif qualifier_type == 'trailer':
            weight_qualifier = 'trailerweight'
        else:
            raise ParseError(f'Unexpected qualifier "{qualifier_type}"')

        weight = t.children[1 if t.data == 'qualified_weight_pre' else 0]

        return f'{weight_qualifier}>{weight}'
    elif t.data == 'length_restriction':
        return f'length>{t.children[0]}'
    elif t.data == 'seat_restriction':
        return f'seats>={t.children[0]}'
    elif t.data == 'axle_restriction':
        return f'axles>={t.children[0]}'
    elif t.data == 'conditional_restriction':
        return t.children[0].data
    else:
        raise ParseError(f'Unexpected token "{t}"')


def parse_speeds(s) -> dict:
    """Parses a speed definition string into a dictionary of OSM tags"""
    parse_tree = parser.parse(s)

    result = {}
    for speed in (osm_speed_visitor(speed_def) for speed_def in parse_tree.children):
        for k, v in speed.items():
            if k in result:
                result[k] += f'; {v}'
            else:
                result[k] = v

    return result
