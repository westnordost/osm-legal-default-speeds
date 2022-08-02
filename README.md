# OSM Legal Default Speeds

Kotlin multiplatform library that provides information about legal default speed limits.

It can be used by data consumers such as router software to fill the gaps in OpenStreetMap data, 
i.e. when explicit `maxspeed` tagging is missing because there is no explicit sign or because it 
hasn't been tagged yet. 

Additionally, it can be used to supplement the legal speed limits for other vehicle types (busses, 
goods vehicles, motorcycles, ...), as these can be assumed to never be tagged unless explicitly
signed.

It takes the data from the [Default speed limits](https://wiki.openstreetmap.org/wiki/Default_speed_limits)
page from the OpenStreetMap wiki as input, best parsed from a JSON.
<!--
## Installation

Add [`de.westnordost:osm-legal-default-speeds:1.0`](https://mvnrepository.com/artifact/de.westnordost/osm-legal-default-speeds/1.0) 
as a Maven dependency or download the jar from there.
-->
## Usage

### Parse Data

You need to parse the `default_speeds.json` with the JSON l ibrary of your choice and feed its data
into the constructor.

<details>
<summary>Example parsing it with <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx-serialization</a> (click to expand)</summary>

```kotlin
@Serializable data class SpeedLimitsJson(
    val meta: Map<String, String>,
    val roadTypes: Map<String, RoadTypeFilterJson>,
    val speedLimits: Map<String, List<RoadTypeJson>>,
    val warnings: List<String>
)

@Serializable data class RoadTypeFilterJson(
    override val filter: String? = null,
    override val fuzzyFilter: String? = null,
    override val relationFilter: String? = null
) : RoadTypeFilter

@Serializable data class RoadTypeJson(
    override val name: String? = null,
    override val tags: Map<String, String>
) : RoadType


val data: SpeedLimitsJson = Json.decodeFromStream(defaultSpeedsJsonFile.openStream())
```
</details>

```kotlin
val legalSpeeds = LegalDefaultSpeeds(data.roadTypes, data.speedLimits)
```

### Query speed limits

Specify the ISO 3166-1 alpha 2 code the road segment is located in and its tags.
```kotlin
legalSpeeds.getSpeedLimits("DK", mapOf("highway" to "motorway"))
```

This returns:
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = "motorway",
    tags = mapOf(
        "maxspeed" to "130",
        "maxspeed:bus:conditional" to "80 @ (weightrating>3.5)",
        "maxspeed:coach" to "100",
        "maxspeed:conditional" to "80 @ (trailer); 80 @ (weightrating>3.5)",
        "maxspeed:hgv" to "80",
        "minspeed" to "50"
    ),
    certitude = Certitude.Exact
)
```

## Features

### Matching by relation membership
Especially in the United States but in some other countries as well, road types can be identified by
membership in a (route) relation. So if you have access to the relations each road segment is a 
member of, you can specify the tags of the relations to improve accuracy.

Tag filters for relations are defined in the fourth column of the
[road types table](https://wiki.openstreetmap.org/wiki/Default_speed_limits#Road_types_to_tag_filters)
in the wiki.

<details>
<summary>Example</summary>

```kotlin
legalSpeeds.getSpeedLimits(
    "US-ND",
    tags = mapOf("lanes" to "2", "oneway" to "yes"),
    relationsTags = listOf(mapOf("type" to "route", "route" to "road", "network" to "US:I"))
)
```
...returns...
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = "US interstate highway with 2 or more lanes in each direction",
    tags = mapOf("maxspeed" to "75 mph"),
    certitude = Certitude.Exact
)
```
</details>

### Replacing placeholders

Matching for certain properties of a road, such as whether it is urban or not, is done via
placeholders. See any "tag" in curly braces in the
[road types table](https://wiki.openstreetmap.org/wiki/Default_speed_limits#Road_types_to_tag_filters).
These can be replaced, if for example you have another data source with which it is possible to
determine a property more precisely.

<details>
<summary>Example</summary>

```kotlin
legalSpeeds.getSpeedLimits("US-MO", mapOf("highway" to "motorway"), null)
{ (name, evaluate) -> 
    if (name == "urban") myDataSource.isUrban(roadSegment) else evaluate()
}
```
...returns (if `myDataSource.isUrban` returns true)...
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = "urban motorway",
    tags = mapOf("maxspeed" to "60 mph"),
    certitude = Certitude.Exact
)
```
</details>

### Matching by given speed limit

When tags of the given road segment already contain a `maxspeed` value, a "reverse" match by that 
value is attempted in case no exact match was found.

<details>
<summary>Example</summary>

```kotlin
legalSpeeds.getSpeedLimits("AT", mapOf("maxspeed" to "100"))
```
...returns...
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = "rural",
    tags = mapOf(
        "maxspeed:bus" to "80",
        "maxspeed:bus:conditional" to "70 @ (articulated)",
        "maxspeed:conditional" to "80 @ (trailer); 70 @ (weightrating>3.5)",
        "maxspeed:hgv" to "70"
    ),
    certitude = Certitude.FromMaxSpeed
)
```
</details>

This only works if the given `maxspeed` matches exactly the `maxspeed` of the road type it should 
match with. Such matching is preferred over fuzzy matches.

The tags returned are always only the tags *additional* to the ones the road segment already has, so
the `maxspeed` of 100 km/h is omitted in the result.

### Fuzzy matching

Which tag filters constitute fuzzy tag matching rules are defined in the third column of the
[road types table](https://wiki.openstreetmap.org/wiki/Default_speed_limits#Road_types_to_tag_filters)
in the wiki.

<details>
<summary>Example</summary>

```kotlin
legalSpeeds.getSpeedLimits("BO", mapOf("highway" to "residential"))
```
...returns...
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = "urban",
    tags = mapOf("maxspeed" to "40"),
    certitude = Certitude.Fuzzy
)
```
...because roads tagged with `highway=residential` are oftentimes urban roads.
</details>

### Fallback to default road type

If nothing matches, the speed limits of the default road type are returned. The default road type is
usually the default road outside settlements. Some countries do not have default road types defined,
in which case simply `null` is returned.

<details>
<summary>Example</summary>

```kotlin
legalSpeeds.getSpeedLimits("GD", mapOf())
```
...returns...
```kotlin
LegalDefaultSpeeds.Result(
    roadTypeName = null,
    tags = mapOf(
        "maxspeed" to "40 mph",
        "maxspeed:bus" to "35 mph",
        "maxspeed:goods" to "35 mph"
    ),
    certitude = Certitude.Fallback
)
```
</details>

## Credits

This library was made possible with a [grant by NLNet Zero Discovery](https://nlnet.nl/project/OSM-SpeedLimits/).