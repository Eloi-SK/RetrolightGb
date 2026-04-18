# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RetrolightGb is a Game Boy emulator built with Kotlin Multiplatform and Compose Multiplatform, targeting Android, iOS, and JVM Desktop.

## Build & Run Commands

```bash
# Android debug APK
./gradlew assembleDebug

# Run desktop (JVM)
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.eloi.retrolightgb.ComposeAppCommonTest"

# Clean
./gradlew clean
```

## Architecture

### Source Sets

- `commonMain` — all emulation core and shared UI
- `androidMain` / `iosMain` / `jvmMain` — platform-specific entry points and file picker implementations

All emulation logic lives under `composeApp/src/commonMain/kotlin/com/eloi/retrolightgb/`.

### Emulation Core (`core/`)

Three singletons wired together via Kodein DI:

- **`Memory`** — 64 KB flat address space. Handles boot ROM switchout, I/O register reads/writes, and interrupt flags. ROM is loaded here with `memory.load()`.
- **`Cpu`** — Z80-like 8-bit CPU. Instruction dispatch via two `Map<Int, () -> Unit>` tables (`instructions` and `cbInstructions`). Each instruction mutates registers, advances `pc`, and records T-cycles (`t`) and M-cycles (`m`).
- **`Ppu`** — Pixel Processing Unit ticked by the CPU's consumed cycles. Implements four hardware modes (OAM=2, Transfer=3, HBlank=0, VBlank=1) with cycle-accurate timing. Renders tiles from VRAM into an `IntArray` frame buffer, then fires a Compose state update.

### Emulation Loop

```
ROM loaded into Memory
    → CPU.step() loop on Dispatchers.Default coroutine
        → each step: decode opcode → execute → update cycles
        → Ppu.tick(cycles) after every CPU step
            → PPU advances mode state machine
            → on VBlank: writes frame buffer → triggers VBlank interrupt
```

### UI Layer (`ui/`)

- **`GameBoyScreen.kt`** — `Canvas` composable that draws the `160×144` frame buffer scaled 4× using `drawIntoCanvas`.
- **`FilePickerLauncher.kt`** — `expect` interface; `actual` implementations per platform in each source set.
- State is managed with Compose `mutableStateOf`; PPU writes directly to the state variable so the screen recomposes on each new frame.

### Dependency Injection (`di/`)

Kodein is used. `commonModule.kt` binds Memory, CPU, and PPU as singletons. `DIProvider.kt` exposes a `CompositionLocal`; composables retrieve instances with `rememberInstance<T>()`.

### Memory Map (key regions)

| Range | Purpose |
|---|---|
| 0x0000–0x00FF | Boot ROM (unmapped after boot) |
| 0x0100–0x7FFF | Cartridge ROM |
| 0x8000–0x9FFF | VRAM (tile data + tile maps) |
| 0xC000–0xDFFF | Work RAM |
| 0xFF40 | LCDC control register |
| 0xFF41 | STAT / PPU mode register |
| 0xFF44 | LY — current scanline |
| 0xFF0F / 0xFFFF | IF / IE — interrupt flags |

### Platform Entry Points

- **Android**: `androidMain/.../MainActivity.kt` — `ComponentActivity`
- **Desktop**: `jvmMain/.../main.kt` — `application {}` window; also opens a memory debug window
- **iOS**: `iosMain/.../MainViewController.kt` — `UIViewController` wrapper
