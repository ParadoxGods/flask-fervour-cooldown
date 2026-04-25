# Flask Fervour Cooldown

RuneLite external plugin for tracking the Leagues 6 Flask of Fervour cooldown by exact game tick.

Features:
- Starts a 300 tick cooldown when you activate Flask of Fervour
- Calibrates from the in-game cooldown message when the flask reports remaining seconds
- Shows exact cooldown ticks remaining in a movable RuneLite overlay panel
- Reduces the predicted cooldown immediately from your outgoing hitsplats: `damage / 10` ticks
- Draws cooldown tick text and shading over Flask of Fervour and the empty flask variant
- Highlights the flask when the cooldown reaches zero

Development:
- Requires a JDK. If Gradle finds a JRE first, set `JAVA_HOME` to a JDK before building.
- Run `./gradlew run` to launch RuneLite with the plugin loaded
- Run `./gradlew test` to execute the unit tests
