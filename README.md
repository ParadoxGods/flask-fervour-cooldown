# Flask Fervour Cooldown

RuneLite external plugin for tracking the Leagues 6 Flask of Fervour cooldown by exact game tick.

Features:
- Reads RuneLite's `LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN` varp instead of guessing from clicks
- Shows exact cooldown ticks remaining in a movable RuneLite overlay panel
- Draws cooldown tick text and shading over Flask of Fervour and the empty flask variant
- Highlights the flask when the cooldown reaches zero

Development:
- Requires a JDK. If Gradle finds a JRE first, set `JAVA_HOME` to a JDK before building.
- Run `./gradlew run` to launch RuneLite with the plugin loaded
- Run `./gradlew test` to execute the unit tests
