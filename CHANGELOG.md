# Changelog

## 1.1

- Fix: Implementation for [Speed limits by vehicle type, 2. Always the lowest speed is the applicable one](https://wiki.openstreetmap.org/wiki/Default_speed_limits#Speed_limits_by_vehicle_type) was incomplete - not all `maxspeed` tags with higher speed were removed
- Detect circular references to placeholders in road type filters at initialization in order to avoid endless loops during runtime
 
## 1.0

Initial version