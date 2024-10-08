import pytest

from parsers.osm_restrictions import parse_speeds
from parsers.parse_utils import validate_road_types
from parsers.parse_utils import validate_road_types_in_speed_table

@pytest.mark.parametrize(
    "data,expected",
    [
        ("40", {"maxspeed": "40"}),
        ("40 mph", {"maxspeed": "40 mph"}),
        
        # Lanes
        ("80|60", {"maxspeed:lanes": "80|60"}),
        ("80|60|40", {"maxspeed:lanes": "80|60|40"}),
        
        # Conditionals
        ("40 mph (2t trailer)", {"maxspeed:conditional": "40 mph @ (trailerweight>2)"}),
        ("40 mph (0.75t trailer)", {"maxspeed:conditional": "40 mph @ (trailerweight>0.75)"}),
        ("40 mph (2st trailer)", {"maxspeed:conditional": "40 mph @ (trailerweight>2 st)"}),
        
        ("40 mph (articulated)", {"maxspeed:conditional": "40 mph @ (articulated)"}),
        ("40 mph (trailer)", {"maxspeed:conditional": "40 mph @ (trailer)"}),
        ("40 mph (caravan)", {"maxspeed:conditional": "40 mph @ (caravan)"}),
        ("40 mph (wet)", {"maxspeed:conditional": "40 mph @ (wet)"}),
        ("40 mph (empty)", {"maxspeed:conditional": "40 mph @ (empty)"}),
        
        ("40 mph (6 axles)", {"maxspeed:conditional": "40 mph @ (axles>=6)"}),
        ("40 mph (12 seats)", {"maxspeed:conditional": "40 mph @ (seats>=12)"}),
        ("40 mph (2 trailers)", {"maxspeed:conditional": "40 mph @ (trailers>=2)"}),
        
        ("40 mph (2t)", {"maxspeed:conditional": "40 mph @ (weightrating>2)"}),
        ("40 mph (2.5t)", {"maxspeed:conditional": "40 mph @ (weightrating>2.5)"}),
        
        ("40 (current 2t)", {"maxspeed:conditional": "40 @ (weight>2)"}),
        ("40 (2t current)", {"maxspeed:conditional": "40 @ (weight>2)"}),
        ("40 (empty 2t)", {"maxspeed:conditional": "40 @ (emptyweight>2)"}),
        ("40 (2t empty)", {"maxspeed:conditional": "40 @ (emptyweight>2)"}),
        ("40 (capacity 2t)", {"maxspeed:conditional": "40 @ (weightcapacity>2)"}),
        ("40 (2t capacity)", {"maxspeed:conditional": "40 @ (weightcapacity>2)"}),
        
        ("40 mph (2000lb)", {"maxspeed:conditional": "40 mph @ (weightrating>2000 lb)"}),
        ("40 mph (2st)", {"maxspeed:conditional": "40 mph @ (weightrating>2 st)"}),
        ("40 mph (2.5st)", {"maxspeed:conditional": "40 mph @ (weightrating>2.5 st)"}),

        ("40 mph (current 2st)", {"maxspeed:conditional": "40 mph @ (weight>2 st)"}),
        ("40 mph (2st current)", {"maxspeed:conditional": "40 mph @ (weight>2 st)"}),
        ("40 mph (empty 2st)", {"maxspeed:conditional": "40 mph @ (emptyweight>2 st)"}),
        ("40 mph (2st empty)", {"maxspeed:conditional": "40 mph @ (emptyweight>2 st)"}),
        ("40 mph (capacity 2st)", {"maxspeed:conditional": "40 mph @ (weightcapacity>2 st)"}),
        ("40 mph (2st capacity)", {"maxspeed:conditional": "40 mph @ (weightcapacity>2 st)"}),

        ("40 mph (10m)", {"maxspeed:conditional": "40 mph @ (length>10)"}),
        ("40 mph (10ft)", {"maxspeed:conditional": "40 mph @ (length>10 ft)"}),

        # Speed + Conditionals
        (
            "60mph, 40 mph (2t)",
            {"maxspeed": "60 mph", "maxspeed:conditional": "40 mph @ (weightrating>2)"},
        ),

        # Restrictions on conditionals
        (
            "40 mph (2t, articulated)",
            {"maxspeed:conditional": "40 mph @ (weightrating>2 AND articulated)"},
        ),
        # Multiple restrictions
        (
            "60mph, 40 mph (2t), 20mph (6 axles)",
            {"maxspeed": "60 mph", "maxspeed:conditional": "40 mph @ (weightrating>2); 20 mph @ (axles>=6)"},
        ),

        # Time intervals
        ("40 mph (sunset-sunrise)", {"maxspeed:conditional": "40 mph @ (sunset-sunrise)"}),
        (
            "40 mph ((sunset+01:30)-(sunrise-01:30))",
            {"maxspeed:conditional": "40 mph @ ((sunset+01:30)-(sunrise-01:30))"},
        ),
        ("40 (Sep-Jun)", {"maxspeed:conditional": "40 @ (Sep-Jun)"}),
        ("40 (Sep-Jun Mo-Fr)", {"maxspeed:conditional": "40 @ (Sep-Jun Mo-Fr)"}),
        ("40 (Sep-Jun Mo-Fr 08:00-16:00)", {"maxspeed:conditional": "40 @ (Sep-Jun Mo-Fr 08:00-16:00)"}),
        ("40 (08:00-16:00)", {"maxspeed:conditional": "40 @ (08:00-16:00)"}),
        ("40 (Mo-Fr)", {"maxspeed:conditional": "40 @ (Mo-Fr)"}),
        ("30 (Mo-Fr 08:00-17:00; PH,SH off)", {"maxspeed:conditional": "30 @ (Mo-Fr 08:00-17:00; PH,SH off)"}),
        ("30 (Oct-May Sa,Su)", {"maxspeed:conditional": "30 @ (Oct-May Sa,Su)"}),
        
        # Advisory speed
        ("advisory: 130", {"maxspeed:advisory": "130"}),
        
        # Min speed
        ("min: 50", {"minspeed": "50"}),
        
        # access prohibited
        ("X", { "access": "no" }),
        
        # Junk
        ("junk", None),  # Obviously invalid
        ("40 mph ((2t)", None),  # Mismatched braces
        ("40 mph (2t))", None),  # Mismatched braces
        ("40 mph (2u))", None),  # Invalid restriction
    ]
)
def test_parser(data, expected):
    try:
        result = parse_speeds(data)
        assert result == expected
    except:  # noqa: E722
        if expected is not None:
            raise

@pytest.mark.parametrize(
    "data,expected",
    [
        (
            {"alley": {"filter": "highway=service"}},
            []
        ),
        (
            {"alley": {"fuzzyFilter": "highway=service"}},
            []
        ),
        (
            {"alley": {"filter": "{service}"}},
            ["alley: Unable to map 'service'"]
        ),
        (
            {"alley": {"fuzzyFilter": "{service}"}},
            ["alley: Unable to map 'service'"]
        ),
        (
            {"urban": {"filter": "{lit}"}, "lit": { "filter": "lit=yes" }},
            []
        ),
        (
            {"urban": {"filter": "{lit} or {residential}"}, "lit": { "filter": "lit=yes" }},
            ["urban: Unable to map 'residential'"]
        ),
        (
            {"urban": {"filter": "{lit}"}, "rural": { "filter": "!{lit}" }},
            ["urban: Unable to map 'lit'", "rural: Unable to map 'lit'"]
        ),
    ]
)
def test_validate_road_types(data, expected):
    assert validate_road_types(data) == expected

@pytest.mark.parametrize(
    "speeds_by_country_code,road_types,expected",
    [
        (
            { "AA": [{}] },
            {},
            []
        ),
        (
            { "AA": [{ "name": "rural" }] },
            { "rural": {"filter": "lit=no"} },
            []
        ),
        (
            { "AA": [{ "name": "rural" }] },
            {},
            ["AA: Unable to map 'rural'"]
        ),
        (
            { "AA": [{ "name": "rural" }, { "name": "urban" }] },
            { "rural": {"filter": "lit=no"} },
            ["AA: Unable to map 'urban'"]
        ),
        (
            { "AA": [{ "name": "rural" }], "AB": [{ "name": "urban" }] },
            { "rural": {"filter": "lit=no"} },
            ["AB: Unable to map 'urban'"]
        ),
    ]
)
def test_validate_road_types_in_speed_table(speeds_by_country_code, road_types, expected):
    assert validate_road_types_in_speed_table(speeds_by_country_code, road_types) == expected

