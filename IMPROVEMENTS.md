# RetrolightGb — Melhorias e Funcionalidades

Lista de melhorias do emulador, em ordem de impacto. Itens concluídos marcados com ✅.

> Atualizado em 2026-06-09.

---

## ✅ Concluídos

- **#1 — `println` no caminho quente** — removido o `println` de `Ppu.checkLyLycCoincidence` (rodava a cada coincidência LY=LYC). _(Sobrou um em `CartridgeFactory`, mas roda uma vez por carga de ROM, fora do loop.)_
- **#8 — Render via `ImageBitmap`** — `GameBoyScreen` agora faz um único `drawImage` por frame em vez de 23.040 `drawRect`.
- **#9 — Double-buffer na PPU** — dois `IntArray` planos alternados; zero alocação por frame (antes: `Array(144){ copyOf() }` a cada VBlank).
- **#10 — Dispatch da CPU por array** — tabelas `instructions`/`cbInstructions` migradas de `Map<UInt,()->Unit>` para `Array<(()->Unit)?>(256)` indexado direto (sem lookup de HashMap nem boxing de `UInt`).
- **#11 — Save states** — snapshot completo da máquina (CPU, Memory/MBC, PPU, APU) via Okio, com validação por título da ROM. Desktop (menu + F5/F8) e mobile (FABs); slot rápido 0. Testes de round-trip em `commonTest/SaveStateTest.kt`.
- **#19 — `CLAUDE.md` atualizado** — arquitetura documentada agora cobre APU, MBCs, RTC, saves e as mudanças de render/dispatch.
- _(bônus)_ **Bug de troca de ROM no desktop** — `drain()`→`flush()` no `AudioSink.jvm`, troca fora da UI thread com `cancelAndJoin` e `ensureActive`.

---

## 🐞 Correções de precisão (pendentes)

- **#2 — OAM DMA instantâneo** — `Memory` copia os 160 bytes imediatamente no write de `0xFF46`. No hardware leva ~160 M-cycles e bloqueia o barramento. Raro afetar jogos; vale ao menos documentar/agendar a cópia.
- **#3 — Quirks de Timer (TIMA)** — falta o atraso de 1 ciclo no reload do TIMA e o comportamento de borda de descida do DIV. Falha nos testes mooneye de timer.
- **#4 — HALT bug do DMG** — quando `IME=0` e há interrupção pendente, o DMG relê o opcode (PC não avança). Hoje só soma ciclos. Afeta alguns jogos/test ROMs.
- **#5 — `STOP` é no-op** — só avança o PC; não trata o reset do DIV (troca de velocidade é irrelevante em DMG).
- **#6 — Modo 3 da PPU fixo em 172 ciclos** — ignora penalidades de SCX e de sprites. Não é pixel-FIFO accurate; falha testes de timing mas funciona na maioria dos jogos comerciais.
- **#7 — Opcodes ilegais crasham** — lançam `NotImplementedError` em vez de travar como no hardware. Tratá-los como nop/lock evita derrubar a emulação por ROMs malformadas.

---

## ⚡ Performance (pendente)

- _(nenhum pendente — #8, #9, #10 concluídos)_

---

## ✨ Funcionalidades novas (pendentes)

- **#12 — Controles de emulação na UI** — pause/resume, reset e **fast-forward/turbo** (limitar o `delay` no loop de `GameBoy.kt`). Hoje só dá pra abrir ROM e save/load.
- **#13 — Suporte a Game Boy Color (CGB)** — hoje é DMG-only (paletas fixas). Maior salto de escopo; abre toda a biblioteca colorida.
- **#14 — Controle de áudio** — volume/mute. A APU sempre toca no volume cheio; não há UI pra isso.
- **#15 — Rebind de teclas + gamepad** — desktop tem mapeamento fixo. Permitir reconfigurar e suportar controle físico (e haptics no Android).
- **#16 — ROMs recentes / biblioteca** — lista de jogos abertos recentemente em vez de sempre abrir o file picker.
- **#17 — Screenshot e filtros de tela** — captura de imagem, ghosting de LCD e filtros de escala (extensão natural do sistema de paletas).
- **#18 — Cheats (Game Genie / GameShark)** — patch de memória/ROM.

### Extensões de itens já feitos
- **Save states — múltiplos slots** — a API já recebe `slot: Int`; falta UI para mais de um slot (hoje só o slot rápido 0).

---

## 📄 Manutenção (pendente)

- **#20 — Harness de test ROMs** — não há execução automatizada de Blargg/mooneye (validam timing e os quirks dos itens #3, #4, #6). O serial já captura saída — dá pra automatizar os Blargg facilmente.

---

## Sugestões de próximo passo

- **Feature visível:** #12 (pause/resume + fast-forward) — complementa bem o save state.
- **Precisão / test ROMs:** #3 (timer) e #4 (HALT bug), validáveis com #20.
- **Maior escopo:** #13 (Game Boy Color).
