# Audio/Video Sync Issue — ZalithLauncher (DreamDisplays mod)

## Device
- Samsung SM-X810 (Galaxy Tab S9 FE), Android 14 (API 34), Adreno 740
- ZalithLauncher v2 running Minecraft 26.2 Fabric 0.19.3 (OpenJDK guest JVM)

---

## The Core Problem

DreamDisplays plays video (YouTube etc.) inside Minecraft by running two FFmpeg subprocesses (one for video frames, one for audio PCM) and rendering video on a block texture while routing audio to the device speaker. On ZalithLauncher, both the mod and FFmpeg run inside an **OpenJDK guest JVM**, not Android's ART. This makes all normal Android audio/video APIs unreachable.

---

## Problems Encountered & Fixes Applied

### 1. `android.media.AudioTrack` — ClassNotFoundException ❌→✅ Fixed
**Problem**: The original code used `android.media.AudioTrack` via reflection. Android framework classes are in ART, not accessible from OpenJDK's classloader hierarchy.  
**Fix**: Created `AudioTrackWrapper` with a `ReflectedTrack` (primary) + `OpenALTrack` (fallback) backend. OpenAL via LWJGL is guaranteed present since Minecraft uses it for 3D audio.

### 2. `javax.sound.sampled` — No native backing ❌→✅ Fixed
**Problem**: Tried `SourceDataLine` as fallback. ZalithLauncher's OpenJDK bundles `javax.sound.sampled` class stubs but not the native `jsound` library, so `AudioSystem.getSourceDataLine()` threw `No line matching interface SourceDataLine`.  
**Fix**: Replaced `javax.sound.sampled` backend with `OpenALTrack` using LWJGL's `org.lwjgl.openal`.

### 3. Minecraft Sound Engine Corruption — `AL_INVALID_NAME` ❌→✅ Fixed
**Problem**: `alcMakeContextCurrent()` is process-wide. Calling it on the MediaPlayer-audio thread replaced Minecraft's active OpenAL context, causing Minecraft's sound engine to get `AL_INVALID_NAME` on every AL call.  
**Fix**: Used `EXTThreadLocalContext.alcSetThreadContext()` (thread-local context) instead. Each thread has its own current context; Minecraft's is untouched.

### 4. VAAPI Hardware Decode Failure ❌→✅ Fixed
**Problem**: `HwAccelBackend.detectDefault()` returned `VAAPI` for `isLinux`. ZalithLauncher sets `os.name=Linux`, so Android got VAAPI. VAAPI doesn't exist on Android (requires DRM/KMS drivers). Every video start logged `Device creation failed: -12`.  
**Fix**: Added `MEDIACODEC` backend; `detectDefault()` now checks `os.version` for `"Android"` first and returns `MEDIACODEC`. MediaCodec also fails on this device's PojavLauncher FFmpeg build (same -12 error, likely not compiled with MediaCodec NDK support) but the fallback detection now recognises "mediacodec" failure markers.  
**Current status**: Both VAAPI and MediaCodec fail → falls back to software H.264 decode.

### 5. Audio Clock Freezing → Massive Resync Cascade ❌→✅ Fixed
**Problem**: `sampleClock()` is called from the `MediaPlayer-prebuffer` thread. That thread never had `alcSetThreadContext()` called, so `alGetSourcei(source, AL_SAMPLE_OFFSET)` returned 0. The clock appeared frozen → stall watchdog fired → `applyPendingResync` was called repeatedly, skipping up to 17,295ms of audio → silence.  
**Fix**: Added `ensureContext()` call inside `playbackHeadPosition` getter. Now correct regardless of calling thread.

### 6. OpenAL Buffer Size — CPU Starvation (Video Lag) ❌→✅ Fixed
**Problem**: `bufferBytes = LINE_BUFFER_BYTES = 70,560 bytes` (400ms per OpenAL buffer). `acquireFreeBuffer()` polled every 1ms waiting for one buffer to drain — ~400 JNI calls per buffer — stealing CPU from software H.264 decode. Result: `190/240 frames dropped, worst lateness 350ms`.  
**Fix**: Override `bufferBytes = sampleRate / 20 * BYTES_PER_FRAME = 8,820 bytes` (50ms per buffer). Poll loop now spins ~50 times instead of ~400 per buffer.

### 7. `bufferSizeInFrames` Semantic Mismatch ❌→✅ Fixed
**Problem**: `AudioSink.paceLiveWrite()` computes unplayed frames as `bufferSizeInFrames - playbackHeadPosition`. For OpenAL, `playbackHeadPosition` grows monotonically (total played), but `bufferSizeInFrames` was returning a fixed capacity. Result: difference always 0 → pacer locked at MAX_PACE_BYTES, underrun detector fired every chunk.  
**Fix**: `bufferSizeInFrames` now returns `framesSubmitted` (total frames ever submitted). `framesSubmitted - playbackHeadPosition` correctly gives unplayed frame count.

### 8. Pending-Processed Buffers in Clock Calculation ❌→✅ Fixed
**Problem**: When OpenAL source ran out of buffers and stopped (AL_STOPPED), `AL_SAMPLE_OFFSET` reset to 0. But `framesFromDoneBuffers` hadn't been updated yet for processed-but-not-yet-unqueued buffers. Clock appeared frozen for up to `NUM_BUFFERS × 50ms`.  
**Fix**: `playbackHeadPosition` now includes `AL_BUFFERS_PROCESSED * framesPerBuffer` in the calculation, giving the correct position even when the source is stopped.

### 9. Double-Release Context Leak ❌→✅ Partially Fixed
**Problem**: `AudioSink.stop()` (external thread) and the pump thread's cleanup both call `release()`. The first call destroys the OpenAL context. The pump thread exits with its thread-local context still set → OpenAL Soft logs `"leak detected"` for every seek/session change.  
**Fix**: Added `AtomicBoolean released` guard — losing thread always calls `alcSetThreadContext(0L)` without re-destroying. **Still leaking on JVM shutdown** (daemon threads killed before cleanup).

### 10. Resync Cascade — Audio Out of Sync ❌→✅ Fixed (this session)
**Problem**: `Audio clock stuck for ~753ms` → resync fires once. But stale audio in OpenAL's 200ms buffer keeps playing at old position while `contentStartNanos` has jumped forward → immediately looks out of sync again → resync fires again every ~1s for 20+ seconds → total 3–4 seconds of audio skipped → permanent A/V offset.  
**Root cause of stall**: FFmpeg software H.264 decode occasionally takes 750ms for a complex GOP. OpenAL's 4 × 50ms = 200ms queue drains during this block. Source stops. Clock freezes.  
**Fix 1**: `NUM_BUFFERS = 16` (800ms queue > 750ms decode stall) — source should never run dry.  
**Fix 2**: Resync cooldown (3s minimum between resyncs) — cascade can't repeat.  
**Fix 3**: Flush OpenAL line on resync — stale buffered audio stops immediately, preventing overlap. Head tracking (`lastRawHead`, `headWraps`) reset before flush.

---

## Current Status

| Symptom | Status |
|---|---|
| Audio plays | ✅ Working (OpenAL via LWJGL) |
| Minecraft sound engine intact | ✅ Working (thread-local context) |
| Audio clock stability | ✅ Much improved; stall eliminated by 800ms buffer |
| Video frame drops | ⚠️ Software decode at 1360×720 is borderline; expected some drops |
| Hardware decode | ❌ Neither VAAPI nor MediaCodec works on this FFmpeg build |
| Audio/video sync (steady state) | ⚠️ Should be correct; resync cascade no longer corrupts it |
| ALSOFT context leak on JVM exit | ⚠️ Expected — daemon threads killed without cleanup |

---

## Remaining Unknown: Steady-State A/V Offset

The user reports audio and video sometimes not in sync even when both are running smoothly. Possible causes:
1. **CatchUp over/under-skip**: When hardware→software decode fallback happens, both processes restart. The `CatchUp` mechanism discards leading audio to match the current playback position. If the skip is slightly off, a fixed A/V offset remains.
2. **Session restart timing**: The audio process and video process start at slightly different times. The gap between their actual decode starts introduces an offset.
3. **Resync history corrupting `contentStartNanos`**: Each resync advances the audio clock origin. If total resyncs over-advanced it, audio appears ahead of video permanently.

**Next steps to diagnose**:
- Add DEBUG logging to print `contentStartNanos`, total resync skipped, and first videoPts at session start to compare expected vs actual offsets.
- Test with a short (30s) clip that doesn't stress decode, and check if sync is still off without any stall/resync.

---

## File Map

| File | Role |
|---|---|
| `pipeline/AudioTrackWrapper.kt` | OpenAL streaming backend; all fixes for clock, context, buffer management |
| `pipeline/AudioSink.kt` | PCM pump, audio master clock, resync logic |
| `process/HwAccelBackend.kt` | Hardware decode backend selection; Android detection |
| `process/MediaProcess.kt` | FFmpeg command builder |
| `process/FFmpegBinary.kt` | FFmpeg binary resolver (plugin APK path via `POJAV_FFMPEG_PATH`) |
| `pipeline/VideoFramePipe.kt` | Video frame reader (PPM parser) |
| `pipeline/FramePrebuffer.kt` | A/V pacing, stall detection, resync triggers |

---

## Key Constants (AudioTrackWrapper / AudioSink)

| Constant | Value | Meaning |
|---|---|---|
| `NUM_BUFFERS` | 16 | OpenAL buffers; 16 × 50ms = 800ms queue depth |
| `bufferBytes` | `sampleRate/20 * 4` = 8820 | 50ms per OpenAL buffer |
| `CHUNK_BYTES` | 8820 | 50ms per read/write cycle |
| `LINE_BUFFER_BYTES` | 70560 | 400ms; pacer ceiling |
| `MAX_PACE_BYTES` | 52920 | 300ms; max pacing target |
| `RESYNC_COOLDOWN_NANOS` | 3,000,000,000 | 3s minimum between resyncs |
