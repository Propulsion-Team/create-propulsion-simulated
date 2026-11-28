# ComputerCraft peripherals API

Create Propulsion registers these peripherals when **CC: Tweaked** is installed. Peripheral types match Create’s `SyncedPeripheral` pattern (`mainThread` Lua calls run on the server).

**Entry point:** see also [ComputerCraft overview](cc/README.md).

To control every thruster on a ship at once without modems, to read the ship's position, motion and orientation, or to use redstone links from Lua, see the [ComputerCraft ship APIs](ComputerCraft-Ship-API.md).

---

## Throttle control (all thrusters)

All thrust readouts and thrust-derived physics use the shared `thrustUnitsPerPn` value in `thrusterGeneral` of the common config (default `1000`) so displayed values match assembled/physics behavior.

While **any** computer is attached to a thruster peripheral:

1. The block uses **peripheral throttle** (normalized `0.0 … 1.0`), driven by Lua (`setPower`, `setThrust`, etc.).
2. **`getPower()`** returns that peripheral throttle. Normalized commands retain floating-point precision and are not rounded to one of the 16 redstone levels.
3. World **redstone signals next to the block do not drive thrust** until the computer disconnects.

When the computer **detaches**, peripheral throttle is cleared and the thruster returns to **normal** redstone-controlled behavior.

### Precise thrust and obstruction (all thrusters except RCS)

All `…PN` methods use the same pN value shown in the goggles tooltip and on the creative thruster scroll. There are no kN methods.

Every thruster peripheral also has:

| Method | Returns | Notes |
|--------|---------|--------|
| `setThrustPN(thrust)` | — | Sets throttle to `thrust / getMaxThrustPN()`, in the same pN as the goggles tooltip and on-block scroll. Clamped to `0 … max`. Overrides redstone like `setPower`; a later `setPower` or `setPowerNormalized` replaces it |
| `getMaxThrustPN()` | `number` | Nominal thrust at full throttle for the current fuel, before obstruction and atmosphere |
| `getObstructionEfficiency()` | `number` | `0.0 … 1.0` thrust fraction left after obstruction. `getObstruction()` gives the unobstructed block count |

The thrust is stored as a throttle fraction, so it changes if `getMaxThrustPN()` changes (for example after a fuel swap). Multiblock thrusters can be controlled through any block of the cube, and the values describe the whole cube.

### Vector core (all thrusters, including RCS)

A disabled Vector Core stays installed, but thrust is no longer applied at the center of mass. A held Vector Core can also be toggled with Shift + Right-click. The state is kept when the core is installed or removed.

| Method | Returns | Notes |
|--------|---------|--------|
| `toggleVectorCore()` | `boolean` | Toggles the installed core. Returns the new state, `false` if no core is installed or it is depleted |
| `setVectorCoreEnabled(enabled)` | `boolean` | Returns `false` if no core is installed or it is depleted |
| `isVectorCoreEnabled()` | `boolean` | `true` only if a core is installed, enabled and above 1 durability |

Fluid thrusters, solid fuel thrusters, and ion thrusters use **different peripheral type strings** (`thruster`, `solid_fuel_thruster`, `ion_thruster`) so scripts can tell them apart.

---

## Thruster (`thruster`)

**Fuel / chemical thrusters only** (not ion).

| Method | Returns | Notes |
|--------|---------|--------|
| `getObstruction()` | `number` | Returns unobstructed-block count from the obstruction scan |
| `setPower(redstonePower)` | — | `redstonePower`: `0 … 15`; sets peripheral throttle |
| `setPowerNormalized(power)` | — | Precise normalized `power`: `0.0 … 1.0`; values are clamped but not quantized to redstone levels |
| `getPower()` | `number` | Normalized throttle `0.0 … 1.0` |
| `getCurrentThrustPN()` | `number` | |
| `getAirflowMs()` | `number` | |
| `getFuelAmountMb()` | `number` | |
| `getFuelCapacityMb()` | `number` | |
| `tanks()` | `table` | |
| `pushFluid(toName[, limit[, fluidName]])` | `number` | |
| `pullFluid(fromName[, limit[, fluidName]])` | `number` | |

---

## Solid fuel thruster (`solid_fuel_thruster`)

**Item-fuel** thrusters: same throttle and thrust readouts as [Thruster](#thruster-thruster), but **no fluid API**. One fuel item in a single slot on the back face. Item I/O uses generic inventory methods on that face.

| Method | Returns | Notes |
|--------|---------|--------|
| `setPower` / `setPowerNormalized` | — | Same throttle semantics as [Throttle control](#throttle-control-all-thrusters) |
| `getPower()` | `number` | |
| `getObstruction()` | `number` | |
| `getCurrentThrustPN()` | `number` | |
| `getAirflowMs()` | `number` | |
| `getFuelItem()` | `table` or `nil` | `{name, count}` of the item in the fuel slot |
| `getBurnTimeRemaining()` | `number` | Ticks left on current burn (only decreases while powered) |
| `isBurning()` | `boolean` | `true` when powered and a burn is active |
| `list()` | `table` | Fuel slot inventory |
| `pushItems(toName, fromSlot[, limit[, toSlot]])` | `number` | Push from thruster to another peripheral |
| `pullItems(fromName, toSlot[, limit[, fromSlot]])` | `number` | Pull into fuel slot from another peripheral |

Block overview: [Solid Fuel Thruster](Solid-Fuel-Thruster.md).

---

## Ion thruster (`ion_thruster`)

FE-powered ion thrusters: same throttle and thrust readouts as [Thruster](#thruster-thruster), but **no fluid API**. Internal buffer is exposed as FE:

| Method | Returns | Notes |
|--------|---------|--------|
| `setPower` / `setPowerNormalized` | — | Same throttle semantics as [Throttle control](#throttle-control-all-thrusters) |
| `getPower()` | `number` | |
| `getObstruction()` | `number` | Returns unobstructed-block count from the obstruction scan |
| `getCurrentThrustPN()` | `number` | |
| `getAirflowMs()` | `number` | |
| `getEnergyAmountFe()` | `number` | Stored FE |
| `getEnergyCapacityFe()` | `number` | Buffer capacity |

---

## Creative thruster (`creative_thruster`)

Same throttle methods as the normal thruster, plus scroll-equivalent thrust configuration.

| Method | Returns | Notes |
|--------|---------|--------|
| `setPower` / `setPowerNormalized` | — | Same semantics as [Thruster](#thruster-thruster) |
| `getPower()` | `number` | |
| `setThrustConfig(percent)` | — | Scroll step `0 … 99` (matches on-block UI steps; capped internally) |
| `getThrustConfig()` | `number` | Current step |
| `getTargetThrustPN()` | `number` | Base thrust target from scroll / config (before throttle & atmosphere) |
| `getCurrentThrustPN()` | `number` | |
| `getAirflowMs()` | `number` | |
| `getObstruction()` | `number` | Returns unobstructed-block count from the obstruction scan |

---

## Vector thruster (`vector_thruster`)

Standard **fuel** vector thruster (not liquid fuel cell, not creative). Direction + throttle only; attach/detach follows [Throttle control](#throttle-control-all-thrusters).

### Direction (`-1 … 1` plane)

| Method | Notes |
|--------|--------|
| `getVectorX()` / `getVectorY()` | Current nozzle direction components |
| `getTargetVectorX()` / `getTargetVectorY()` | Target |
| `setVectorX(x)` | Precise `x` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVectorY(y)` | Precise `y` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVector(x, y)` | Both coordinates are clamped and retain floating-point precision |

### Throttle (`0 … 15` or normalized)

| Method | Notes |
|--------|--------|
| `setThrust(power)` | `power`: `0 … 15` → peripheral throttle |
| `setThrustNormalized(power)` | `power`: `0.0 … 1.0` |
| `setPower(power)` | Alias of `setThrust` |
| `setPowerNormalized(power)` | Alias of `setThrustNormalized` |
| `getThrust()` | `0 … 15` scale |
| `getPower()` | Normalized `0.0 … 1.0` |

---

## Liquid vector thruster (`liquid_vector_thruster`)

Liquid-fuel vector thruster: **same direction and throttle methods** as **Vector thruster** (`vector_thruster`) above, **plus fluid tank passthrough methods**. No `setThrustOutput` / override helpers (those exist only on **Creative vector thruster**).

### Direction (`-1 … 1` plane)

| Method | Notes |
|--------|--------|
| `getVectorX()` / `getVectorY()` | Current nozzle direction components |
| `getTargetVectorX()` / `getTargetVectorY()` | Target |
| `setVectorX(x)` | Precise `x` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVectorY(y)` | Precise `y` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVector(x, y)` | Both coordinates are clamped and retain floating-point precision |

### Throttle (`0 … 15` or normalized)

| Method | Notes |
|--------|--------|
| `setThrust(power)` | `power`: `0 … 15` → peripheral throttle |
| `setThrustNormalized(power)` | `power`: `0.0 … 1.0` |
| `setPower(power)` | Alias of `setThrust` |
| `setPowerNormalized(power)` | Alias of `setThrustNormalized` |
| `getThrust()` | `0 … 15` scale |
| `getPower()` | Normalized `0.0 … 1.0` |

### Fluid passthrough (`IFluidHandler`)

| Method | Returns | Notes |
|--------|---------|--------|
| `tanks()` | `table` | |
| `pushFluid(toName[, limit[, fluidName]])` | `number` | |
| `pullFluid(fromName[, limit[, fluidName]])` | `number` | |

---
## Creative vector thruster (`creative_vector_thruster`)

Separate peripheral implementation (`CreativeVectorThrusterPeripheral`): direction + throttle match **Vector thruster**, plus override APIs below.

### Direction (`-1 … 1` plane)

| Method | Notes |
|--------|--------|
| `getVectorX()` / `getVectorY()` | Current nozzle direction components |
| `getTargetVectorX()` / `getTargetVectorY()` | Target |
| `setVectorX(x)` | Precise `x` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVectorY(y)` | Precise `y` clamped `-1.0 … 1.0`; not quantized to redstone levels |
| `setVector(x, y)` | Both coordinates are clamped and retain floating-point precision |

### Throttle (`0 … 15` or normalized)

| Method | Notes |
|--------|--------|
| `setThrust(power)` | `power`: `0 … 15` → peripheral throttle |
| `setThrustNormalized(power)` | `power`: `0.0 … 1.0` |
| `setPower(power)` | Alias of `setThrust` |
| `setPowerNormalized(power)` | Alias of `setThrustNormalized` |
| `getThrust()` | `0 … 15` scale |
| `getPower()` | Normalized `0.0 … 1.0` |

### Base thrust override (creative only)

| Method | Returns | Notes |
|--------|---------|--------|
| `setThrustOutput(thrustOutputPn)` | — | Base thrust in goggles-tooltip **pN**. Clamped to the same maximum as the on-block scroll: `maxThrust` in `creativeVectorThruster` of the common config. Pass **`< 0`** (e.g. `-1`) to clear override and use scroll thrust again. |
| `clearThrustOutput()` | — | Same as `setThrustOutput(-1)` — removes Lua thrust override |
| `getMaxThrustOutputPn()` | `number` | Max allowed `setThrustOutput` in pN from config |
| `isCustomThrustOutputActive()` | `boolean` | `true` if a CC thrust override is active |

---

## RCS thruster (`rcs_thruster`)

Single and quad RCS thrusters. Nozzles are numbered from `1`: the single RCS has `1`, the quad has `1 … 4` (exhaust south, north, west, east in the block's own orientation).

While a computer is attached, nozzles you set are controlled by Lua instead of redstone or links. When the last computer detaches, all nozzles return to redstone control.

With hydrazine in the tank a nozzle is **boosted** (thrust multiplied by `hydrazineThrustMultiplier`, default `2.5`). Hydrazine use scales with the average nozzle power.

| Method | Returns | Notes |
|--------|---------|--------|
| `getNozzleCount()` | `number` | `1` or `4` |
| `setNozzlePower(nozzle, power)` | — | `power`: `0 … 15` |
| `setNozzlePowerNormalized(nozzle, power)` | — | `power`: `0.0 … 1.0` |
| `setNozzleThrustPN(nozzle, thrust)` | — | Precise thrust in pN, clamped to `0 … getMaxThrustPN()`. Stored as a fraction of the current max, so it changes if boost turns on or off |
| `getNozzlePower(nozzle)` | `number` | Normalized `0.0 … 1.0` |
| `getNozzleThrustPN(nozzle)` | `number` | Current thrust of one nozzle |
| `getCurrentThrustPN()` | `number` | All nozzles combined |
| `getMaxThrustPN()` | `number` | Per-nozzle full thrust; higher while boosted |
| `isBoosted()` | `boolean` | `true` while the hydrazine boost is active |
| `getFuelAmountMb()` | `number` | |
| `getFuelCapacityMb()` | `number` | |
| `tanks()` | `table` | |
| `pushFluid(toName[, limit[, fluidName]])` | `number` | |
| `pullFluid(fromName[, limit[, fluidName]])` | `number` | Only hydrazine is accepted |

---

## Hydropump (`hydropump`)

| Method | Returns | Notes |
|--------|---------|--------|
| `getPumpingSpeed()` | `number` | Current pumping speed in mB per 20 ticks |
| `setPumpingSpeed(speed)` | — | Clamped to `1 … getMaxPumpingSpeed()`, same as the on-block scroll |
| `getMaxPumpingSpeed()` | `number` | Maximum from the `hydropump` config |

---

## Stirling engine (`stirling_engine`)

| Method | Notes |
|--------|--------|
| `getRpm()` | |
| `setSpeed(targetSpeed)` | Clamped to supported scroll levels |
| `setActive(active)` | |

---

## Redstone transmission (`redstone_transmission`)

| Method | Notes |
|--------|--------|
| `getTransmissionMode()` | `"direct"` or `"incremental"` |
| `setTransmissionMode(mode)` | Throws on invalid mode |
| `getShiftLevel()` | |
| `setShiftLevel(level)` | |

---

## Coral generator (`coral_generator`)

| Method |
|--------|
| `getCoralAmountMb()` |
| `getCoralCapacityMb()` |
| `getEnergyAmountFe()` |
| `getEnergyCapacityFe()` |

---

## Lidar (`lidar`)

A 360° sensor dome: it sees the whole half-sphere above the surface it is mounted on (90° from its axis in every direction), and reports what rays from its sensor hit. Place it against a surface; its axis points away from that surface. Rays start at the sensor face and are measured in world space, so they work from ships, into ships, and between ships.

The lidar also works without a computer. Every tick it casts one ray straight along its axis and outputs redstone on every side except its sensor face. Right-click it with an empty hand to open its screen:

- **Detection toggles**: which targets the ray hits: blocks, fluids, non-solid blocks, see-through blocks, mobs, players, other ships, its own ship, and precise block shapes. They match the `raycast` options of the same names.
- **Range slider**: how far the ray reaches, 0 to 64 blocks (default 32). Drag the knob or scroll over it; hold Shift to scroll 8 at a time.
- **Block filter**: up to 18 blocks, buckets or Create filters. **Include** hits only the listed blocks; **Exclude** ignores them. An empty filter matches every block.
- **Analog / Binary**: analog outputs 15 for a hit right at the sensor, falling to 1 at the set range; binary outputs 15 for any hit.
- **Invert**: outputs `15 − signal`, so the output is strongest when nothing is hit.

These settings only drive the redstone output. `raycast` and `scan` use the options the computer passes.

| Method | Returns | Notes |
|--------|---------|-------|
| `raycast([options])` | hit table or `nil` | `nil` when nothing is hit within range. Runs on the server thread |
| `scan([options])` | list of hit tables | Pulse scan: many rays at once, see [Pulse scan](#pulse-scan). Rays run on worker threads, not the server thread |
| `getDirection([frame])` | vector | The sensor axis. Default `"local"` (ship axes); `"world"` for world space |
| `getMaxRange()` | `number` | Longest allowed ray or scan, `64` blocks |
| `getMaxAngle()` | `number` | Largest allowed angle between any ray and the sensor axis, `90` degrees (the full dome) |
| `getMaxScanRays()` | `number` | Most rays one `scan` may fire, `4096` |

**Options** (all optional):

| Option | Default | Meaning |
|--------|---------|---------|
| `range` | `64` | Ray length in blocks, up to `getMaxRange()` |
| `direction` | sensor axis | Vector to cast along. Must be within `getMaxAngle()` of the sensor axis |
| `frame` | `"local"` | Frame of `direction`: `"local"` (ship axes) or `"world"` |
| `fluids` | `false` | Hit water, lava and other fluids |
| `nonSolid` | `false` | Hit blocks without collision (grass, flowers, signs) |
| `seeThrough` | `true` | Hit blocks you can see through (glass, leaves, ice) |
| `entities` | `true` | Hit mobs and other entities |
| `players` | `true` | Hit players |
| `subLevels` | `true` | Hit blocks on other ships |
| `ownShip` | `false` | Hit blocks on the ship the lidar is mounted on |
| `blocks` | `true` | Report block hits. `false` reports only entities, players and (in `scan`) whole ships; blocks still block line of sight |
| `precise` | `true` | Use each block's exact shape. `false` treats every block that counts as a full cube: faster, but slabs, fences and plants report the hit at the edge of their block space |
| `blockState` | `true` | Include `block.state` in results. `false` returns only `block.id`, which is much cheaper for large scans |
| `include` | — | List of block ids or `#tags`; only these blocks count as hits. At most 256 entries |
| `exclude` | — | List of block ids or `#tags`; the ray passes through these. At most 256 entries |

**Hit table:**

```lua
{
  distance = 12.4,                -- blocks from the sensor face
  position = {x, y, z},           -- world position of the hit
  type = "block",                 -- "block" | "subLevel" | "mob" | "player" | "entity"
  block = { id = "minecraft:stone", state = { ... } },   -- block and subLevel hits; state values are strings
  entity = { type = "minecraft:player", name = "...", position = {x, y, z} },  -- entity hits; name for players and name-tagged entities, position is the entity's feet
  subLevel = { id = "...", name = "..." },               -- when the hit block is on a ship
}
```

```lua
local lidar = peripheral.find("lidar")
local hit = lidar.raycast({ range = 64, entities = false, exclude = { "#minecraft:leaves" } })
if hit then print(hit.type, ("%.1f"):format(hit.distance)) else print("clear") end
```

### Pulse scan

`scan([options])` fires a cone of rays in one call and returns everything they hit, nearest first. A block or entity hit by several rays appears once, at its nearest distance. It takes every `raycast` option, plus:

| Option | Default | Meaning |
|--------|---------|---------|
| `range` | `64` | How far the rays reach, up to `getMaxRange()` |
| `angle` | `90` | Half-angle of the scanned cone in degrees, up to `getMaxAngle()`. `90` scans the whole dome |
| `radius` | — | Alternative to `angle` for narrow cones: radius of the scanned circle at `range`, giving a half-angle of `atan(radius / range)`. Ignored when `angle` is set |
| `rays` | `256` | Number of rays, `1 … 4096`, spread evenly over the circle |

The whole cone must stay within `getMaxAngle()` of the sensor axis, counting `direction` if you aim it off-axis. Rays are spread evenly over the cone's part of the dome. Neighbouring rays end up about `range × √(2π × (1 − cos angle) / rays)` apart at the far end: for the full dome at range 64, that is about 2.5 blocks with 4096 rays.

Blocks are found by sampling, so a block between two rays can be missed; raise `rays` for denser coverage. Entities are not sampled: every entity inside the cone and within range is checked once for line of sight from the sensor, so small or distant entities are found even when no ray would have hit them. An entity counts as visible when the line to its middle, head or feet is clear.

**Threads.** On the server thread, `scan` only takes a snapshot of the area: nearby chunks, ships' positions and entities. The rays then run on up to 4 worker threads while the server keeps ticking. The result usually arrives in the same tick or the next one, from the world as it was when the scan started. While the computer waits, it receives a `lidar_scan` event that `scan` uses internally; your own event loops can ignore it. Each lidar runs one scan at a time: calling `scan` while it is still working throws `Lidar is busy`. If too many scans are running on the server, it throws `Lidar workers are busy, try again`.

**Target mode.** With `blocks = false`, `scan` fires no sampling rays at all, so `rays` doesn't matter. It only checks targets: every entity and player in the cone (as above), and, with `subLevels = true`, every other ship whose bounding box is in the cone. A ship is aimed at from its centre and eight points around it; the first ray whose first hit is one of that ship's blocks makes it visible. It is reported once, as `type = "subLevel"`, with the block that was hit and the `subLevel` table. This is the cheapest way to look for players, mobs or other ships:

```lua
local ships = lidar.scan({ range = 64, blocks = false, entities = false, players = false })
```

```lua
local hits = lidar.scan({ range = 32, radius = 12, rays = 128, entities = true })
for _, hit in ipairs(hits) do
  if hit.type == "player" then print(hit.entity.name, ("%.1f"):format(hit.distance)) end
end
```

### Debug view

Set `lidar = true` under `[debug]` in the client config (`createpropulsion-client.toml`) to see every ray the lidar casts, for 4 ticks after each call. Only players with the option on receive the rays, and only within 128 blocks of the lidar. The debug view is operator-only (permission level 2 or higher).

| Colour | Meaning |
|--------|---------|
| Red | Hit a block |
| Magenta | Hit a ship block |
| Yellow | Hit an entity or player |
| Cyan | Hit nothing within range |

Hits also get a small cross at the hit point. In target mode you see only the line-of-sight checks, not a full cone.
