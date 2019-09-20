import pytest

from parsers.osm_restrictions import parse_speeds


@pytest.mark.parametrize(
    "data,expected",
    [
        ("40", {"maxspeed": "40"}),  # Boring
        ("40 mph", {"maxspeed": "40 mph"}),  # Add units
        ("80|60", {"maxspeed:lanes": "80|60", "maxspeed": "80"}),  # Multiple lanes
        ("80|60|40", {"maxspeed:lanes": "80|60|40", "maxspeed": "80"}),  # Multiple lanes
        (
            "60mph, 40 mph (2t)",
            {"maxspeed": "60 mph", "maxspeed:conditional": "40 mph @ (weightrating>2)"},
        ),  # Add restriction
        (
            "40 mph (2t, articulated)",
            {"maxspeed:conditional": "40 mph @ (weightrating>2 AND articulated)"},
        ),  # Multiple conditions
        (
            "60mph, 40 mph (2t), 20mph (6 axles)",
            {"maxspeed": "60 mph", "maxspeed:conditional": "40 mph @ (weightrating>2); 20 mph @ (axles>=6)"},
        ),  # Multiple restriction speeds
        ("40 mph (2t trailer)", {"maxspeed:conditional": "40 mph @ (trailerweight>2)"}),
        ("40 mph (articulated)", {"maxspeed:conditional": "40 mph @ (articulated)"}),
        ("40 mph (trailer)", {"maxspeed:conditional": "40 mph @ (trailer)"}),
        ("40 mph (caravan)", {"maxspeed:conditional": "40 mph @ (caravan)"}),
        ("40 mph (wet)", {"maxspeed:conditional": "40 mph @ (wet)"}),
        ("40 mph (6 axles)", {"maxspeed:conditional": "40 mph @ (axles>=6)"}),
        ("40 mph (12 seats)", {"maxspeed:conditional": "40 mph @ (seats>=12)"}),
        ("40 mph (empty 2t)", {"maxspeed:conditional": "40 mph @ (emptyweight>2)"}),
        ("40 mph (capacity 2t)", {"maxspeed:conditional": "40 mph @ (weightcapacity>2)"}),
        ("40 mph (10m)", {"maxspeed:conditional": "40 mph @ (length>10)"}),
        ("40 mph (sunset-sunrise)", {"maxspeed:conditional": "40 mph @ (sunset-sunrise)"}),  # Time intervals
        ("40 mph (sunset+01:00)", {"maxspeed:conditional": "40 mph @ (sunset+01:00)"}),
        (
            "40 mph ((sunset+01:30)-(sunrise-01:30))",
            {"maxspeed:conditional": "40 mph @ ((sunset+01:30)-(sunrise-01:30))"},
        ),
        ("advisory: 130", {"maxspeed:advisory": "130"}),  # Advisory speed
        ("junk", None),  # Obviously invalid
        ("40 mph ((2t)", None),  # Mismatched braces
        ("40 mph (2t))", None),  # Mismatched braces
        ("40 mph (2u))", None),  # Invalid restriction
    ],
)
def test_parser(data, expected):
    try:
        result = parse_speeds(data)
        assert result == expected
    except:  # noqa: E722
        if expected is not None:
            raise
