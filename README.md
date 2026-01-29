<div align="center">
  <h1>Mirage</h1>
  <p>
    A personal Paper fork focused on stable TPS and a comfy server experience.  
  </p>
</div>

## What is this?

**mirage** is my personal fork of Paper, built alongside my [YouTube channel](https://www.youtube.com/@fembytee)
and [Discord community](https://discord.com/invite/XYUU3GFsJz).

This isn't a project trying to compete with large, general-purpose server cores.  
It's a space to explore ideas, experiment freely, and ask unique questions about how Minecraft servers actually work.

## Building

```bash
# clone the repo
git clone https://github.com/dractical/mirage.git
cd mirage

# patches / build
./gradlew applyAllPatches
./gradlew createMojmapPaperclipJar
```

## Patch workflows

Feature patches are generated from commits in the nested git repos, not from uncommitted changes.
You must commit your changes in the relevant repo before rebuilding feature patches.

### Using scripts (recommended)

1) Make your changes in the appropriate repo:
    - Minecraft sources: `mirage-server/src/minecraft/java`
    - Paper server: `paper-server`
    - Paper API: `paper-api`
2) Commit inside that repo.
3) Rebuild feature patches:

```bash
# all areas (Minecraft + Paper server + Paper API)
scripts/rebuild-feature-patches.sh

# only Minecraft
scripts/rebuild-minecraft-feature-patches.sh

# only Paper server
scripts/rebuild-paper-feature-patches.sh

# only Paper API
scripts/rebuild-feature-patches.sh paper-api
```

### Manual (no scripts)

Minecraft feature patches:

```bash
cd mirage-server/src/minecraft/java
git add net/minecraft/server/MinecraftServer.java # replace with actual file(s)
git commit -m "whatever change"
cd ../../..
./gradlew :mirage-server:rebuildMinecraftFeaturePatches -x :mirage-server:rebuildMinecraftFilePatches
```

Paper server feature patches:

```bash
cd paper-server
git add .
git commit -m "whatever change"
cd ..
./gradlew :mirage-server:rebuildPaperServerFeaturePatches \
  -x :mirage-server:rebuildPaperServerFilePatches \
  -x :mirage-server:rebuildServerFilePatches
```

Paper API feature patches:

```bash
cd paper-api
git add .
git commit -m "whatever change"
cd ..
./gradlew :rebuildPaperApiFeaturePatches -x :rebuildPaperApiFilePatches
```
