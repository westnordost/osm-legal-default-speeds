# OSM Default Speeds

This project generates a machine-readable JSON from the data on the [Default speed limits](https://wiki.openstreetmap.org/wiki/Default_speed_limits) page from the OpenStreetMap wiki.

### Usage

The easiest way to get a JSON for the current wiki page is to just run the workflow [*"Generate default speed limits JSON"*](https://github.com/westnordost/osm-default-speeds/actions/workflows/generate-json.yml) directly on GitHub. It executes `main.py` and attaches a `default_speeds.json` as an artifact to each successful run.

A version of the generated [`default_speeds.json`](https://github.com/westnordost/osm-default-speeds/blob/master/output/default_speeds.json) is also situated in this repository but it may not be the most recent version as the wiki page may change from time to time.

### Credits

This project was started by @ianthetechie in 2019 and finally finished in 2022 by @westnordost as part of a [NLNet grant](https://nlnet.nl/project/OSM-SpeedLimits/).
