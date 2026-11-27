# ComputerCraft ship APIs

With **CC: Tweaked** installed, Create Propulsion adds three global APIs and two Lua modules to every computer. You don't need any extra block, modem or addon.

| Name | Kind | What it does |
|------|------|--------------|
| [`ship`](#ship) | global API | Position, motion, orientation and mass of the ship the computer is on |
| [`thrusters`](#thrusters) | global API | Every Propulsion thruster on that ship: read, set, aim and allocate thrust |
| [`links`](#links) | global API | Send and receive Create redstone link signals. Works anywhere, not only on ships |
| [`propulsion.quaternion`](#propulsionquaternion) | `require` module | Rotation math |
| [`propulsion.matrix`](#propulsionmatrix) | `require` module | 3×3 matrix math, including inertia eigen-decomposition |

The APIs work for any computer or turtle placed on an assembled ship (a Sable sub-level). When the computer is not on a ship, `ship.isOnShip()` returns `false`, `thrusters.list()` returns an empty table, and every other `ship` / `thrusters` function raises `This computer is not on a ship`.

**See also:** [ComputerCraft peripherals](ComputerCraft-Peripherals.md) · [ComputerCraft overview](cc/README.md)

---

## Conventions

- **Axes.** Y is up and the frame is right-handed, as in Minecraft. A ship's **forward is its local −Z** (north when it was assembled), **right is +X**, **up is +Y**. Those depend on how the ship was assembled; for axes that follow the build, use [`ship.getComputerAxes()`](#ship).
- **Frames.** `"world"` uses Minecraft's axes. `"local"` uses the ship's own axes. Functions with an optional `frame` argument document their default.
- **Ship-local positions** are measured from the ship's **center of mass**, in blocks.
- **Angles are in degrees** everywhere. Angular velocities are in degrees per second.
  - `heading`: compass style, `0 … 360`. `0` means forward points north (−Z) and `90` means east (+X).
  - `pitch`: `-90 … 90`, positive is nose up.
  - `roll`: `-180 … 180`, positive is right side down.
- **Vectors** are returned as plain `{x, y, z}` tables. Wrap them with `vector.new(v.x, v.y, v.z)` to use CC's `vector` methods. Every function that takes a vector accepts any table with `x`, `y` and `z` fields, CC `vector` objects included.
- **Thrust** is in pN, the same unit the goggles and the [thruster peripherals](ComputerCraft-Peripherals.md) use.

### Tick cost

`ship` and `thrusters` functions read live physics, so each call runs on the server thread and **waits for the next game tick**. A control loop should do one `ship.getState()` and one `thrusters.setAll(...)` or `thrusters.allocate(...)` per iteration, instead of many small calls.

`propulsion.quaternion` and `propulsion.matrix` are plain Lua and cost no ticks.

---

## `ship`

| Function | Returns | Notes |
|----------|---------|-------|
| `ship.isOnShip()` | `boolean` | |
| `ship.getId()` | `string` | The sub-level's UUID |
| `ship.getName()` | `string` or `nil` | |
| `ship.getPosition()` | vector | World position of the center of mass |
| `ship.getComputerPosition()` | vector | World position of this computer's block center |
| `ship.getComputerAxes([frame])` | `{right, up, forward}` or `nil` | Axes defined by the computer block: `forward` points out of its front (screen) face, the same side CC calls `"front"`; for a turtle, the way it faces. `up` is the ship's up, `right = forward × up`. Default `"world"`; `"local"` gives them in ship axes. `nil` for a computer without a facing (pocket computers) |
| `ship.getMass()` | `number` | |
| `ship.getVelocity([frame])` | vector | Center-of-mass velocity in blocks/s. Default `"world"` |
| `ship.getAirVelocity([frame])` | vector | Velocity relative to the air (wind removed). Default `"world"` |
| `ship.getAngularVelocity([frame])` | vector | Degrees/s about each axis. Default `"world"` |
| `ship.getGravity([frame])` | vector | Gravitational acceleration at the center of mass, blocks/s², from the dimension's physics settings. Default `"world"`. Weight is `getMass() * length` |
| `ship.getEuler()` | `heading, pitch, roll` | Three return values, see [conventions](#conventions) |
| `ship.getOrientation()` | `{x, y, z, w}` | Unit quaternion, local → world. Pass to `quaternion.new` |
| `ship.getInertiaTensor()` | 3×3 table | About the center of mass, in ship axes: `t[row][col]`. `nil` while mass data is unavailable. Pass to `matrix.new` for its inverse or principal axes |
| `ship.getState()` | table | Everything above in one call, see below |

`ship.getState()` returns world-frame values:

```lua
{
  id, name,                        -- name may be nil
  position, computerPosition,      -- vectors
  computerAxes,                    -- {right, up, forward}, may be nil
  orientation,                     -- {x, y, z, w}
  heading, pitch, roll,            -- degrees
  velocity, airVelocity,           -- blocks/s
  angularVelocity,                 -- degrees/s
  gravity,                         -- blocks/s^2
  mass,
  inertiaTensor,                   -- may be nil
}
```

Anything derived from the orientation is plain Lua with [`propulsion.quaternion`](#propulsionquaternion) and costs no tick:

```lua
local q = quaternion.new(s.orientation)
local localVelocity = q:rotateInverse(s.velocity)    -- world -> ship axes
local right, up, forward = q:axes()                  -- ship axes in world space
local localPos = q:rotateInverse(vector.new(p.x, p.y, p.z) - s.position) -- world point -> ship-local
```

---

## `thrusters`

Every thruster on the ship can be listed and controlled, whether or not it is next to the computer. Each thruster has a string **id**, `"x,y,z"`. RCS thrusters list each **nozzle** separately as `"x,y,z#n"`, where `n` is `1 … 4`, matching the RCS peripheral's nozzle numbers. Multiblock thrusters appear once, under their controller block.

| Function | Returns | Notes |
|----------|---------|-------|
| `thrusters.list()` | table of thruster info | Empty table when not on a ship |
| `thrusters.get(id)` | thruster info | |
| `thrusters.setThrust(id, pn)` | — | Clamped to `0 … maxThrust` |
| `thrusters.setThrottle(id, t)` | — | `t` in `0.0 … 1.0` |
| `thrusters.setAll({[id] = pn, ...})` | — | Sets several thrusters in one tick. Thrusters not in the table are left alone |
| `thrusters.setVector(id, direction [, frame])` | vector | Vector thrusters only. Aims the force (not the exhaust) along `direction`, clamped to the gimbal range. Default frame `"local"`. Returns the direction the thruster will push in once the gimbal has turned |
| `thrusters.getNetForce([frame])` | vector | Sum of current thrust. Default `"local"` |
| `thrusters.getNetTorque([frame])` | vector | About the center of mass. Default `"local"` |
| `thrusters.allocate(force, torque [, frame])` | table | See below |
| `thrusters.setVectorCores(enabled [, ids])` | `boolean` | Enables or disables Vector Cores on the listed thrusters, or on every thruster on the ship when `ids` is `nil`. An RCS nozzle id addresses its whole block. Returns `false` if any of them has no core or a depleted one |
| `thrusters.release([id])` | — | Returns one thruster, or all thrusters this computer controls, to redstone control |

**Thruster info** fields:

| Field | Type | Notes |
|-------|------|-------|
| `id` | `string` | |
| `type` | `string` | Same as the peripheral type: `thruster`, `ion_thruster`, `solid_fuel_thruster`, `creative_thruster`, `vector_thruster`, `liquid_vector_thruster`, `creative_vector_thruster`, `rcs_thruster` |
| `position` | vector | Where the force is applied, ship-local (from the center of mass) |
| `direction` | vector | Unit direction of the force on the ship, ship axes |
| `torque` | vector | Torque per pN of thrust, `position × direction`. Zero when a Vector Core applies the force at the center of mass |
| `maxThrust` | `number` | pN at full throttle |
| `thrust` | `number` | Current pN |
| `throttle` | `number` | `0.0 … 1.0` |
| `core` | `boolean` | An enabled Vector Core is installed |
| `vector` | `boolean` | Can be aimed with `setVector` |
| `nozzle` | `number` | RCS only |
| `obstruction` | `number` | Not for RCS. `0.0 … 1.0` thrust left after obstruction |

### Taking control

Setting a thruster through this API works like attaching a peripheral: the thruster ignores redstone and follows the computer. It goes back to redstone control when you call `thrusters.release()` or when the computer shuts down or reboots. A thruster that another computer still has wrapped as a peripheral stays under computer control.

### `thrusters.allocate(force, torque [, frame])`

Finds thrust values whose combined force and torque best match `force` (pN) and `torque` (pN·blocks), then applies them. Either target may be `nil` for zero. The default frame is `"local"`. Each thruster stays within `0 … maxThrust`, and thrusters that would only cancel each other out stay off. The solve is least squares, so if the request is impossible you get the closest possible result. Vector thrusters keep their current direction. Aim them with `setVector` first. Each call starts from the previous solution, so calling it every tick stays smooth, and `force` or `torque` components above 1e12 are rejected with an error.

It returns:

```lua
{
  thrusts = { [id] = pn, ... }, -- what was applied
  force = vector,               -- the force those thrusts produce
  torque = vector,              -- the torque those thrusts produce
}
```

`maxThrust` doesn't include obstruction or air pressure, so the ship may push slightly less than `force` says.

---

## `links`

The computer joins Create's redstone link networks as an invisible link at its own block. Normal links, and the links on vector and RCS thrusters, can't tell it apart from a Redstone Link block. Create's link range applies (`links.getRange()`, 256 blocks by default), measured in world space, so it also works between ships and the ground.

### Frequencies

A frequency is a **pair** of items, `f1` and `f2`, the same as the two slots on a Redstone Link. `f1` is the upper slot on a wall-mounted link. Each item can be:

| Value | Meaning |
|-------|---------|
| `"minecraft:redstone"` | An item ID |
| `nil` or `""` | Empty slot |
| `{item = "minecraft:leather_helmet", color = 0xFF0000}` | A dyed item. Create tells dye colours apart; any other item data is ignored |

Frequencies returned by the API use the same format: a string, `""` for an empty slot, or the table form for dyed items.

### Scope

Functions with a `scope` argument accept:

- `"all"` (default): normal Create behaviour.
- `"ship"`: only links on the **same ship** as the computer. A ship-scoped signal isn't heard by anything off the ship, and reading with `"ship"` ignores everything off the ship. Use it when several copies of one ship design share frequencies. This scope raises an error if the computer isn't on a ship.

### Functions

| Function | Returns | Notes |
|----------|---------|-------|
| `links.sendLinkSignal(f1, f2, strength [, scope])` | — | Starts transmitting, changes the strength, or stops with `0`. Clamped to `0 … 15` |
| `links.sendLinkSignals({ {f1, f2, strength [, scope]}, ... })` | — | Many at once, in one tick. At most 128 entries per call; invalid entries raise an error before anything is applied |
| `links.getLinkSignal(f1, f2 [, scope])` | `0 … 15` | Strongest signal received. The computer's own transmissions don't count |
| `links.getActiveSignals()` | `{ {f1, f2, strength, scope}, ... }` | What this computer is transmitting. Entries can be passed back to `sendLinkSignals` |
| `links.listen(f1, f2 [, scope])` | `0 … 15` | Fires `redstone_link` events when the signal changes. Returns the current strength |
| `links.unlisten(f1, f2)` | `boolean` | `false` if the computer wasn't listening |
| `links.scan([scope])` | `{ {f1, f2, strength}, ... }` | Every frequency pair with a non-zero signal reaching this computer. Examines at most 4096 networks per call |
| `links.getRange()` | `number` | Create's `linkRange` server config |
| `links.clearAll()` | — | Stops every transmission and listener of this computer |

### Event

`"redstone_link", f1, f2, strength` fires when a frequency registered with `links.listen` changes strength.

```lua
links.listen("minecraft:redstone", "minecraft:lever")
while true do
  local _, f1, f2, strength = os.pullEvent("redstone_link")
  print(f1, f2, strength)
end
```

Each computer can have at most 128 transmitters and 128 listeners; creating more raises an error (changing or removing existing ones always works).

Waiting for the event costs nothing. Calling `getLinkSignal` in a loop costs one tick per call.

### Lifetime

Transmissions and listeners belong to the running computer. They stop when it shuts down, reboots, is broken or is unloaded, the same way CC resets `redstone.setOutput`. A computer that moves to another dimension (for example a pocket computer) drops its links on the next `links` call.

---

## `propulsion.quaternion`

```lua
local quaternion = require("propulsion.quaternion")
```

Quaternions are tables `{x, y, z, w}` with methods. `q1 * q2` combines rotations (`q2` first), and `q * v` rotates a vector.

| Function | Notes |
|----------|-------|
| `quaternion.new(x, y, z, w)` / `quaternion.new(t)` | `t` can be the table from `ship.getOrientation()`. No arguments gives the identity |
| `quaternion.identity()` | |
| `quaternion.fromEuler(heading [, pitch [, roll]])` | Same angle convention as `ship` |
| `quaternion.fromAxisAngle(axis, degrees)` | |
| `quaternion.fromRotationVector(v)` | Axis × angle (degrees) |
| `quaternion.lookAt(forward [, up])` | Orientation whose forward (−Z) points along `forward`. `up` defaults to world up |
| `quaternion.rotationTo(from, to)` | Shortest rotation turning direction `from` into `to` |
| `quaternion.fromMatrix(m)` | From a rotation matrix |

| Method | Returns |
|--------|---------|
| `q:mul(o)` | `q * o` |
| `q:rotate(v)` / `q:rotateInverse(v)` | vector |
| `q:inverse()`, `q:conjugate()`, `q:normalize()` | quaternion |
| `q:length()`, `q:dot(o)` | `number` |
| `q:slerp(o, t)` | quaternion, shortest path |
| `q:toEuler()` | `heading, pitch, roll` |
| `q:toAxisAngle()` | `axis, degrees` |
| `q:toRotationVector()` | vector (axis × degrees) |
| `q:axes()` | `right, up, forward` |
| `q:toMatrix()` | `propulsion.matrix` |

---

## `propulsion.matrix`

```lua
local matrix = require("propulsion.matrix")
```

Matrices are tables of rows, `m[row][col]`, and act on column vectors: `m * v`. They support `*` (by a matrix, vector or number), `+`, `-` and unary `-`.

| Function | Notes |
|----------|-------|
| `matrix.new(rows)` / `matrix.new(a, b, c, d, e, f, g, h, i)` | `rows` can be the table from `ship.getInertiaTensor()`. No arguments gives the identity |
| `matrix.identity()`, `matrix.zero()` | |
| `matrix.fromScale(v)` | Diagonal matrix |
| `matrix.fromColumns(x, y, z)` | |
| `matrix.fromQuaternion(q)` | Rotation matrix |

| Method | Returns | Notes |
|--------|---------|-------|
| `m:mul(o)`, `m:add(o)`, `m:sub(o)` | matrix or vector | |
| `m:transform(v)` | vector | `m * v` |
| `m:transformInverse(v)` | vector | `transpose(m) * v`, which is the inverse for rotation matrices |
| `m:transpose()`, `m:inverse()` | matrix | `inverse` raises an error for a singular matrix |
| `m:determinant()` | `number` | |
| `m:orthonormalize()` | matrix | Gram–Schmidt on the columns |
| `m:diagonalize()` | `values, vectors` | Eigenvalues and matching unit eigenvectors of a **symmetric** matrix (Jacobi iteration) |
| `m:toQuaternion()`, `m:toEuler()` | | For rotation matrices |
| `m:row(i)`, `m:column(i)`, `m:copy()` | | |

---

## Example: hold level with a heading

```lua
local quaternion = require("propulsion.quaternion")

local targetHeading = 0
local kp, kd = 2.0, 1.5        -- tune for your ship

local target = quaternion.fromEuler(targetHeading, 0, 0)

while true do
  local s = ship.getState()
  local q = quaternion.new(s.orientation)

  -- angular error and rate, both in ship axes and degrees
  local err = q:inverse():mul(target):toRotationVector()
  local w = q:rotateInverse(s.angularVelocity)
  local torque = err * kp - w * kd

  -- cancel the weight (world space, converted to ship axes) and correct the attitude
  local up = q:rotateInverse(vector.new(s.gravity.x, s.gravity.y, s.gravity.z) * -s.mass)
  thrusters.allocate(up, torque)
end
```

`getState` and `allocate` are the only calls that cost a tick; the quaternion math is free.

---

## License note

`propulsion/quaternion.lua` and `propulsion/matrix.lua` are partly ported from Godot Engine's `core/math/basis.cpp`, which is MIT licensed. The required notice is in both files and in the repository's `LICENSE`.
