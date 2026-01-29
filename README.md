<div align="center">
  <h1>Mirage</h1>
  <p>
    A personal Paper fork focused on stable TPS and a comfy server experience.  
  </p>
</div>

## What is this?

**mirage** is my personal fork of Paper, built alongside my [YouTube channel](https://www.youtube.com/@fembytee) and [Discord community](https://discord.com/invite/XYUU3GFsJz).

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