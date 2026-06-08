# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RetrolightGb is a Game Boy (DMG) emulator built with Kotlin Multiplatform and Compose Multiplatform, targeting Android, iOS, and JVM Desktop. The emulation core (CPU, PPU, APU, memory + MBCs) is fully shared in `commonMain`; only entry points, file picking, audio output, save persistence, and the current-time source are platform-specific.

## Build & Run Commands

```bash
# Android debug APK
./gradlew assembleDebug

# Run desktop (JVM)
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.eloi.retrolightgb.core.cpu.CpuInstructionTest"

# Clean
./gradlew clean
```

> **JDK note:** Gradle 8.13 rejects JDK 25. The daemon is pinned to **Azul 23** via `org.gradle.java.home` in the machine-local `~/.gradle/gradle.properties` (not checked in). `gradlew` itself resolves `JAVA_HOME` *before* reading that file, so `JAVA_HOME` must point at any valid JDK for the launcher to start.

## Architecture

### Source Sets

- `commonMain` — all emulation core and shared UI
- `androidMain` / `iosMain` / `jvmMain` — platform entry points + `actual` implementations of `FilePickerLauncher`, `AudioSink`, `SaveManager`, `CurrentTime`, and `argbToImageBitmap`
- `commonTest` — CPU instruction, interrupt, memory, and MBC tests

All emulation logic lives under `composeApp/src/commonMain/kotlin/com/eloi/retrolightgb/`.

### Emulation Core (`core/`)

Four singletons wired together via Kodein DI (`Apu` → `Memory` → `Cpu` / `Ppu`):

- **`Memory`** (`core/memory/`) — 64 KB flat address space. Owns the boot ROM (unmapped after boot), I/O register reads/writes, the DIV/TIMA timer (`tickTimer`), serial output (buffered into `serialOutput` state), and joypad state (`pressButton`/`releaseButton`). Cartridge access is delegated to an MBC. Forwards APU register writes (0xFF10–0xFF3F) to the injected `Apu`. ROM is loaded with `memory.load(rom)`, which first persists the current game's battery RAM.
- **`Cpu`** (`core/cpu/`) — Z80-like 8-bit CPU. Register state lives in `CpuRegisters`; instruction bodies are grouped into `ops/` (`AluOps`, `LoadOps`, `BitOps`, `ControlFlowOps`). Dispatch is via two `Map<UInt, () -> Unit>` tables (`instructions` and `cbInstructions`). `CpuTracer` records a ring buffer of recent instructions, dumpable with `dumpTrace()` on crash. Each step advances `pc` and records T-cycles (`t`); consumers read the delta via `t - lastT`.
- **`Ppu`** (`core/ppu/`) — Pixel Processing Unit ticked by the CPU's consumed cycles. Implements the four hardware modes (OAM=2, Transfer=3, HBlank=0, VBlank=1) with cycle-accurate timing. Renders BG, window, and sprites scanline-by-scanline into a **flat `IntArray` of color IDs (0..3)**. Double-buffered: scanlines render into `backBuffer`; at VBlank the back buffer is published as the `frameBuffer` Compose state and the buffers swap — no per-frame allocation.
- **`Apu`** (`core/apu/`) — Audio Processing Unit. Four channels (`PulseChannel` ×2 — ch1 has sweep, `WaveChannel`, `NoiseChannel`), a 512 Hz frame sequencer, and stereo mixing. Generates 44100 Hz / 16-bit stereo samples via a Bresenham sample timer, buffered and pushed to the platform `AudioSink`.

### Cartridges & Memory Bank Controllers (`core/memory/`)

`CartridgeFactory.create(rom)` inspects the cartridge type byte (0x0147) and returns the matching `Cartridge` implementation: `RomOnly`, `Mbc1`, `Mbc2`, `Mbc3` (with RTC), or `Mbc5`. `CartridgeFactory.hasBattery(rom)` decides whether RAM is persisted.

### Saves (`SaveManager`)

`expect object SaveManager { load(name); save(name, data) }`. Battery-backed RAM is keyed by the ROM's internal title (extracted from 0x0134–0x0143). Saved on ROM switch, on `Memory.save()`, and on window/activity teardown. `CurrentTime` is an `expect` providing the wall-clock used by MBC3 RTC.

### Emulation Loop (`GameBoy.kt`)

```
ROM picked → coroutine on Dispatchers.Default:
    apu.stop()                      // flush queued audio, unblock any pending write()
    previous job.cancelAndJoin()    // wait for old loop to fully stop before resetting shared state
    cpu.reset(); ppu.reset(); memory.load(rom); apu.reset(); apu.start()
    launch loop:
        while (true):
            cpu.step()
            cycles = cpu.t - cpu.lastT
            ppu.tick(cycles); memory.tickTimer(cycles); apu.tick(cycles)
            when a frame's worth of cycles (70_224) elapses:
                ensureActive()       // cooperative cancellation point
                sleep until the frame deadline (≈59.73 FPS pacing)
```

**ROM switching must happen off the UI thread and `cancelAndJoin` the old loop before touching `Cpu`/`Ppu`/`Memory`/`Apu`** — otherwise the old loop races the reset and keeps feeding the previous game's audio. See the desktop `AudioSink.stop()` using `flush()` (not `drain()`) so it doesn't block.

### UI Layer (`ui/`)

- **`GameBoyScreen.kt`** — `Canvas` that converts the PPU's color-ID `IntArray` to ARGB via `GameBoyPalette.argb` (precomputed LUT), builds an `ImageBitmap` (`argbToImageBitmap`, expect/actual), and draws it scaled with `FilterQuality.None`. One `drawImage` per frame (not 23 040 `drawRect`).
- **`ImageBitmapFactory.kt`** — `expect fun argbToImageBitmap(...)`. Android uses `Bitmap.createBitmap`; JVM/iOS use Skia `Image.makeRaster`.
- **`GameBoyPalette.kt`** — selectable DMG palettes; each exposes an `argb: IntArray` LUT indexed by color ID.
- **`mobile/`** — `GameBoyMobileBox` + `MobileJoyPad` overlay for touch input.
- **`SerialTerminal.kt`** — debug window showing serial output (Blargg test ROMs).
- **`FilePickerLauncher`** — `expect` interface; per-platform `actual` for ROM selection.

### Dependency Injection (`di/`)

Kodein. `commonModule.kt` binds `Apu`, `Memory`, `Cpu`, `Ppu` as singletons. `DIProvider.kt` exposes a `CompositionLocal`; composables retrieve instances with `rememberInstance<T>()`.

### Memory Map (key regions)

| Range | Purpose |
|---|---|
| 0x0000–0x00FF | Boot ROM (unmapped after boot) |
| 0x0100–0x7FFF | Cartridge ROM (banked via MBC) |
| 0x8000–0x9FFF | VRAM (tile data + tile maps) |
| 0xA000–0xBFFF | External cartridge RAM (banked / battery-backed) |
| 0xC000–0xDFFF | Work RAM |
| 0xFE00–0xFE9F | OAM (sprite attributes) |
| 0xFF00 | Joypad |
| 0xFF04–0xFF07 | DIV / TIMA / TMA / TAC (timer) |
| 0xFF10–0xFF3F | APU registers + Wave RAM |
| 0xFF40 | LCDC control register |
| 0xFF41 | STAT / PPU mode register |
| 0xFF44 / 0xFF45 | LY / LYC |
| 0xFF46 | OAM DMA (instantaneous) |
| 0xFF0F / 0xFFFF | IF / IE — interrupt flags |

### Platform Entry Points

- **Android**: `androidMain/.../MainActivity.kt` — `ComponentActivity`
- **Desktop**: `jvmMain/.../main.kt` — `application {}` window with a File/View `MenuBar` (Open ROM, palette selection, serial Terminal window) and keyboard joypad mapping (arrows + Z/X/Enter/Backspace)
- **iOS**: `iosMain/.../MainViewController.kt` — `UIViewController` wrapper