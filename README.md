# Tick Cooldown Tracker

RuneLite external plugin for tracking configurable OSRS item cooldowns by game tick.

Features:
- Starts a cooldown when a configured inventory or equipment item action is clicked
- Shows exact ticks remaining in a movable RuneLite overlay panel
- Draws cooldown text and shading over matching inventory/equipment items
- Highlights the item when the cooldown reaches zero

Development:
- Requires a JDK. If Gradle finds a JRE first, set `JAVA_HOME` to a JDK before building.
- Run `./gradlew run` to launch RuneLite with the plugin loaded
- Run `./gradlew test` to execute the unit tests

Configuration:
- Add cooldown definitions as `item id or exact item name: ticks`
- Separate entries with new lines, commas, or semicolons
- Example: `8013: 100, Dragon dagger: 4`
