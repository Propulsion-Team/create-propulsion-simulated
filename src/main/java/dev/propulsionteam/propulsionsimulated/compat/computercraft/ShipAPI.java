package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.LuaValues.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ShipAPI implements ILuaAPI {
    private final IComputerSystem computer;

    public ShipAPI(IComputerSystem computer) {
        this.computer = computer;
    }

    @Override
    public String[] getNames() {
        return new String[] {"ship"};
    }

    private ShipContext ship() throws LuaException {
        ShipContext ship = ShipContext.of(computer);
        if (ship == null) throw new LuaException("This computer is not on a ship");
        return ship;
    }

    private static Map<String, Object> inFrame(ShipContext ship, Vector3d world, Optional<String> frame)
        throws LuaException {
        return LuaValues.vector(LuaValues.frame(frame, Frame.WORLD) == Frame.LOCAL ? ship.rotateToLocal(world) : world);
    }

    private Vector3d computerPosition(ShipContext ship) {
        BlockPos pos = computer.getPosition();
        return ship.plotToWorld(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Nullable
    private Map<String, Object> computerAxes(ShipContext ship, Frame frame) {
        BlockState state = computer.getLevel().getBlockState(computer.getPosition());
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return null;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Vector3d forward = new Vector3d(facing.getStepX(), 0, facing.getStepZ());
        Vector3d up = new Vector3d(0, 1, 0);
        Vector3d right = forward.cross(up, new Vector3d());
        if (frame == Frame.WORLD) {
            forward = ship.rotateToWorld(forward);
            up = ship.rotateToWorld(up);
            right = ship.rotateToWorld(right);
        }
        return Map.of("right", LuaValues.vector(right), "up", LuaValues.vector(up), "forward", LuaValues.vector(forward));
    }

    @LuaFunction(mainThread = true)
    public final Object getComputerAxes(Optional<String> frame) throws LuaException {
        return computerAxes(ship(), LuaValues.frame(frame, Frame.WORLD));
    }

    @LuaFunction(mainThread = true)
    public final boolean isOnShip() {
        return ShipContext.of(computer) != null;
    }

    @LuaFunction(mainThread = true)
    public final String getId() throws LuaException {
        return ship().subLevel.getUniqueId().toString();
    }

    @LuaFunction(mainThread = true)
    public final Object getName() throws LuaException {
        return ship().subLevel.getName();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getPosition() throws LuaException {
        return LuaValues.vector(ship().centerOfMassWorld());
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getComputerPosition() throws LuaException {
        return LuaValues.vector(computerPosition(ship()));
    }

    @LuaFunction(mainThread = true)
    public final double getMass() throws LuaException {
        return ship().massOrZero();
    }

    @LuaFunction(mainThread = true)
    public final Object getInertiaTensor() throws LuaException {
        ShipContext ship = ship();
        return ship.mass != null ? LuaValues.matrix(ship.mass.getInertiaTensor()) : null;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getVelocity(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        return inFrame(ship, ship.velocityWorld(), frame);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getAirVelocity(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        return inFrame(ship, ship.airVelocityWorld(), frame);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getAngularVelocity(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        return inFrame(ship, ship.angularVelocityWorldDegrees(), frame);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getGravity(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        return inFrame(ship, ship.gravityWorld(), frame);
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getEuler() throws LuaException {
        double[] hpr = ship().headingPitchRoll();
        return MethodResult.of(hpr[0], hpr[1], hpr[2]);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getOrientation() throws LuaException {
        return LuaValues.quaternion(ship().orientation);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getState() throws LuaException {
        ShipContext ship = ship();
        double[] hpr = ship.headingPitchRoll();
        Map<String, Object> out = new HashMap<>();
        out.put("id", ship.subLevel.getUniqueId().toString());
        if (ship.subLevel.getName() != null) out.put("name", ship.subLevel.getName());
        out.put("position", LuaValues.vector(ship.centerOfMassWorld()));
        out.put("computerPosition", LuaValues.vector(computerPosition(ship)));
        Map<String, Object> axes = computerAxes(ship, Frame.WORLD);
        if (axes != null) out.put("computerAxes", axes);
        out.put("orientation", LuaValues.quaternion(ship.orientation));
        out.put("heading", hpr[0]);
        out.put("pitch", hpr[1]);
        out.put("roll", hpr[2]);
        out.put("velocity", LuaValues.vector(ship.velocityWorld()));
        out.put("airVelocity", LuaValues.vector(ship.airVelocityWorld()));
        out.put("angularVelocity", LuaValues.vector(ship.angularVelocityWorldDegrees()));
        out.put("gravity", LuaValues.vector(ship.gravityWorld()));
        out.put("mass", ship.massOrZero());
        if (ship.mass != null) out.put("inertiaTensor", LuaValues.matrix(ship.mass.getInertiaTensor()));
        return out;
    }
}
