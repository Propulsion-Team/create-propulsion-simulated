package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.LuaValues.Frame;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.PropulsionCoreBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterForceProvider;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsOrientation;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterControlMath;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ThrustersAPI implements ILuaAPI {
    private static final int ALLOCATION_SWEEPS = 200;
    private static final long ALLOCATION_WORK = 200_000;
    private static final double MAX_ALLOCATION_INPUT = 1.0e12;

    private final IComputerSystem computer;
    private final Set<BlockEntity> controlled = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, Double> warmStart = new HashMap<>();

    public ThrustersAPI(IComputerSystem computer) {
        this.computer = computer;
    }

    @Override
    public String[] getNames() {
        return new String[] {"thrusters"};
    }

    @Override
    public void shutdown() {
        ServerThreadTasks.queue(this::releaseAll);
    }

    private record Entry(String id, String type, BlockEntity blockEntity, int nozzle, Vector3d point,
                         Vector3d direction, double maxPn, double currentPn, double throttle, boolean core) {
        Vector3d torquePerPn(ShipContext ship) {
            return core ? new Vector3d() : ship.plotToLocal(point).cross(direction);
        }
    }

    private static String id(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String type(@Nullable AbstractComputerBehaviour behaviour) {
        IPeripheral peripheral = behaviour != null ? behaviour.getPeripheralCapability() : null;
        return peripheral != null ? peripheral.getType() : "thruster";
    }

    private static List<Entry> scan(ShipContext ship) {
        List<Entry> entries = new ArrayList<>();
        double unitsPerPn = PropulsionConfig.getThrustUnitsPerPnOrDefault();
        for (BlockEntitySubLevelActor actor : ship.subLevel.getPlot().getBlockEntityActors()) {
            if (actor instanceof AbstractThrusterBlockEntity thruster && !thruster.isRemoved()) {
                ThrusterForceProvider.ForceSample sample = ThrusterForceProvider.unitSample(thruster);
                if (sample == null) continue;
                entries.add(new Entry(id(thruster.getBlockPos()), type(thruster.computerBehaviour), thruster, -1,
                    sample.pointLocal(), sample.directionLocal(), thruster.getMaxThrustPn() / unitsPerPn,
                    thruster.getCurrentThrust() / unitsPerPn, thruster.getThrottle(),
                    PropulsionCoreBehaviour.isActive(thruster)));
            } else if (actor instanceof RcsThrusterBlockEntity rcs && !rcs.isRemoved()) {
                Direction normal = RcsThrusterBlock.normal(rcs.getBlockState());
                BlockPos pos = rcs.getBlockPos();
                String type = type(rcs.computerBehaviour);
                boolean core = PropulsionCoreBehaviour.isActive(rcs);
                for (int i = 0; i < rcs.links.length; i++) {
                    Vector3d point = RcsOrientation.point(RcsOrientation.nozzle(rcs.isSingle(), i), normal)
                        .add(pos.getX(), pos.getY(), pos.getZ());
                    Vector3d direction = RcsOrientation.impulse(rcs.isSingle(), i, normal, 1.0, 1.0).normalize();
                    entries.add(new Entry(id(pos) + "#" + (i + 1), type, rcs, i, point, direction,
                        rcs.thrust(), rcs.nozzleThrust(i), rcs.getNozzlePower(i), core));
                }
            }
        }
        return entries;
    }

    private ShipContext ship() throws LuaException {
        ShipContext ship = ShipContext.of(computer);
        if (ship == null) throw new LuaException("This computer is not on a ship");
        return ship;
    }

    private static Map<String, Entry> byId(ShipContext ship) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (Entry e : scan(ship)) map.put(e.id(), e);
        return map;
    }

    private static Entry find(Map<String, Entry> entries, Object id) throws LuaException {
        Entry entry = id instanceof String key ? entries.get(key) : null;
        if (entry == null) throw new LuaException("No thruster '" + id + "' on this ship");
        return entry;
    }

    private static Map<String, Object> inFrame(ShipContext ship, Vector3d local, Frame frame) {
        return LuaValues.vector(frame == Frame.WORLD ? ship.rotateToWorld(local) : local);
    }

    private static Map<String, Object> describe(ShipContext ship, Entry e) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", e.id());
        out.put("type", e.type());
        out.put("position", LuaValues.vector(ship.plotToLocal(e.point())));
        out.put("direction", LuaValues.vector(e.direction()));
        out.put("torque", LuaValues.vector(e.torquePerPn(ship)));
        out.put("maxThrust", e.maxPn());
        out.put("thrust", e.currentPn());
        out.put("throttle", e.throttle());
        out.put("core", e.core());
        out.put("vector", e.blockEntity() instanceof VectorThrusterBlockEntity);
        if (e.nozzle() >= 0) out.put("nozzle", e.nozzle() + 1);
        if (e.blockEntity() instanceof AbstractThrusterBlockEntity thruster) {
            out.put("obstruction", (double) thruster.getObstructionEfficiency());
        }
        return out;
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> list() {
        ShipContext ship = ShipContext.of(computer);
        if (ship == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Entry e : scan(ship)) out.add(describe(ship, e));
        return out;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> get(String id) throws LuaException {
        ShipContext ship = ship();
        return describe(ship, find(byId(ship), id));
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getNetForce(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        Vector3d force = new Vector3d();
        for (Entry e : scan(ship)) force.fma(e.currentPn(), e.direction());
        return inFrame(ship, force, LuaValues.frame(frame, Frame.LOCAL));
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getNetTorque(Optional<String> frame) throws LuaException {
        ShipContext ship = ship();
        Vector3d torque = new Vector3d();
        for (Entry e : scan(ship)) torque.fma(e.currentPn(), e.torquePerPn(ship));
        return inFrame(ship, torque, LuaValues.frame(frame, Frame.LOCAL));
    }

    @LuaFunction(mainThread = true)
    public final void setThrust(String id, double pn) throws LuaException {
        setAll(Map.of(id, pn));
    }

    @LuaFunction(mainThread = true)
    public final void setThrottle(String id, double throttle) throws LuaException {
        applyThrottle(find(byId(ship()), id), throttle);
    }

    @LuaFunction(mainThread = true)
    public final void setAll(Map<?, ?> thrusts) throws LuaException {
        Map<String, Entry> entries = byId(ship());
        for (Map.Entry<?, ?> item : thrusts.entrySet()) {
            Entry e = find(entries, item.getKey());
            if (!(item.getValue() instanceof Number pn)) throw new LuaException("Thrust for '" + e.id() + "' must be a number");
            applyThrottle(e, e.maxPn() > 0 ? pn.doubleValue() / e.maxPn() : 0.0);
        }
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> setVector(IArguments args) throws LuaException {
        ShipContext ship = ship();
        Entry e = find(byId(ship), args.getString(0));
        if (!(e.blockEntity() instanceof VectorThrusterBlockEntity vector)) {
            throw new LuaException("Thruster '" + e.id() + "' is not a vector thruster");
        }
        Frame frame = LuaValues.frame(args.optString(2), Frame.LOCAL);
        Vector3d direction = LuaValues.readVector(args, 1);
        if (frame == Frame.WORLD) direction = ship.rotateToLocal(direction);

        VectorThrusterControlMath.Coordinates c = vector.coordinatesForThrustDirection(direction);
        vector.setVectorCoordinates(c.x(), c.y());
        controlled.add(vector);
        return inFrame(ship, vector.thrustDirectionForCoordinates(c.x(), c.y()), frame);
    }

    @LuaFunction(mainThread = true)
    public final boolean setVectorCores(boolean enabled, Optional<Map<?, ?>> ids) throws LuaException {
        Map<String, Entry> entries = byId(ship());
        Set<BlockEntity> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        if (ids.isEmpty()) {
            for (Entry e : entries.values()) targets.add(e.blockEntity());
        } else {
            for (Object id : ids.get().values()) targets.add(find(entries, id).blockEntity());
        }
        boolean all = !targets.isEmpty();
        for (BlockEntity be : targets) all &= PropulsionCoreBehaviour.setEnabled(be, enabled);
        return all;
    }

    @LuaFunction(mainThread = true)
    public final void release(Optional<String> id) throws LuaException {
        if (id.isEmpty()) {
            releaseAll();
            return;
        }
        Entry entry = find(byId(ship()), id.get());
        warmStart.remove(entry.id());
        if (controlled.remove(entry.blockEntity())) release(entry.blockEntity());
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> allocate(IArguments args) throws LuaException {
        ShipContext ship = ship();
        Frame frame = LuaValues.frame(args.optString(2), Frame.LOCAL);
        Vector3d force = args.get(0) == null ? new Vector3d() : LuaValues.readVector(args, 0);
        Vector3d torque = args.get(1) == null ? new Vector3d() : LuaValues.readVector(args, 1);
        if (frame == Frame.WORLD) {
            force = ship.rotateToLocal(force);
            torque = ship.rotateToLocal(torque);
        }
        if (!withinLimit(force) || !withinLimit(torque)) throw new LuaException("force/torque is too large");

        List<Entry> entries = scan(ship).stream().filter(e -> e.maxPn() > 0).toList();
        double[] solution = solve(ship, entries, force, torque);
        for (double value : solution) {
            if (!Double.isFinite(value)) throw new LuaException("Could not solve for a finite allocation");
        }
        warmStart.clear();

        Map<String, Object> thrusts = new HashMap<>();
        Vector3d achievedForce = new Vector3d();
        Vector3d achievedTorque = new Vector3d();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            applyThrottle(e, solution[i] / e.maxPn());
            thrusts.put(e.id(), solution[i]);
            warmStart.put(e.id(), solution[i]);
            achievedForce.fma(solution[i], e.direction());
            achievedTorque.fma(solution[i], e.torquePerPn(ship));
        }
        return Map.of("thrusts", thrusts, "force", inFrame(ship, achievedForce, frame),
            "torque", inFrame(ship, achievedTorque, frame));
    }

    private static boolean withinLimit(Vector3d v) {
        return Math.abs(v.x) <= MAX_ALLOCATION_INPUT && Math.abs(v.y) <= MAX_ALLOCATION_INPUT
            && Math.abs(v.z) <= MAX_ALLOCATION_INPUT;
    }

    private double[] solve(ShipContext ship, List<Entry> entries, Vector3d force, Vector3d torque) {
        int n = entries.size();
        double[][] columns = new double[n][];
        double[] norms = new double[n];
        double[] u = new double[n];
        double maxNorm = 0;
        for (int i = 0; i < n; i++) {
            Vector3d d = entries.get(i).direction();
            Vector3d t = entries.get(i).torquePerPn(ship);
            columns[i] = new double[] {d.x, d.y, d.z, t.x, t.y, t.z};
            norms[i] = d.lengthSquared() + t.lengthSquared();
            maxNorm = Math.max(maxNorm, norms[i]);
        }
        double damping = 1.0e-6 * Math.max(maxNorm, 1.0);

        double[] residual = {-force.x, -force.y, -force.z, -torque.x, -torque.y, -torque.z};
        for (int i = 0; i < n; i++) {
            Double previous = warmStart.get(entries.get(i).id());
            if (previous == null || !Double.isFinite(previous)) continue;
            u[i] = Math.clamp(previous, 0.0, entries.get(i).maxPn());
            for (int k = 0; k < 6; k++) residual[k] += columns[i][k] * u[i];
        }
        int sweeps = (int) Math.clamp(ALLOCATION_WORK / Math.max(n, 1), 1L, (long) ALLOCATION_SWEEPS);
        for (int sweep = 0; sweep < sweeps; sweep++) {
            double change = 0;
            for (int i = 0; i < n; i++) {
                double[] a = columns[i];
                double gradient = damping * u[i];
                for (int k = 0; k < 6; k++) gradient += a[k] * residual[k];
                double max = entries.get(i).maxPn();
                double delta = Math.clamp(u[i] - gradient / (norms[i] + damping), 0.0, max) - u[i];
                if (delta == 0) continue;
                for (int k = 0; k < 6; k++) residual[k] += a[k] * delta;
                u[i] += delta;
                change = Math.max(change, Math.abs(delta) / max);
            }
            if (change < 1.0e-7) break;
        }
        return u;
    }

    private void applyThrottle(Entry e, double throttle) throws LuaException {
        if (!Double.isFinite(throttle)) throw new LuaException("Thrust must be a finite number");
        if (e.blockEntity() instanceof AbstractThrusterBlockEntity thruster) {
            ThrusterComputerHelpers.setThrottleNormalized(thruster, throttle);
        } else if (e.blockEntity() instanceof RcsThrusterBlockEntity rcs) {
            rcs.setNozzleOverride(e.nozzle(), (float) Math.clamp(throttle, 0.0, 1.0));
        }
        controlled.add(e.blockEntity());
    }

    private void releaseAll() {
        warmStart.clear();
        for (BlockEntity be : controlled) release(be);
        controlled.clear();
    }

    private static void release(BlockEntity be) {
        if (be.isRemoved()) return;
        if (be instanceof AbstractThrusterBlockEntity thruster && !hasPeripheralComputer(thruster.computerBehaviour)) {
            ThrusterComputerHelpers.returnToRedstone(thruster);
        } else if (be instanceof RcsThrusterBlockEntity rcs && !hasPeripheralComputer(rcs.computerBehaviour)) {
            rcs.clearOverrides();
        }
    }

    private static boolean hasPeripheralComputer(@Nullable AbstractComputerBehaviour behaviour) {
        return behaviour != null && behaviour.hasAttachedComputer();
    }
}
