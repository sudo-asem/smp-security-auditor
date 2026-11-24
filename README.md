# SMP Security Auditor (Fabric 26.1.2)

A passive client-side reconnaissance mod. When the server synchronizes its Brigadier command tree to your client, the mod exports the command nodes the server sent to your player.

## What it does

- Captures the command tree received by the client after login / command-tree synchronization.
- Writes `smp-audit/commands.txt` and `smp-audit/commands.json` in the active Prism Launcher instance directory.
- Flags command names that look security-sensitive for manual review.
- Does not send commands, attempt logins, modify permissions, scan ports, exploit plugins, or change server state.

**Important:** a command appearing in the client command tree does not prove it can be executed. The server may still reject it when execution is attempted. Conversely, commands omitted from tab completion may still have alternate aliases or plugin-specific entry points. This export is reconnaissance, not a vulnerability verdict.

## Build

Minecraft 26.1.2 requires Java 25. Install a Java 25 JDK and use Gradle 9.4.0 or newer compatible with Fabric Loom 1.15.

From this folder run:

```powershell
gradle build
```

The jar will be created under `build/libs/`.

## Install in Prism Launcher

1. Build the jar.
2. In Prism Launcher, right-click the Minecraft 26.1.2 Fabric instance and choose **Folder**.
3. Open or create the `mods` folder.
4. Copy the built `smp-security-auditor-1.0.0.jar` into `mods`.
5. Launch the instance and join the server you are authorized to assess.
6. After the command tree is received, open the instance folder and read `smp-audit/commands.txt` or `commands.json`.

The mod is client-only. Do not install it on the server.
