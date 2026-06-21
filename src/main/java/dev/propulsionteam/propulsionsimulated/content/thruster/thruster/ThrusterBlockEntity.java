package dev.propulsionteam.propulsionsimulated.content.thruster.thruster;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.FluidThrusterProperties;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterParticleType;
import dev.propulsionteam.propulsionsimulated.particles.smoke.ThrusterSmokeParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.ryanhcode.sable.Sable;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;

public class ThrusterBlockEntity extends AbstractThrusterBlockEntity {
    public static final int BASE_MAX_THRUST = 600000;
    public static final int MAX_WIDTH = 3;

    public SmartFluidTankBehaviour tank;
    public SmartFluidTankBehaviour oxidizerTank;

    @Nullable
    protected BlockPos controllerPos;
    protected boolean updateConnectivity = true;
    protected double lastConsumedMbPerTick = 0.0d;
    protected double lastOxidizerConsumedMbPerTick = 0.0d;
    protected double fuelDrainAccumulator = 0.0d;
    protected double oxidizerDrainAccumulator = 0.0d;
    // Ticks to skip multiblock-validity checks after a sublevel move to tolerate transient invalidity.
    private static final int DISASSEMBLY_GRACE_TICKS = 5;
    private int disassemblyCooldown = 0;

    public ThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ThrusterBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        tank = SmartFluidTankBehaviour.single(this, getBaseTankCapacityMb());
        behaviours.add(tank);
        tank.getPrimaryHandler().setValidator(stack -> ThrusterFuelManager.getProperties(stack.getFluid()) != null);
        
        if (supportsMultiblock()) {
            oxidizerTank = SmartFluidTankBehaviour.single(this, getBaseTankCapacityMb());
            behaviours.add(oxidizerTank);
            oxidizerTank.getPrimaryHandler().setValidator(stack -> stack.getFluid() == dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids.OXIDIZER.get() || stack.getFluid() == dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids.FLOWING_OXIDIZER.get());
        }
    }

    public boolean isMultiblock() {
        return width > 1;
    }

    public boolean isController() {
        return controllerPos == null;
    }

    @Override
    public String getDyeId() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                return ctrl.dyeId;
            }
        }
        return super.getDyeId();
    }

    @Override
    public void setDyeId(String id) {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                ctrl.setDyeId(id);
                return;
            }
        }
        super.setDyeId(id);
    }

    @Nullable
    public ThrusterBlockEntity getControllerBE() {
        if (isController() || !hasLevel()) return this;
        BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,controllerPos);
        return be instanceof ThrusterBlockEntity t ? t : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (!supportsMultiblock()) {
            if (isMultiblock()) {
                width = 1;
                controllerPos = null;
            }
            return;
        }
        if (isController() && isMultiblock()) {
            if (disassemblyCooldown > 0) {
                disassemblyCooldown--;
            } else if (!SimulatedThrustAdapter.isOutsideWorldBuildHeight(level, worldPosition)) {
                // Fix: Skip multiblock validation when outside build height to prevent disassembly.
                Direction facing = getFacing();
                if (!isValidFormedCube(worldPosition, width, facing)) {
                    disassembleMulti();
                    return;
                }
            }
        }
        if (updateConnectivity) {
            updateConnectivity = false;
            if (isController() && !isMultiblock()) {
                tryAssemble();
            }
        }
    }

    protected void tryAssemble() {
        Direction facing = getBlockState().getValue(AbstractThrusterBlock.FACING);
        for (int size = MAX_WIDTH; size >= 2; size--) {
            BlockPos origin = findCubeOrigin(size, facing);
            if (origin != null) {
                formMulti(origin, size, facing);
                return;
            }
        }
    }

    @Nullable
    protected BlockPos findCubeOrigin(int size, Direction facing) {
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    BlockPos origin = worldPosition.offset(-dx, -dy, -dz);
                    if (isValidCube(origin, size, facing)) return origin;
                }
            }
        }
        return null;
    }

    protected boolean isValidCube(BlockPos origin, int size, Direction facing) {
        if (level == null) return false;
        BlockState originState = SimulatedThrustAdapter.getBlockStateSafe(level, origin);
        Block expectedBlock = originState.getBlock();
        Class<?> expectedType = null;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = SimulatedThrustAdapter.getBlockStateSafe(level,pos);
                    if (!state.is(expectedBlock)) return false;
                    if (!state.hasProperty(AbstractThrusterBlock.FACING)) return false;
                    if (state.getValue(AbstractThrusterBlock.FACING) != facing) return false;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,pos);
                    if (!(be instanceof ThrusterBlockEntity t)) return false;
                    if (expectedType == null) {
                        expectedType = t.getClass();
                    } else if (t.getClass() != expectedType) {
                        // Prevent mixed thruster families (e.g. normal + ion) from assembling.
                        return false;
                    }
                    if (!t.supportsMultiblock()) return false;
                    if (t.isMultiblock() && t.width >= size) return false;
                }
            }
        }
        return true;
    }

    protected void formMulti(BlockPos origin, int size, Direction facing) {
        if (level == null) return;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,origin.offset(x, y, z));
                    if (be instanceof ThrusterBlockEntity t && t.isMultiblock()) {
                        ThrusterBlockEntity ctrl = t.getControllerBE();
                        if (ctrl != null) ctrl.disassembleMulti();
                    }
                }
            }
        }

        List<ThrusterBlockEntity> members = new ArrayList<>(size * size * size);
        ThrusterBlockEntity controller = null;
        FluidStack totalFuel = FluidStack.EMPTY;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,pos);
                    if (!(be instanceof ThrusterBlockEntity t)) return;
                    members.add(t);
                    if (pos.equals(origin)) controller = t;
                    if (t.tank != null) {
                        totalFuel = mergeFluid(totalFuel, t.tank.getPrimaryHandler().getFluid());
                        t.tank.getPrimaryHandler().setFluid(FluidStack.EMPTY);
                    }
                }
            }
        }
        if (controller == null) return;

        int newCap = getBaseTankCapacityMb() * size * size * size;
        if (controller.tank != null) {
            controller.tank.getPrimaryHandler().setCapacity(newCap);
            controller.tank.getPrimaryHandler().setFluid(trimToCapacity(totalFuel, newCap));
        }

        for (ThrusterBlockEntity t : members) {
            t.controllerPos = (t == controller) ? null : origin;
            t.width = size;
            t.isThrustDirty = true;
            BlockPos cellPos = t.getBlockPos();
            BlockState liveState = SimulatedThrustAdapter.getBlockStateSafe(level,cellPos);
            if (liveState.hasProperty(ThrusterBlock.MULTIBLOCK)
                && !liveState.getValue(ThrusterBlock.MULTIBLOCK)) {
                level.setBlock(cellPos, liveState.setValue(ThrusterBlock.MULTIBLOCK, true), Block.UPDATE_CLIENTS);
            }
            t.setChanged();
            t.notifyUpdate();
        }
        controller.calculateObstruction(level, origin, facing);
    }

    public void disassembleMulti() {
        if (!isController() || !isMultiblock() || level == null) return;
        int size = width;
        BlockPos origin = worldPosition;

        FluidStack fuelPool = FluidStack.EMPTY;
        if (tank != null) {
            fuelPool = tank.getPrimaryHandler().getFluid().copy();
            tank.getPrimaryHandler().setFluid(FluidStack.EMPTY);
            tank.getPrimaryHandler().setCapacity(getBaseTankCapacityMb());
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,pos);
                    if (!(be instanceof ThrusterBlockEntity t)) continue;
                    if (t.tank != null && !fuelPool.isEmpty()) {
                        int take = Math.min(getBaseTankCapacityMb(), fuelPool.getAmount());
                        FluidStack slice = new FluidStack(fuelPool.getFluid(), take);
                        t.tank.getPrimaryHandler().fill(slice, IFluidHandler.FluidAction.EXECUTE);
                        fuelPool.shrink(take);
                    }
                    t.controllerPos = null;
                    t.width = 1;
                    t.updateConnectivity = true;
                    t.isThrustDirty = true;
                    t.thrusterData.setThrust(0);
                    BlockState liveState = SimulatedThrustAdapter.getBlockStateSafe(level,pos);
                    if (liveState.hasProperty(ThrusterBlock.MULTIBLOCK)
                        && liveState.getValue(ThrusterBlock.MULTIBLOCK)) {
                        level.setBlock(pos, liveState.setValue(ThrusterBlock.MULTIBLOCK, false), Block.UPDATE_CLIENTS);
                    }
                    t.setChanged();
                    t.notifyUpdate();
                }
            }
        }
    }

    private static FluidStack mergeFluid(FluidStack pool, FluidStack addition) {
        if (addition.isEmpty()) return pool;
        if (pool.isEmpty()) return addition.copy();
        if (FluidStack.isSameFluidSameComponents(pool, addition)) {
            FluidStack out = pool.copy();
            out.grow(addition.getAmount());
            return out;
        }
        return pool;
    }

    private static FluidStack trimToCapacity(FluidStack stack, int cap) {
        if (stack.isEmpty() || stack.getAmount() <= cap) return stack;
        FluidStack out = stack.copy();
        out.setAmount(cap);
        return out;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        ThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
        if (ctrl == null) return null;
        
        IFluidHandler fuel = ctrl.tank.getPrimaryHandler();
        IFluidHandler ox = (ctrl.oxidizerTank != null) ? ctrl.oxidizerTank.getPrimaryHandler() : null;

        if (!ctrl.isMultiblock()) {
            if (side == null || side == getFluidCapSide()) {
                return fuel;
            }
            return null;
        }
        
        if (side == null) {
            return (ox != null) ? new dev.propulsionteam.propulsionsimulated.utility.MultiFluidHandler(fuel, ox) : fuel;
        }
        
        if (!isFrontLayerCell(ctrl, ctrl.getBlockState().getValue(AbstractThrusterBlock.FACING))) return null;
        
        if (side == ctrl.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite()) {
            return null;
        }
        
        return (ox != null) ? new dev.propulsionteam.propulsionsimulated.utility.MultiFluidHandler(fuel, ox) : fuel;
    }

    protected boolean supportsMultiblock() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (isController() && isMultiblock()) {
            double extra = 32.0d;
            return new AABB(
                    worldPosition.getX() - extra,
                    worldPosition.getY() - extra,
                    worldPosition.getZ() - extra,
                    worldPosition.getX() + width + extra,
                    worldPosition.getY() + width + extra,
                    worldPosition.getZ() + width + extra
            );
        }

        double extra = 16.0d;
        return new AABB(
                worldPosition.getX() - extra,
                worldPosition.getY() - extra,
                worldPosition.getZ() - extra,
                worldPosition.getX() + 1.0d + extra,
                worldPosition.getY() + 1.0d + extra,
                worldPosition.getZ() + 1.0d + extra
        );
    }

    private boolean isFrontLayerCell(ThrusterBlockEntity ctrl, Direction cubeFacing) {
        BlockPos origin = ctrl.worldPosition;
        int size = ctrl.width;
        int rel;
        switch (cubeFacing.getAxis()) {
            case X -> rel = worldPosition.getX() - origin.getX();
            case Y -> rel = worldPosition.getY() - origin.getY();
            case Z -> rel = worldPosition.getZ() - origin.getZ();
            default -> {
                return false;
            }
        }
        int frontIdx = cubeFacing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? size - 1 : 0;
        return rel == frontIdx;
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        super.afterMove(oldLevel, newLevel, state, oldPos, newPos);
        // On any assembly/disassembly event, wipe this block's multiblock state so that
        // tryAssemble() can rebuild it cleanly once all blocks have settled in their new
        // positions. Attempting to translate controllerPos through rotation doesn't work
        // because rotation changes relative offsets between members.
        if (isMultiblock()) {
            width = 1;
            controllerPos = null;
            updateConnectivity = true;
            isThrustDirty = true;
            setChanged();
        }
        // Grace period: delay validation and reassembly until after all blocks have been
        // moved. Each afterMove call resets the counter, so the final afterMove determines
        // when reassembly actually runs.
        disassemblyCooldown = DISASSEMBLY_GRACE_TICKS;
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        if (!isController()) {
            isThrustDirty = false;
            return;
        }
        if (isMultiblock()) {
            updateMultiThrust(currentBlockState);
        } else {
            updateSingleThrust(currentBlockState);
        }
    }

    protected void updateSingleThrust(BlockState currentBlockState) {
        final double prevConsumedMbPerTick = lastConsumedMbPerTick;
        final int prevFuelAmount = tank != null ? tank.getPrimaryHandler().getFluidAmount() : 0;
        float thrust = 0;
        float currentPower = getPower();
        lastConsumedMbPerTick = 0.0d;
        if (isWorking() && currentPower > 0) {
            FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(currentPower, obstructionEffect);

            if (thrustPercentage > 0 && properties != null) {
                final int tickRate = 10;
                double requestedConsumption = calculateFuelConsumption(currentPower, properties.consumptionMultiplier(), tickRate);
                int consumption = consumeFuelWithAccumulator(requestedConsumption);
                FluidStack drainedStack = tank.getPrimaryHandler().drain(consumption, IFluidHandler.FluidAction.EXECUTE);
                int fuelConsumed = drainedStack.getAmount();

                if (fuelConsumed > 0 || (consumption == 0 && !fluidStack().isEmpty())) {
                    // Keep thrust continuous for sub-1 mB windows: accumulator-based drain can
                    // legitimately round to 0 for this update while fuel is still available.
                    float consumptionRatio = consumption > 0
                        ? (float) fuelConsumed / (float) consumption
                        : 1.0f;
                    float fuelEfficiency = ThrusterFuelManager.getEfficiency(fluidStack().getFluid());
                    float baseThrustPn = (float) (getBaseThrust() * getThrustUnitsPerKn());
                    baseThrustPn *= (float) calculateAtmosphericFactor();
                    thrust = baseThrustPn * thrustPercentage * properties.thrustMultiplier() * fuelEfficiency * consumptionRatio;
                    lastConsumedMbPerTick = (double) fuelConsumed / (double) tickRate;
                }
            }
        }
        setThrustAndSync(thrust);
        if (didSingleTooltipTelemetryChange(prevConsumedMbPerTick, prevFuelAmount)) {
            setChanged();
            notifyUpdate();
        }
        isThrustDirty = false;
    }

    protected int consumeOxidizerWithAccumulator(double requestedAmount) {
        oxidizerDrainAccumulator += requestedAmount;
        int consumption = (int) Math.floor(oxidizerDrainAccumulator);
        if (consumption > 0) {
            oxidizerDrainAccumulator -= consumption;
        }
        return consumption;
    }

    protected void updateMultiThrust(BlockState currentBlockState) {
        final double prevConsumedMbPerTick = lastConsumedMbPerTick;
        final double prevOxidizerConsumedMbPerTick = lastOxidizerConsumedMbPerTick;
        final int prevFuelAmount = tank != null ? tank.getPrimaryHandler().getFluidAmount() : 0;
        final int prevOxidizerAmount = oxidizerTank != null ? oxidizerTank.getPrimaryHandler().getFluidAmount() : 0;
        int n = width * width * width;
        float totalThrust = 0;
        float currentPower = getPower();
        lastConsumedMbPerTick = 0.0d;
        lastOxidizerConsumedMbPerTick = 0.0d;

        if (isWorking() && currentPower > 0) {
            FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(currentPower, obstructionEffect);
            if (thrustPercentage > 0 && properties != null) {
                final int tickRate = 10;
                double baseConsumption = calculateFuelConsumption(currentPower, properties.consumptionMultiplier(), tickRate);
                
                boolean canUseOxidizer = validOxidizer();
                // Multiblock fuel efficiency always applies; oxidizer adds an extra multiplier.
                double fuelEff = getMultiblockFuelEfficiency(width);
                if (canUseOxidizer) {
                    fuelEff *= getMultiblockOxidizerEfficiency(width);
                }
                double fuelNeededDouble = baseConsumption * (double) n * fuelEff;
                int fuelNeeded = consumeFuelWithAccumulator(fuelNeededDouble);

                FluidStack fuelSim = tank.getPrimaryHandler().drain(fuelNeeded, IFluidHandler.FluidAction.SIMULATE);
                int fuelAvail = fuelSim.getAmount();
                float fuelRatio = fuelNeeded > 0
                    ? (float) fuelAvail / (float) fuelNeeded
                    : (fluidStack().isEmpty() ? 0.0f : 1.0f);

                if (fuelRatio > 0) {
                    int fuelActual = (int) (fuelNeeded * fuelRatio);
                    tank.getPrimaryHandler().drain(fuelActual, IFluidHandler.FluidAction.EXECUTE);
                    
                    if (canUseOxidizer) {
                        double oxNeededDouble = baseConsumption * (double) n * getMultiblockOxidizerEfficiency(width);
                        int oxToDrain = consumeOxidizerWithAccumulator(oxNeededDouble * fuelRatio);
                        if (oxToDrain > 0) {
                            oxidizerTank.getPrimaryHandler().drain(oxToDrain, IFluidHandler.FluidAction.EXECUTE);
                        }
                        lastOxidizerConsumedMbPerTick = (double) oxToDrain / (double) tickRate;
                    }

                    float fuelEfficiency = ThrusterFuelManager.getEfficiency(fluidStack().getFluid());
                    float baseThrustPn = (float) (getBaseThrust() * getThrustUnitsPerKn());
                    baseThrustPn *= (float) calculateAtmosphericFactor();
                    float oxidizerThrustMultiplier = canUseOxidizer ? getMultiblockOxidizerThrustMultiplier(width) : 1.0f;
                    totalThrust = baseThrustPn * thrustPercentage * properties.thrustMultiplier() * fuelEfficiency * fuelRatio * n * getMultiblockThrustMultiplier(width) * oxidizerThrustMultiplier;
                    lastConsumedMbPerTick = (double) fuelActual / (double) tickRate;
                }
            }
        }

        // Controller holds the total thrust; non-members get 0 so they skip physics and sound.
        setThrustAndSync(totalThrust);
        if (didMultiTooltipTelemetryChange(prevConsumedMbPerTick, prevOxidizerConsumedMbPerTick, prevFuelAmount, prevOxidizerAmount)) {
            setChanged();
            notifyUpdate();
        }
        BlockPos origin = worldPosition;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                for (int z = 0; z < width; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (be instanceof ThrusterBlockEntity t) {
                        t.getThrusterData().setThrust(0);
                        t.isThrustDirty = false;
                        t.lastConsumedMbPerTick = this.lastConsumedMbPerTick;
                        t.lastOxidizerConsumedMbPerTick = this.lastOxidizerConsumedMbPerTick;
                    }
                }
            }
        }
        isThrustDirty = false;
    }

    private boolean didSingleTooltipTelemetryChange(double prevConsumedMbPerTick, int prevFuelAmount) {
        if (java.lang.Math.abs(lastConsumedMbPerTick - prevConsumedMbPerTick) > 1e-4d) {
            return true;
        }
        int fuelAmountNow = tank != null ? tank.getPrimaryHandler().getFluidAmount() : 0;
        return fuelAmountNow != prevFuelAmount;
    }

    private boolean didMultiTooltipTelemetryChange(double prevConsumedMbPerTick, double prevOxidizerConsumedMbPerTick,
                                                   int prevFuelAmount, int prevOxidizerAmount) {
        if (java.lang.Math.abs(lastConsumedMbPerTick - prevConsumedMbPerTick) > 1e-4d) {
            return true;
        }
        if (java.lang.Math.abs(lastOxidizerConsumedMbPerTick - prevOxidizerConsumedMbPerTick) > 1e-4d) {
            return true;
        }
        int fuelAmountNow = tank != null ? tank.getPrimaryHandler().getFluidAmount() : 0;
        if (fuelAmountNow != prevFuelAmount) {
            return true;
        }
        int oxidizerAmountNow = oxidizerTank != null ? oxidizerTank.getPrimaryHandler().getFluidAmount() : 0;
        return oxidizerAmountNow != prevOxidizerAmount;
    }

    @Override
    public void calculateObstruction(net.minecraft.world.level.Level lvl, BlockPos pos, Direction forwardDirection) {
        if (!isController() && isMultiblock()) {
            return;
        }
        if (isController() && isMultiblock()) {
            super.calculateObstruction(lvl, worldPosition, getBlockState().getValue(AbstractThrusterBlock.FACING));
            return;
        }
        super.calculateObstruction(lvl, pos, forwardDirection);
    }

    @Override
    protected float calculateObstructionEffect() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl != null ? ctrl.calculateObstructionEffect() : 0f;
        }
        return super.calculateObstructionEffect();
    }

    @Override
    public int getEmptyBlocks() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl != null ? ctrl.getEmptyBlocks() : 0;
        }
        return super.getEmptyBlocks();
    }

    private Vec3 getMultiblockCenterNozzlePositionLocal() {
        Direction exhaustDirection = getFacing().getOpposite();
        Vec3 localExhaustDirection = new Vec3(exhaustDirection.getStepX(), exhaustDirection.getStepY(), exhaustDirection.getStepZ());
        double half = width * 0.5d;
        Vec3 localCubeCenter = new Vec3(
            worldPosition.getX() + half,
            worldPosition.getY() + half,
            worldPosition.getZ() + half
        );
        return localCubeCenter.add(localExhaustDirection.scale(half + 0.45d));
    }

    @Override
    public Vec3 getParticleDebugNozzlePositionLocal() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl != null ? ctrl.getParticleDebugNozzlePositionLocal() : super.getParticleDebugNozzlePositionLocal();
        }
        if (isController() && isMultiblock()) {
            return getMultiblockCenterNozzlePositionLocal();
        }
        return super.getParticleDebugNozzlePositionLocal();
    }

    @Override
    public WorldExhaustRay getWorldExhaustRay() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl != null ? ctrl.getWorldExhaustRay() : super.getWorldExhaustRay();
        }
        if (!(isController() && isMultiblock()) || level == null) {
            return super.getWorldExhaustRay();
        }

        Direction exhaustDirection = getFacing().getOpposite();
        Vec3 localNozzle = getMultiblockCenterNozzlePositionLocal();
        Vector3d localNozzleVec = new Vector3d(localNozzle.x, localNozzle.y, localNozzle.z);
        Vector3d localExhaustVec = new Vector3d(exhaustDirection.getStepX(), exhaustDirection.getStepY(), exhaustDirection.getStepZ()).normalize();
        SimulatedThrustAdapter.Projection projection = SimulatedThrustAdapter.projectToWorld(level, worldPosition, localNozzleVec, localExhaustVec);

        Vec3 worldDirection = projection.direction();
        if (worldDirection.lengthSqr() < MathUtility.epsilon) {
            worldDirection = new Vec3(localExhaustVec.x, localExhaustVec.y, localExhaustVec.z);
        } else {
            worldDirection = worldDirection.normalize();
        }
        return new WorldExhaustRay(projection.level(), projection.position(), worldDirection);
    }

    @Override
    protected boolean shouldDamageEntities() {
        if (isMultiblock() && !isController()) {
            return false;
        }
        return super.shouldDamageEntities();
    }

    @Override
    public void sable$physicsTick(final dev.ryanhcode.sable.sublevel.ServerSubLevel subLevel, final dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle handle, final double timeStep) {
        if (isMultiblock() && !isController()) return;
        if (isMultiblock()) {
            float thrust = getCurrentThrust();
            if (thrust <= 0.0f || !Float.isFinite(thrust)) return;
            Vector3d directionLocal = new Vector3d(getThrustDirectionLocal()).normalize();
            Vec3 centerNozzle = getMultiblockCenterNozzlePositionLocal();
            Vector3d applicationPoint = new Vector3d(centerNozzle.x, centerNozzle.y, centerNozzle.z);
            Vector3d impulseLocal = new Vector3d(directionLocal).mul(thrust * timeStep);
            Vector3d adjustedImpulse = new Vector3d(impulseLocal).div(getThrustUnitsPerKn());
            SimulatedThrustAdapter.applyImpulseAtPoint(subLevel, applicationPoint, adjustedImpulse);
            return;
        }
        super.sable$physicsTick(subLevel, handle, timeStep);
    }

    private static float getMultiblockFuelEfficiency(int cubeWidth) {
        if (cubeWidth <= 1) return 1.0f;
        if (cubeWidth == 2) return PropulsionConfig.MULTIBLOCK_2X_FUEL_EFFICIENCY.get().floatValue();
        if (cubeWidth == 3) return PropulsionConfig.MULTIBLOCK_3X_FUEL_EFFICIENCY.get().floatValue();
        return 1.0f;
    }

    private static float getMultiblockOxidizerEfficiency(int cubeWidth) {
        if (cubeWidth <= 1) return 1.0f;
        if (cubeWidth == 2) return PropulsionConfig.MULTIBLOCK_2X_OXIDIZER_EFFICIENCY.get().floatValue();
        if (cubeWidth == 3) return PropulsionConfig.MULTIBLOCK_3X_OXIDIZER_EFFICIENCY.get().floatValue();
        return 1.0f;
    }

    private static float getMultiblockThrustMultiplier(int cubeWidth) {
        if (cubeWidth == 2) return PropulsionConfig.MULTIBLOCK_2X_THRUST_MULTIPLIER.get().floatValue();
        if (cubeWidth == 3) return PropulsionConfig.MULTIBLOCK_3X_THRUST_MULTIPLIER.get().floatValue();
        return 1.0f;
    }

    private static float getMultiblockOxidizerThrustMultiplier(int cubeWidth) {
        float oxidizerEfficiency = getMultiblockOxidizerEfficiency(cubeWidth);
        if (oxidizerEfficiency <= 0.0f) return 1.0f;
        return 1.0f / oxidizerEfficiency;
    }

    @Override
    public float getPower() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl != null ? ctrl.getPower() : 0f;
        }
        if (controlMode == ControlMode.PERIPHERAL) {
            return digitalInput;
        }
        if (isController() && isMultiblock()) {
            return getAggregatedRedstone() / 15.0f;
        }
        return redstoneInput / 15.0f;
    }

    private int getAggregatedRedstone() {
        int max = redstoneInput;
        if (level == null) return max;
        BlockPos origin = worldPosition;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                for (int z = 0; z < width; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,origin.offset(x, y, z));
                    if (be instanceof ThrusterBlockEntity t && t.redstoneInput > max) {
                        max = t.redstoneInput;
                    }
                }
            }
        }
        return max;
    }

    @Override
    public void setRedstoneInput(int power) {
        if (this.redstoneInput == power) return;
        this.redstoneInput = power;
        if (controlMode == ControlMode.NORMAL) {
            dirtyThrust();
            notifyUpdate();
        }
        if (isMultiblock() && !isController()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                ctrl.dirtyThrust();
                ctrl.notifyUpdate();
            }
        }
    }

    protected boolean validOxidizer() {
        return oxidizerTank != null && !oxidizerTank.isEmpty() && oxidizerTank.getPrimaryHandler().getFluidAmount() > 0;
    }

    @Override
    protected boolean isWorking() {
        return validFluid();
    }

    @Override
    public boolean shouldEmitParticles() {
        if (!super.shouldEmitParticles()) {
            return false;
        }
        if (isMultiblock()) {
            // Treat assembled cubes as one engine effect: only controller emits.
            if (!isController()) return false;
            if (calculateObstructionEffect() <= 0f) return false;
        }
        FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
        return properties != null && properties.particleType() != ThrusterParticleType.NONE;
    }

    @Override
    public void emitParticles(Level level, BlockPos pos, BlockState state) {
        if (!isController() && isMultiblock()) return;
        if (!validFluid()) return;

        float power = getPower();
        if (power <= 0.035f) return;
        if (getEmptyBlocks() <= 0) return;

        WorldExhaustRay ray = getWorldExhaustRay();
        if (ray == null) return;

        Level particleLevel = ray.level();
        Vec3 nozzle = ray.nozzlePos();
        Vec3 direction = ray.direction();

        if (particleLevel == null) return;
        if (direction.lengthSqr() < MathUtility.epsilon) return;

        direction = direction.normalize();

        int w = isMultiblock() ? width : 1;

        double particleCountMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleCountMultiplier());
        if (particleCountMultiplier <= 0) return;

        double particleVelocityMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleVelocityMultiplier());

        double smokeStart = switch (w) {
            case 1 -> 7.7d;
            case 2 -> 11.4d;
            default -> 16.0d;
        };

        double streamLength = switch (w) {
            case 1 -> 1.15d;
            case 2 -> 1.75d;
            default -> 2.6d;
        };

        double streamRadius = switch (w) {
            case 1 -> 0.28d;
            case 2 -> 0.58d;
            default -> 1.05d;
        };

        double particleSpeed = switch (w) {
            case 1 -> 0.72d;
            case 2 -> 0.95d;
            default -> 1.25d;
        };

        float baseScale = switch (w) {
            case 1 -> 2.6f;
            case 2 -> 3.8f;
            default -> 5.4f;
        };

        int baseLifetime = switch (w) {
            case 1 -> 48;
            case 2 -> 58;
            default -> 72;
        };

        int particlesToSpawn = switch (w) {
            case 1 -> Math.max(2, (int) java.lang.Math.round((2.0d + power * 3.0d) * particleCountMultiplier));
            case 2 -> Math.max(4, (int) java.lang.Math.round((4.0d + power * 5.0d) * particleCountMultiplier));
            default -> Math.max(7, (int) java.lang.Math.round((7.0d + power * 7.0d) * particleCountMultiplier));
        };

        Vec3 up = Math.abs(direction.y) < 0.92d ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = direction.cross(up).normalize();
        Vec3 localUp = right.cross(direction).normalize();

        for (int i = 0; i < particlesToSpawn; i++) {
            double beamFrac = particlesToSpawn <= 1 ? 0.0d : (double) i / (double) particlesToSpawn;

            double burst = java.lang.Math.pow(level.random.nextDouble(), 2.2d);
            double angle = level.random.nextDouble() * java.lang.Math.PI * 2.0d;

            double spawnSpread = streamRadius * (0.18d + burst * 0.38d);
            Vec3 radial = right.scale(java.lang.Math.cos(angle) * spawnSpread)
                    .add(localUp.scale(java.lang.Math.sin(angle) * spawnSpread));

            double along = smokeStart
                    + streamLength * beamFrac * 0.28d
                    + level.random.nextDouble() * 0.35d;

            Vec3 spawn = nozzle
                    .add(direction.scale(along))
                    .add(radial);

            double punch = switch (w) {
                case 1 -> 2.35d;
                case 2 -> 2.85d;
                default -> 3.35d;
            };

            double speedRandom = 0.95d + level.random.nextDouble() * 0.55d;

            Vec3 outward = radial.lengthSqr() > 0.0001d
                    ? radial.normalize().scale((0.018d + level.random.nextDouble() * 0.035d) * w)
                    : Vec3.ZERO;

            Vec3 turbulent = right.scale((level.random.nextDouble() - 0.5d) * 0.055d * w)
                    .add(localUp.scale((level.random.nextDouble() - 0.5d) * 0.055d * w));

            Vec3 velocity = direction.scale(particleSpeed * punch * power * speedRandom * particleVelocityMultiplier)
                    .add(outward)
                    .add(turbulent)
                    .add(0.0d, 0.004d + level.random.nextDouble() * 0.010d, 0.0d);

            float sizeRandom = 0.72f + level.random.nextFloat() * 0.76f;
            float distanceBoost = 0.85f + (float) beamFrac * 0.45f;
            float scale = baseScale * sizeRandom * distanceBoost;

            int lifetime = baseLifetime + level.random.nextInt(18);

            float heat = (float) (1.0d - beamFrac);
            float r = 0.20f + heat * 0.08f;
            float g = 0.20f + heat * 0.05f;
            float b = 0.23f + heat * 0.03f;

            ThrusterSmokeParticleData particle = new ThrusterSmokeParticleData(
                    scale,
                    lifetime,
                    r,
                    g,
                    b
            );

            if (particleLevel instanceof ServerLevel serverLevel) {
                double maxDistSq = PARTICLE_BROADCAST_RANGE_BLOCKS * PARTICLE_BROADCAST_RANGE_BLOCKS;

                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(spawn.x, spawn.y, spawn.z) > maxDistSq) continue;

                    serverLevel.sendParticles(
                            player,
                            particle,
                            true,
                            spawn.x,
                            spawn.y,
                            spawn.z,
                            0,
                            velocity.x,
                            velocity.y,
                            velocity.z,
                            1.0d
                    );
                }
            } else {
                particleLevel.addParticle(
                        particle,
                        true,
                        spawn.x,
                        spawn.y,
                        spawn.z,
                        velocity.x,
                        velocity.y,
                        velocity.z
                );
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null && ctrl != this) {
                return ctrl.addToGoggleTooltip(tooltip, isPlayerSneaking);
            }
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    private boolean isValidFormedCube(BlockPos origin, int size, Direction facing) {
        if (level == null) return false;
        Block expectedBlock = getBlockState().getBlock();
        Class<?> expectedType = getClass();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = SimulatedThrustAdapter.getBlockStateSafe(level,pos);
                    if (!state.is(expectedBlock)) return false;
                    if (!state.hasProperty(AbstractThrusterBlock.FACING) || state.getValue(AbstractThrusterBlock.FACING) != facing) return false;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level,pos);
                    if (!(be instanceof ThrusterBlockEntity t)) return false;
                    if (t.getClass() != expectedType) return false;
                    ThrusterBlockEntity ctrl = t.getControllerBE();
                    if (ctrl == null || ctrl != this) return false;
                    if (t.width != size) return false;
                }
            }
        }
        return true;
    }

    @Override
    protected double getBaseThrust() {
        return PropulsionConfig.BASE_THRUST.get();
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.BASE_THRUST.get();
    }

    public Direction getFluidCapSide() {
        return getBlockState().getValue(ThrusterBlock.FACING);
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return 0.95;
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (fluidStack().isEmpty()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_fuel")).style(ChatFormatting.RED);
        } else if (!validFluid()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.wrong_fuel")).style(ChatFormatting.RED);
        } else if (!isPowered()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered")).style(ChatFormatting.GOLD);
        } else if (getEmptyBlocks() == 0) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).style(ChatFormatting.RED);
        } else {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working")).style(ChatFormatting.GREEN);
        }
    }

    @Override
    public double getDisplayedThrustPnForTooltip() {
        if (isMultiblock()) {
            ThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
            if (ctrl == null) return super.getDisplayedThrustPnForTooltip();
            return ctrl.getThrusterData().getThrust();
        }
        return super.getDisplayedThrustPnForTooltip();
    }
    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        ThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
        if (ctrl == null) return;

        if (ctrl.isMultiblock()) {
            // --- Efficiency bonus ---
            float fuelEff = getMultiblockFuelEfficiency(ctrl.width);
            float oxEff   = getMultiblockOxidizerEfficiency(ctrl.width);
            int fuelSavePct = java.lang.Math.round((1.0f - fuelEff) * 100.0f);
            int oxSavePct   = java.lang.Math.round((1.0f - oxEff)   * 100.0f);
            if (fuelSavePct > 0 || oxSavePct > 0) {
                boolean hasOx = ctrl.validOxidizer();
                // Header line for base multiblock savings (always active for multiblocks).
                CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.bulk_bonus"))
                    .text(":")
                    .space()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.bulk_bonus_active").withStyle(ChatFormatting.GREEN))
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip);

                // Base multiblock fuel savings.
                if (fuelSavePct > 0) {
                    CreateLang.builder()
                        .add(Component.literal("  "))
                        .add(Component.literal("Fuel: ").withStyle(ChatFormatting.GRAY))
                        .add(Component.literal("-" + fuelSavePct + "%").withStyle(ChatFormatting.AQUA))
                        .forGoggles(tooltip);
                }

                // Additional oxidizer-based fuel savings.
                if (oxSavePct > 0) {
                    CreateLang.builder()
                        .add(Component.literal("  "))
                        .add(Component.literal("Oxidizer Bonus: ").withStyle(ChatFormatting.GRAY))
                        .add(hasOx
                            ? Component.literal("-" + oxSavePct + "%").withStyle(ChatFormatting.AQUA)
                            : Component.translatable("createpropulsion.gui.goggles.thruster.bulk_bonus_inactive").withStyle(ChatFormatting.RED))
                        .forGoggles(tooltip);
                }

                float oxThrustMultiplier = getMultiblockOxidizerThrustMultiplier(ctrl.width);
                int thrustBonusPct = java.lang.Math.round((oxThrustMultiplier - 1.0f) * 100.0f);
                if (thrustBonusPct > 0) {
                    CreateLang.builder()
                        .add(Component.literal("  "))
                        .add(Component.translatable("createpropulsion.gui.goggles.thruster.thrust_bonus").withStyle(ChatFormatting.GRAY))
                        .add(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .add(hasOx
                            ? Component.literal("+" + thrustBonusPct + "%").withStyle(ChatFormatting.AQUA)
                            : Component.translatable("createpropulsion.gui.goggles.thruster.bulk_bonus_inactive").withStyle(ChatFormatting.RED))
                        .forGoggles(tooltip);
                }
            }
        }

        if (ctrl.tank == null) return;

        // --- Fuel tank (always shown) ---
        addFluidContainerTooltip(tooltip,
            Component.translatable("createpropulsion.gui.goggles.thruster.fuel_label"),
            ctrl.tank.getPrimaryHandler(), ctrl.lastConsumedMbPerTick);

        // --- Oxidizer tank: multiblock only ---
        if (ctrl.isMultiblock() && ctrl.oxidizerTank != null) {
            addFluidContainerTooltip(tooltip,
                Component.translatable("createpropulsion.gui.goggles.thruster.oxidizer_label"),
                ctrl.oxidizerTank.getPrimaryHandler(), ctrl.lastOxidizerConsumedMbPerTick);
        }
    }

    private void addFluidContainerTooltip(List<Component> tooltip, Component label,
                                          IFluidHandler handler, double consumptionRate) {
        if (handler == null || handler.getTanks() <= 0) return;
        int capacity = handler.getTankCapacity(0);
        FluidStack fluid = handler.getFluidInTank(0);
        int amount = fluid.getAmount();

        // Label line: "Fuel:"
        CreateLang.builder()
            .add(label.copy())
            .style(ChatFormatting.WHITE)
            .forGoggles(tooltip);

        // Storage line: "  100 / 1000 mB"
        CreateLang.builder()
            .add(Component.literal("  "))
            .add(Component.literal(Integer.toString(amount)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
            .add(Component.literal(Integer.toString(capacity)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" mB").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);

        // Consumption line: "  1.5 mB/t"
        CreateLang.builder()
            .add(Component.literal("  "))
            .add(Component.literal(String.format(Locale.ROOT, "%.1f", consumptionRate)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" mB/t").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);
    }
    protected float getFuelEfficiencyMultiplier() {
        FluidStack currentFluid = fluidStack();
        if (currentFluid.isEmpty()) {
            return 1.0f;
        }
        return ThrusterFuelManager.getEfficiency(currentFluid.getFluid());
    }

    public FluidStack fluidStack() {
        ThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
        if (ctrl == null) ctrl = this;
        return ctrl.tank.getPrimaryHandler().getFluid();
    }

    public boolean validFluid() {
        if (fluidStack().isEmpty()) return false;
        return getFuelProperties(fluidStack().getFluid()) != null;
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
        if (properties == null) {
            return super.createParticleOptions();
        }
        FluidThrusterProperties resolvedProperties = properties;
        if (properties.useFluidColor()) {
            int fluidColor = IClientFluidTypeExtensions.of(fluidStack().getFluid()).getTintColor(fluidStack()) & 0xFFFFFF;
            resolvedProperties = new FluidThrusterProperties(
                properties.thrustMultiplier(),
                properties.consumptionMultiplier(),
                properties.particleType(),
                properties.overrideTextures(),
                fluidColor,
                true
            );
        }
        return resolvedProperties.particleType().createParticleOptions(resolvedProperties);
    }

    private double calculateFuelConsumption(float powerPercentage, float fluidPropertiesConsumptionMultiplier, int tickRate) {
        return getFuelConsumptionPerTickAtFullThrottle() * powerPercentage * fluidPropertiesConsumptionMultiplier * tickRate;
    }

    protected double getFuelConsumptionPerTickAtFullThrottle() {
        return PropulsionConfig.FUEL_MB_PER_TICK_AT_FULL_THROTTLE.get();
    }

    private int consumeFuelWithAccumulator(double requestedAmount) {
        if (requestedAmount <= 0.0d) {
            return 0;
        }
        double total = fuelDrainAccumulator + requestedAmount;
        int toConsume = (int) Math.floor(total);
        fuelDrainAccumulator = total - toConsume;
        return Math.max(0, toConsume);
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("Width", width);
        compound.putDouble("LastConsumedMbPerTick", lastConsumedMbPerTick);
        compound.putDouble("LastOxidizerConsumedMbPerTick", lastOxidizerConsumedMbPerTick);
        compound.putDouble("FuelDrainAccumulator", fuelDrainAccumulator);
        compound.putDouble("OxidizerDrainAccumulator", oxidizerDrainAccumulator);
        
        if (tank != null) {
            compound.put("FuelTankSync", tank.getPrimaryHandler().getFluid().saveOptional(registries));
        }
        if (oxidizerTank != null) {
            compound.put("OxidizerTankSync", oxidizerTank.getPrimaryHandler().getFluid().saveOptional(registries));
        }

        if (controllerPos != null) {
            compound.putInt("ControllerOffX", controllerPos.getX() - worldPosition.getX());
            compound.putInt("ControllerOffY", controllerPos.getY() - worldPosition.getY());
            compound.putInt("ControllerOffZ", controllerPos.getZ() - worldPosition.getZ());
        }
        if (updateConnectivity) {
            compound.putBoolean("UpdateConnectivity", true);
        }
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        width = Math.max(1, compound.getInt("Width"));
        lastConsumedMbPerTick = compound.getDouble("LastConsumedMbPerTick");
        lastOxidizerConsumedMbPerTick = compound.getDouble("LastOxidizerConsumedMbPerTick");
        fuelDrainAccumulator = compound.getDouble("FuelDrainAccumulator");
        oxidizerDrainAccumulator = compound.getDouble("OxidizerDrainAccumulator");

        // Update capacity before loading fluid to avoid truncation
        if (isController() && isMultiblock()) {
            int cap = getBaseTankCapacityMb() * width * width * width;
            if (tank != null) tank.getPrimaryHandler().setCapacity(cap);
            if (oxidizerTank != null) oxidizerTank.getPrimaryHandler().setCapacity(cap);
        }

        if (tank != null && compound.contains("FuelTankSync")) {
            tank.getPrimaryHandler().setFluid(FluidStack.parseOptional(registries, compound.getCompound("FuelTankSync")));
        }
        if (oxidizerTank != null && compound.contains("OxidizerTankSync")) {
            oxidizerTank.getPrimaryHandler().setFluid(FluidStack.parseOptional(registries, compound.getCompound("OxidizerTankSync")));
        }

        if (compound.contains("ControllerOffX")) {
            controllerPos = worldPosition.offset(
                compound.getInt("ControllerOffX"),
                compound.getInt("ControllerOffY"),
                compound.getInt("ControllerOffZ"));
        } else {
            controllerPos = null;
        }
        updateConnectivity = compound.getBoolean("UpdateConnectivity");
    }

    protected int getBaseTankCapacityMb() {
        return PropulsionConfig.FUEL_TANK_CAPACITY_MB.get();
    }
}











