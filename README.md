# Chestify

Chestify is a Fabric mod for Minecraft 26.1.2 that adds a compact storage index to inventory and container screens.

When a chest has an item frame attached to it with an item inside, Chestify shows that item as an icon in a paginated grid. Clicking an icon opens the matching nearby marked chest through a server-validated request.

## Features

- Shows nearby item-frame-marked chests in inventory and container screens.
- Sorts icons using the creative-mode item order.
- Supports search by localized item name and registry id.
- Right-clicking a marked item frame opens the attached chest.
- Shift + right-click keeps the vanilla item frame rotation behavior.
- Includes a configurable search radius, controlled by the singleplayer owner or server operators.

## Multiplayer

Install the same jar on both the client and the server. The client handles the UI and scanning, while the server validates and opens marked chests.

## Building

This project currently builds against the official-named Minecraft jar from a local Fabulously Optimized 26.1.2 installation. Copy `gradle.properties.example` to `gradle.properties` and adjust the local paths before running:

```sh
./gradlew build
```

## License

MIT
