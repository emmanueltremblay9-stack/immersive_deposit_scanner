# JourneyMap Integration

JourneyMap is a required dependency. The API is used as `compileOnly` and is not shaded into the final jar.

Client-only JourneyMap classes are isolated under:

`com.oblixorprime.immersivedepositscanner.client.journeymap`

The plugin class implements `journeymap.api.v2.client.IClientPlugin` and is annotated with `@JourneyMapPlugin(apiVersion = "2.0.0")`.

Markers and overlays are runtime-owned by `immersive_deposit_scanner`. Full sync removes only entries created by this mod before rebuilding current entries.

