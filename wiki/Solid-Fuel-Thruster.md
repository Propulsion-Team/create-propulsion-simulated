# Solid Fuel Thruster

The **Solid Fuel Thruster** is a compact 1×1 thruster that burns **items** instead of fluids. Default tuning is weaker than liquid thrusters (lower base thrust and smaller, slower exhaust particles).

## Block behavior

### Fuel input

- Accepts items from the **back** of the block (the `facing` side — same side as the funnel on the model).
- Right-click with valid fuel to insert one item; right-click empty-handed to remove the stored item (only while not burning).
- Compatible with Create **funnels**, **hoppers**, chests, and other item handlers on that face.
- **Wrench** the block to open the fuel hatch. While open, the thruster pulls valid fuel from item containers placed behind the hatch (same face as the funnel).
- Internal storage holds **one** fuel item. The item remains in the slot for the whole burn.

### Accepted fuels

Valid fuels come from [solid thruster fuel](#solid-thruster-fuels) datapacks and KubeJS, plus:

- **Create blaze cake** — uses Create’s superheated blaze-burner burn time; **2×** thrust multiplier while burning.
- **Create creative blaze cake** — infinite burn duration.
- Any other item listed in Create’s `SUPERHEATED_BLAZE_BURNER_FUELS` datamap (if not overridden by a solid thruster fuel entry).

### Burn and thrust

- Fuel is armed when inserted (burn duration is calculated from the fuel definition or blaze-burner data).
- The burn timer **only decreases while the thruster is powered on** (redstone signal present). Power off pauses consumption; thrust and exhaust particles also require power.
- When the burn finishes, the fuel item is consumed (except infinite fuels) and a new item can be inserted.
- Redstone is **on/off only**: any strength ≥ 1 is full throttle; no signal is off. Partial redstone levels do not scale thrust. ComputerCraft normalized throttle remains precise and overrides this binary redstone behavior while under peripheral control.

### Crafting

```
C I C
A B A
I P I
```

- `C` — copper sheet (top left & right)
- `I` — iron sheet (top center, bottom left & right)
- `A` — andesite alloy (middle left & right)
- `B` — chute (center)
- `P` — fluid pipe (bottom center; exhaust / output)

## Config

Section: **`solidFuelThruster`** in `createpropulsion-common.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `solidFuelThrusterBaseThrust` | `200` | Base thrust at full power and full obstruction clearance |
| `solidFuelThrusterNozzleOffset` | `0.75` | Force application offset from block center |
| `solidFuelThrusterParticleCountMultiplier` | `0.35` | Scales exhaust particle count |
| `solidFuelThrusterParticleVelocityMultiplier` | `0.4` | Scales exhaust particle speed |

Obstruction scan, atmospheric pressure, and thrust unit display use the global **thruster** and **physics** config sections.

---

## Solid thruster fuels

Fuel definitions live under:

`data/<namespace>/solid_thruster_fuels/<path>.json`

Only the **Solid Fuel Thruster** reads these files. Liquid thrusters use [`thruster_fuels`](Datapack-Example-Usage.md).

### Resolution rules

- Only declared entries are valid fuels.
- No automatic furnace-fuel or coal-only fallback.
- Precedence:
  1. Removed fuel ids (KubeJS `removeFuel`)
  2. KubeJS scripted / override entries
  3. Datapack entries (items and tags)

Tag entries apply to every item in that tag that is not already defined by a specific `item` entry.

### JSON schema

Each file must define **exactly one** of `item` or `item_tag`.

```json
{
  "item": "minecraft:coal",
  "thrust_multiplier": 1.0,
  "consumption_multiplier": 1.0,
  "burn_ticks": 1600,
  "particle": "plume",
  "override_textures": [
    "createpropulsion:plume_0",
    "createpropulsion:plume_1"
  ],
  "override_color": 6750207,
  "use_item_color": false,
  "required_mod": "optional_mod_id"
}
```

Tag example:

```json
{
  "item_tag": "minecraft:planks",
  "thrust_multiplier": 0.3,
  "consumption_multiplier": 1.0,
  "particle": "plume"
}
```

### Fields

| Field | Required | Description |
|-------|----------|-------------|
| `item` | One of `item` / `item_tag` | Item id (e.g. `minecraft:coal`) |
| `item_tag` | One of `item` / `item_tag` | Item tag id (e.g. `minecraft:logs`) |
| `thrust_multiplier` | Yes | Thrust scaling for this fuel |
| `consumption_multiplier` | Yes | Burn speed scaling. Higher = **faster** burn (shorter duration). `1.0` = normal smelting burn time |
| `burn_ticks` | No | Fixed burn length in ticks. If omitted, uses the item’s smelting fuel time ÷ `consumption_multiplier` |
| `particle` | No | `plume`, `plasma`, or `none` (default: `plume`) |
| `override_textures` | No | Particle atlas texture ids |
| `override_color` | No | RGB color as decimal int (`6750207` = `0x66CCFF`) |
| `use_item_color` | No | Reserved for per-item tinting (prefer `override_color` in datapacks) |
| `required_mod` | No | Entry loads only if that mod id is loaded |

### Burn time

- Default: `item.getBurnTime(SMELTING) / consumption_multiplier` (minimum 1 tick), unless `burn_ticks` is set.
- **Coal / charcoal** use explicit longer burns (`coal` 4000 ticks, `charcoal` 3600 ticks). Blocks use `burn_ticks: 16000`.
- **Logs and planks** use normal smelting burn time at `consumption_multiplier: 1.0`.
- **Other wood** (stairs, slabs, fences, trapdoors, doors) use `consumption_multiplier: 8.0` for a much shorter burn than logs/planks.

### Particle types

| Value | Effect |
|-------|--------|
| `plume` | Standard exhaust (default) |
| `plasma` | Plasma-style exhaust |
| `none` | No particles for this fuel |

### Default pack (`createpropulsion`)

**`data/createpropulsion/solid_thruster_fuels/minecraft/`**

| Type | Entries |
|------|---------|
| Items | `coal`, `coal_block`, `charcoal`, `charcoal_block` |
| Tags | `logs`, `planks`, `wooden_stairs`, `wooden_slabs`, `wooden_fences`, `wooden_trapdoors`, `wooden_doors` |

Wood fuels use lower `thrust_multiplier` values (~0.25–0.35) than coal. Logs and planks use a normal smelting-length burn; other wooden parts burn ~8× faster via `consumption_multiplier`.

**`data/createpropulsion/solid_thruster_fuels/create/`** (requires Create)

| Item | Notes |
|------|--------|
| `create:blaze_cake` | `thrust_multiplier` 1.5, plasma particles; also receives superheated burn time and 2× thrust while burning |
| `create:creative_blaze_cake` | `thrust_multiplier` 2.0, `burn_ticks` 51840000 (infinite), plasma particles |

### Custom datapack example

`data/my_pack/solid_thruster_fuels/blaze_rod.json`

```json
{
  "item": "minecraft:blaze_rod",
  "thrust_multiplier": 1.2,
  "consumption_multiplier": 0.8,
  "particle": "plasma",
  "override_color": 16744448,
  "required_mod": "minecraft"
}
```

### Tag example (mod wood)

`data/my_pack/solid_thruster_fuels/c_wood.json`

```json
{
  "item_tag": "c:wooden_logs",
  "thrust_multiplier": 0.4,
  "consumption_multiplier": 1.0
}
```

### Reload

Run `/reload` to re-read datapack files. Fuel data is synced to clients automatically.

---

## KubeJS

Global: **`SolidThrusterFuelManager`**

Method names match [`ThrusterFuelManager`](KubeJS-API.md), but fuel ids are **items**, not fluids.

```js
ServerEvents.loaded(event => {
  SolidThrusterFuelManager.registerScriptedFuel('minecraft:blaze_rod', {
    thrust_multiplier: 1.2,
    consumption_multiplier: 0.8,
    particle: 'plasma',
    override_color: 0xFF8000
  })

  SolidThrusterFuelManager.registerScriptedFuel('minecraft:bamboo', {
    thrust_multiplier: 0.2,
    consumption_multiplier: 1.5,
    particle: 'plume'
  })

  // SolidThrusterFuelManager.removeFuel('minecraft:coal')
})
```

See [KubeJS API — Solid Fuel Thruster](KubeJS-API.md#solid-fuel-thruster-solidthrusterfuelmanager) for the full method list.

---

## ComputerCraft

With **CC: Tweaked**, peripheral type **`solid_fuel_thruster`**.

- Throttle: `setPower` / `setPowerNormalized` / `getPower`; normalized values provide precise `0.0 … 1.0` control and override redstone
- Thrust: `getCurrentThrustPN`, `getCurrentThrustKN`, `getDisplayedThrustPN`, `getDisplayedThrustKN`, `getAirflowMs`, `getObstruction`
- Fuel: `getFuelAmount` (`0` or `1`), `getFuelCapacity` (`1`), `getBurnTimeRemaining`, `isBurning`
- Items: `list`, `pushItems`, `pullItems` on the back-side fuel slot

Full method list: [ComputerCraft peripherals — Solid fuel thruster](ComputerCraft-Peripherals.md#solid-fuel-thruster-solid_fuel_thruster).

---

## Related docs

- [Liquid thruster fuels (datapack)](Datapack-Example-Usage.md)
- [KubeJS API](KubeJS-API.md)
- [ComputerCraft peripherals](ComputerCraft-Peripherals.md#solid-fuel-thruster-solid_fuel_thruster)
