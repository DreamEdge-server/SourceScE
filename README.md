# SourceScE

Fullscreen **screen effect** plugin for modern Paper servers: a colour‑tinted fullscreen overlay
with native title fade‑in / stay / fade‑out, optional centered title & subtitle text, movement
freeze, and full HUD hiding while the effect is showing.

It is a clean re‑implementation of the idea behind LoneDev's
[ScreenEffects](https://github.com/JavaPlugins/ScreenEffects), but backed by **CraftEngine** for the
overlay image (instead of ItemsAdder) and using **PacketEvents** + **BetterHud** for HUD hiding.

## How it works

- The fullscreen overlay is a white **CraftEngine image** (`sourcesce:fullscreen`) sent inside a
  vanilla **title**, so `fadein / stay / fadeout` are the client's native title fade. The white
  glyph is tinted to any colour in code. A transparent variant (`fullscreen_transparent`) is
  included too.
- The overlay image is wrapped in symmetric negative‑space `<shift>` so it stays a **centred
  background** (never pushed off‑screen) while the title text overlays it and the subtitle text sits
  below.
- While an effect shows, all HUD is hidden and restored afterwards:
  - **BetterHud** HUDs via `HudPlayer.setHudEnabled(false)` (reflection).
  - **Vanilla** HUD (hotbar / health / hunger / XP / sidebar) via a spectator game‑mode packet sent
    with **PacketEvents** (the real game mode is not changed) + a blank scoreboard.
- Both HUD‑hiding behaviours are toggleable in `config.yml`.

## Commands

```
/sourcesce <effect> <color> <fadein> <stay> <fadeout> <freeze|nofreeze> [target] [title] [subtitle]
/sourcesce stop [target]
```

- `effect` — `fullscreen` or `fullscreen_transparent`
- `color` — named (`RED`) or hex (`#770000`); tints the white overlay
- `fadein/stay/fadeout` — ticks (20 = 1s)
- `target` — `me`, `all`, or a player name
- `title` / `subtitle` — text lines; use `/_` for spaces (e.g. `Welcome/_home`)

Aliases: `/sce`, `/screeneffect`. Permissions: `sourcesce.use`, `sourcesce.others`.

## Dependencies

All are **soft** dependencies — the plugin still loads without them (features degrade gracefully):

- [CraftEngine](https://modrinth.com/plugin/craftengine) — required for the overlay image
- [PacketEvents](https://modrinth.com/plugin/packetevents) — hides the vanilla HUD
- [BetterHud](https://modrinth.com/plugin/betterhud) — hides BetterHud's own HUDs

On first run the plugin deploys its CraftEngine image config to `plugins/CraftEngine/resources/sourcesce/`;
run `/ce reload all` once afterwards.

## Building

```
./gradlew build
```

Produces `build/libs/SourceScE.jar`.

## Credits & licence

Concept and the `fullscreen*` textures come from LoneDev's ScreenEffects. Licensed under **GPL‑3.0**
(see `LICENSE`).
