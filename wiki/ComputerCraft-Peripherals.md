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
| `getFuelAmount()` | `number` | `0` or `1` |
| `getFuelCapacity()` | `number` | Always `1` |
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
| `setVectorX(x)` | `x` clamped `-1.0 … 1.0` |
| `setVectorY(y)` | `y` clamped `-1.0 … 1.0` |
| `setVector(x, y)` | Both clamped |

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
| `setVectorX(x)` | `x` clamped `-1.0 … 1.0` |
| `setVectorY(y)` | `y` clamped `-1.0 … 1.0` |
| `setVector(x, y)` | Both clamped |

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
| `setVectorX(x)` | `x` clamped `-1.0 … 1.0` |
| `setVectorY(y)` | `y` clamped `-1.0 … 1.0` |
| `setVector(x, y)` | Both clamped |

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

## Tilt adapter (`tilt_adapter`)

| Method | Notes |
|--------|--------|
| `getLeftSignal()` | |
| `getRightSignal()` | |
| `setTargetAngle(angle)` | While attached, computer drives tilt target |

---

## Coral generator (`coral_generator`)

| Method |
|--------|
| `getCoralAmountMb()` |
| `getCoralCapacityMb()` |
| `getEnergyAmountFe()` |
| `getEnergyCapacityFe()` |

