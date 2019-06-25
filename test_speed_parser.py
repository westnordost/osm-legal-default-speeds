import pytest

from main import split_speeds, ParseError


@pytest.mark.parametrize(
    "data,expected",
    [
        ("40", ["40"]),  # Boring
        ("40, 50", ["40", "50"]),
        ("40 mph", ["40 mph"]),  # Add units
        ("40 mph, 50 mph", ["40 mph", "50 mph"]),
        ("40 mph (2t)", ["40 mph (2t)"]),  # Add restriction
        (
            "40 mph (2t, articulated), 30 mph (2t, articulated)",
            ["40 mph (2t, articulated)", "30 mph (2t, articulated)"],
        ),  # Add restriction with commas
        ("junk", None),  # Obviously invalid
        ("40 mph ((2t)", None),  # Mismatched braces
        ("40 mph (2t))", None),  # Mismatched braces
    ],
)
def test_regex(data, expected):
    try:
        result = split_speeds(data)
        assert result == expected
    except ParseError:
        if expected is not None:
            raise
