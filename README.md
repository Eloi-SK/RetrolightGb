# RetrolightGb

A Game Boy emulator built with Kotlin Multiplatform and Compose Multiplatform, running natively on Android, iOS, and desktop (Windows, macOS, Linux) — all from a single shared codebase.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.8.2-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-API_24+-3DDC84?style=flat&logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-arm64_x64-000000?style=flat&logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-JVM-F80000?style=flat&logo=openjdk&logoColor=white)

---

## Features

- **Full CPU emulation** — Z80-like 8-bit CPU with 200+ instructions and cycle-accurate timing
- **PPU rendering** — Pixel Processing Unit with all 4 hardware modes (OAM, Transfer, HBlank, VBlank), targeting 59.7 FPS
- **APU audio** — 4-channel audio synthesis (2 pulse, 1 wave, 1 noise) with frame sequencer
- **MBC support** — ROM Only, MBC1, MBC2, MBC3 (with RTC), MBC5
- **Save states** — Battery-backed RAM persistence for supported cartridges
- **Color palettes** — 5 palettes including Classic DMG, BGB, Grayscale, Game Boy Pocket, and Game Boy Light
- **Serial terminal** — Debug viewer for in-game serial output
- **Cross-platform UI** — Native file pickers, on-screen joypad (mobile), hardware keyboard support (desktop)

---

## Platforms

| Platform | Input | Notes |
|---|---|---|
| Android | Touch joypad + hardware buttons | API 24+ |
| iOS | Touch joypad + hardware buttons | arm64 / x64 |
| Desktop | Keyboard | Windows, macOS, Linux via JVM |

---

## MBC Support

| Cartridge Type | ROM Banks | RAM / Extra | Example Games |
|---|---|---|---|
| ROM Only | 1 (32 KB) | — | Tetris |
| MBC1 | Up to 128 (2 MB) | Optional battery RAM | Super Mario Land |
| MBC2 | Up to 16 (256 KB) | Built-in 512×4-bit RAM | Kirby's Dream Land |
| MBC3 | Up to 128 (2 MB) | Optional RAM + RTC | Pokémon Gold/Silver |
| MBC5 | Up to 512 (8 MB) | Optional battery RAM | Pokémon Crystal |

---

## Getting Started

### Prerequisites

- Android Studio (Iguana or newer) or IntelliJ IDEA with the Kotlin Multiplatform plugin
- JDK 11+
- Xcode (iOS builds only)

### Clone

```bash
git clone https://github.com/Eloi-SK/RetrolightGb.git
cd RetrolightGb
```

### Build & Run

```bash
# Android debug APK
./gradlew assembleDebug

# Desktop (JVM)
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.eloi.retrolightgb.ComposeAppCommonTest"

# Clean
./gradlew clean
```

iOS: open the project in Xcode via the `iosApp/` directory and run from there.

---

## Architecture

The project follows a clean Kotlin Multiplatform layout:

```
composeApp/src/
├── commonMain/       — All emulation logic + shared UI
│   └── core/
│       ├── cpu/      — Z80 CPU + instruction tables
│       ├── memory/   — 64 KB address space + MBC implementations
│       ├── ppu/      — PPU, mode state machine, frame buffer
│       └── apu/      — 4-channel audio + frame sequencer
├── androidMain/      — MainActivity + Android file picker
├── iosMain/          — UIViewController wrapper + iOS file picker
└── jvmMain/          — Desktop entry point + debug terminal window
```

### Emulation Loop

```
ROM loaded into Memory
  → GameBoy.kt runs CPU.step() on Dispatchers.Default
      → decode opcode → execute → update T-cycles
      → Ppu.tick(cycles) after every step
          → PPU advances mode state machine
          → on VBlank: writes IntArray frame buffer → triggers Compose recompose
```

Timing is budgeted at **70,224 T-cycles per frame** (≈ 59.7 FPS), measured with `TimeSource.Monotonic`.

### Memory Map

| Range | Region |
|---|---|
| 0x0000–0x00FF | Boot ROM (switches out after boot) |
| 0x0100–0x7FFF | Cartridge ROM (switchable banks) |
| 0x8000–0x9FFF | VRAM (tile data + tile maps) |
| 0xA000–0xBFFF | Cartridge RAM (if present) |
| 0xC000–0xDFFF | Work RAM |
| 0xFF00–0xFFFF | I/O registers + HRAM |

### Tech Stack

| Library | Purpose |
|---|---|
| Kotlin Multiplatform 2.2.0 | Shared codebase across platforms |
| Compose Multiplatform 1.8.2 | Declarative UI on all targets |
| Kodein DI 7.25.0 | Dependency injection (Memory, CPU, PPU as singletons) |
| OkIo 3.16.0 | Cross-platform file I/O for ROM loading and saves |
| kotlinx-coroutines 1.10.2 | Emulation loop + async frame scheduling |

---

## Contributing

Contributions are welcome!

1. Fork the repository
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'feat: add ...'`
4. Push: `git push origin feature/your-feature`
5. Open a Pull Request

---

## License

This project is open source. See [LICENSE](LICENSE) for details.