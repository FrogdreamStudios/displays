# Media runtime

Runtime support for media sessions and system helpers needed by the media pipeline, but not part of the public domain
model.

## Contents

- `session/` — `MediaSessionManager`, `DefaultMediaSessionManager`, `DisplayMediaSession`: session runtime, translating
  `api` display / playback services into `MediaSession` views
- `system/` — `Processes`: subprocess plumbing shared by the external binaries the mod drives (`yt-dlp`, `ffmpeg`).
  OS/architecture detection itself lives in `util`'s `OsInfo`, since it has no session-runtime dependencies and is
  needed by modules that don't otherwise depend on `media:runtime`
- `security/` — `MediaHostGuard`: SSRF guard for client-supplied media URLs (public-address checks, redirect-chain
  walking)

## Boundaries

- May depend on `api` and `media`
- Must not pull Minecraft / `Fabric` / `NeoForge` / `Paper` classes
- Do not put stream resolving, `yt-dlp`, `FFmpeg` process management, or rendering here

## Why separate from media

`media` must stay a clean type module. `media:runtime` connects media sessions to display/playback services from `api`.
