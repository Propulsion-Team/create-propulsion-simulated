# ComputerCraft peripherals API

Create Propulsion registers these peripherals when **CC: Tweaked** is installed. Peripheral types match Create’s `SyncedPeripheral` pattern (`mainThread` Lua calls run on the server).

**Entry point:** see also [ComputerCraft overview](cc/README.md).

---

## Throttle control (all thrusters)

All thrust readouts and thrust-derived physics use the shared `thrustUnitsPerKn` physics config value (default `1000`) so displayed values match assembled/physics behavior.

While **any** computer is attached to a thruster peripheral:

1. The block uses **peripheral throttle** (normalized `0.0 … 1.0`), driven by Lua (`setPower`, `setThrust`, etc.).
2. **`getPower()`** returns that peripheral throttle. Normalized commands retain floating-point precision and are not rounded to one of the 16 redstone levels.
3. World **redstone signals next to the block do not drive thrust** until the computer disconnects.

When the computer **detaches**, peripheral throttle is cleared and the thruster returns to **normal** redstone-controlled behavior.

### Precise thrust and obstruction (all thrusters except RCS)

Every thruster peripheral also has:

| Method | Returns | Notes |
|--------|---------|--------|
| `setThrustPN(thrust)` | — | Sets throttle to `thrust / getMaxThrustPN()` in pN. Clamped to `0 … max`. Overrides redstone like `setPower`; a later `setPower` or `setPowerNormalized` replaces it |
| `setThrustKN(thrust)` | — | Same in kN |
| `getMaxThrustPN()` | `number` | Nominal thrust at full throttle for the current fuel, before obstruction and atmosphere |
| `getMaxThrustKN()` | `number` | Same in kN |
| `getObstructionEfficiency()` | `number` | `0.0 … 1.0` thrust fraction left after obstruction. `getObstruction()` gives the unobstructed block count |

The thrust is stored as a throttle fraction, so it changes if `getMaxThrustPN()` changes (for example after a fuel swap). Multiblock thrusters can be controlled through any block of the cube, and the values describe the whole cube.

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
| `getCurrentThrustKN()` | `number` | |
| `getDisplayedThrustPN()` | `number` | Tooltip-scale thrust |
| `getDisplayedThrustKN()` | `number` | |
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
| `getCurrentThrustPN()` / `getCurrentThrustKN()` | `number` | |
| `getDisplayedThrustPN()` / `getDisplayedThrustKN()` | `number` | |
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
| `getCurrentThrustPN()` / `getCurrentThrustKN()` | `number` | |
| `getDisplayedThrustPN()` / `getDisplayedThrustKN()` | `number` | |
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
| `getTargetThrustKN()` | `number` | |
| `getCurrentThrustPN()` | `number` | |
| `getCurrentThrustKN()` | `number` | |
| `getDisplayedThrustPN()` | `number` | |
| `getDisplayedThrustKN()` | `number` | |
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
| `setThrustOutput(thrustOutputPn)` | — | Base thrust in **pN**. Clamped to the same maximum as the on-block scroll: `creativeVectorThrusterMaxThrust` (kN from config) × `thrustUnitsPerKn` (physics config, default `1000`). Pass **`< 0`** (e.g. `-1`) to clear override and use scroll thrust again. |
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
| `getCurrentThrustPN()` / `getCurrentThrustKN()` | `number` | All nozzles combined |
| `getMaxThrustPN()` / `getMaxThrustKN()` | `number` | Per-nozzle full thrust; higher while boosted |
| `isBoosted()` | `boolean` | `true` while the hydrazine boost is active |
| `getFuelAmountMb()` | `number` | |
| `getFuelCapacityMb()` | `number` | |
| `tanks()` | `table` | |
| `pushFluid(toName[, limit[, fluidName]])` | `number` | |
| `pullFluid(fromName[, limit[, fluidName]])` | `number` | Only hydrazine is accepted |

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

