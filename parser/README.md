# OSM Legal Default Speeds Parser

This project generates a JSON from the data on the [Default speed limits](https://wiki.openstreetmap.org/wiki/Default_speed_limits) page from the OpenStreetMap wiki.

### Usage

The easiest way to get a JSON for the current wiki page is to just run the workflow [*"Generate JSON"*](https://github.com/westnordost/osm-legal-default-speeds/actions/workflows/generate-json.yml) directly on GitHub. It executes `main.py` and attaches a `legal_default_speeds.json` as an artifact to each successful run.

A version of the generated [`legal_default_speeds.json`](https://github.com/westnordost/osm-legal-default-speeds/blob/master/demo/distribution/legal_default_speeds.json) is also situated in this repository but it may not be the most recent version as the wiki page may change from time to time.

### Credits

This project was started by [@ianthetechie](https://github.com/ianthetechie) in 2019 and finally finished in 2022 by [@westnordost](https://github.com/westnordost) as part of a [NLNet grant](https://nlnet.nl/project/OSM-SpeedLimits/).
