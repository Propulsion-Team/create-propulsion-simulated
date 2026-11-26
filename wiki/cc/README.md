# ComputerCraft (CC: Tweaked)

Create Propulsion registers block peripherals when **CC: Tweaked** (`computercraft` / `cc_tweaked`) is installed alongside Create’s ComputerCraft integration.

**Full API:** [ComputerCraft peripherals reference](../ComputerCraft-Peripherals.md) — method lists, throttle behavior, and creative-only APIs.

**Ship APIs:** [ComputerCraft ship APIs](../ComputerCraft-Ship-API.md) — global `ship`, `thrusters` and `links` (redstone link) APIs plus the `propulsion.quaternion` / `propulsion.matrix` modules, available on any computer placed on a ship, with no peripheral needed.

### Quick facts

- Peripheral methods marked `mainThread = true` run on the server thread (safe for world/block updates).
- While a computer is **attached**, thrusters enter **peripheral control**: throttle comes from Lua (`setPower` / `setThrust` / …), not from adjacent redstone strength.
- On **detach**, thrusters return to normal redstone control and Lua throttle is cleared.

### Related wiki pages

- [KubeJS API](../KubeJS-API.md)
- [Datapack example usage (liquid fuels)](../Datapack-Example-Usage.md)
- [Solid Fuel Thruster](../Solid-Fuel-Thruster.md)
