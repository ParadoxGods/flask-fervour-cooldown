# Flask Fervour Cooldown

RuneLite external plugin for tracking the Leagues 6 Flask of Fervour cooldown by exact game tick.

Features:
- Reads RuneLite's `LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN` varp instead of guessing from clicks
- Decodes the packed `0x8000` cooldown flag so ready state does not display as `32k` ticks
- Shows exact cooldown ticks remaining in a movable RuneLite overlay panel
- Reduces the predicted cooldown immediately from your outgoing hitsplats: `damage / 10` ticks
- Draws cooldown tick text and shading over Flask of Fervour and the empty flask variant
- Highlights the flask when the cooldown reaches zero
- Includes a temporary raw varp/mode debug line to validate the client cooldown value during testing

Development:
- Requires a JDK. If Gradle finds a JRE first, set `JAVA_HOME` to a JDK before building.
- Run `./gradlew run` to launch RuneLite with the plugin loaded
- Run `./gradlew test` to execute the unit tests
