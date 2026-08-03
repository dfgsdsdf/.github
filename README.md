# Server Bound Resource Pack

Client-side Fabric mod for Minecraft Java Edition 26.2. The mod embeds a built-in resource pack at `resourcepacks/meowy_pack` and enables it automatically only while connected to these servers:

- `45.93.200.38:25585`
- `play.meowy.ru`
- `meow.hobaboba.ru`
- `m.minecraft.in.net`
- `ihave.13cm.online`
- `meow.ru-mc.ru`
- `51.38.155.200:29420`

On disconnect, or on any other server, the resource pack is removed from the active pack list and resources are reloaded.

## Build

Use Java 25+ and Gradle 9.2+ / 9.5.1+:

```bash
gradle build
```

The mod JAR will be produced in `build/libs/`.
