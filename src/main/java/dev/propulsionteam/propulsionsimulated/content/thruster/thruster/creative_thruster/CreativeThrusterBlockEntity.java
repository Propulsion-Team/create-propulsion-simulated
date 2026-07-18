package dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster;

import java.util.List;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.Sable;

import javax.annotation.Nullable;

public class CreativeThrusterBlockEntity extends AbstractThrusterBlockEntity {
    public static final int MAX_WIDTH = 3;
    @Nullable
    protected BlockPos controllerPos;
    protected boolean updateConnectivity = true;
    private static final int DISASSEMBLY_GRACE_TICKS = 5;
    private int disassemblyCooldown = 0;
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;

    public enum PlumeType {
        PLASMA, ION, PLUME, NONE
    }

    public PlumeType plumeType = PlumeType.PLASMA;

    public CreativeThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public CreativeThrusterBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        powerBehaviour = new CreativeThrusterPowerScrollValueBehaviour(this, new CreativeThrusterValueBox(), this::getConfiguredMaxThrustKn);
        // Start scroll at the configured base thrust value
        double base = PropulsionConfig.CREATIVE_THRUSTER_BASE_THRUST.get();
        double max = PropulsionConfig.CREATIVE_THRUSTER_MAX_THRUST.get();
        int startStep = (int) Math.round((base / max) * (CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1));
        powerBehaviour.value = Math.max(0, Math.min(CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1, startStep));
        powerBehaviour.withCallback(this::onAnyPowerBehaviourChanged);
        behaviours.add(powerBehaviour);
    }

    private void onAnyPowerBehaviourChanged(int value) {
        if (isMultiblock() && !isController()) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                ctrl.applyPowerValue(value);
            }
            return;
        }
        applyPowerValue(value);
    }

    private void applyPowerValue(int value) {
        if (powerBehaviour != null && powerBehaviour.getValue() != value) {
            powerBehaviour.value = value;
        }
        updateThrust(getBlockState());
        setChanged();
        sendData();
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        if (!isController() && isMultiblock()) {
            setThrustAndSync(0);
            isThrustDirty = false;
            return;
        }
        float thrust = 0;
        float currentPower = getEffectiveThrottle();
        if (currentPower > 0) {
            float baseThrustPn = (float) (getConfiguredTargetThrustKn() * getThrustUnitsPerKn());
            baseThrustPn *= (float) calculateAtmosphericFactor();
            thrust = currentPower * baseThrustPn;
        }
        setThrustAndSync(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() {
        return true;
    }

    @Override
    public float getPower() {
        if (!isController() && isMultiblock()) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
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
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (be instanceof CreativeThrusterBlockEntity t && t.redstoneInput > max) {
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
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                ctrl.dirtyThrust();
                ctrl.notifyUpdate();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (isMultiblock() && !isController() && powerBehaviour != null) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null && ctrl.powerBehaviour != null) {
                int v = ctrl.powerBehaviour.getValue();
                if (powerBehaviour.getValue() != v) {
                    powerBehaviour.value = v;
                }
            }
        }
        if (isController() && isMultiblock()) {
            if (disassemblyCooldown > 0) {
                disassemblyCooldown--;
            } else if (!SimulatedThrustAdapter.isOutsideWorldBuildHeight(level, worldPosition)) {
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

    public boolean isMultiblock() {
        return width > 1;
    }

    protected boolean supportsMultiblock() {
        return true;
    }

    public boolean isController() {
        return controllerPos == null;
    }

    public boolean isBaseLayerMember() {
        if (!isMultiblock()) return true;
        CreativeThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
        if (ctrl == null) return false;
        BlockPos origin = ctrl.getBlockPos();
        int rel = switch (getFacing().getAxis()) {
            case X -> worldPosition.getX() - origin.getX();
            case Y -> worldPosition.getY() - origin.getY();
            case Z -> worldPosition.getZ() - origin.getZ();
        };
        int baseIdx = getFacing().getAxisDirection() == Direction.AxisDirection.POSITIVE ? width - 1 : 0;
        return rel == baseIdx;
    }

    @Nullable
    public CreativeThrusterBlockEntity getControllerBE() {
        if (isController() || !hasLevel()) return this;
        BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, controllerPos);
        return be instanceof CreativeThrusterBlockEntity t ? t : null;
    }

    protected void tryAssemble() {
        if (!supportsMultiblock()) {
            return;
        }
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
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = SimulatedThrustAdapter.getBlockStateSafe(level, pos);
                    if (!state.is(expectedBlock)) return false;
                    if (!state.hasProperty(AbstractThrusterBlock.FACING)) return false;
                    if (state.getValue(AbstractThrusterBlock.FACING) != facing) return false;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, pos);
                    if (!(be instanceof CreativeThrusterBlockEntity t)) return false;
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
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (be instanceof CreativeThrusterBlockEntity t && t.isMultiblock()) {
                        CreativeThrusterBlockEntity ctrl = t.getControllerBE();
                        if (ctrl != null) ctrl.disassembleMulti();
                    }
                }
            }
        }

        CreativeThrusterBlockEntity controller = null;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, pos);
                    if (!(be instanceof CreativeThrusterBlockEntity t)) return;
                    if (pos.equals(origin)) controller = t;
                }
            }
        }
        if (controller == null) return;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, pos);
                    if (!(be instanceof CreativeThrusterBlockEntity t)) continue;
                    t.controllerPos = t == controller ? null : origin;
                    t.width = size;
                    t.isThrustDirty = true;
                    BlockState liveState = SimulatedThrustAdapter.getBlockStateSafe(level, pos);
                    if (liveState.hasProperty(ThrusterBlock.MULTIBLOCK) && !liveState.getValue(ThrusterBlock.MULTIBLOCK)) {
                        level.setBlock(pos, liveState.setValue(ThrusterBlock.MULTIBLOCK, true), Block.UPDATE_CLIENTS);
                    }
                    t.setChanged();
                    t.notifyUpdate();
                }
            }
        }
        controller.calculateObstruction(level, origin, facing);
    }

    public void disassembleMulti() {
        if (!isController() || !isMultiblock() || level == null) return;
        int size = width;
        BlockPos origin = worldPosition;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, pos);
                    if (!(be instanceof CreativeThrusterBlockEntity t)) continue;
                    t.controllerPos = null;
                    t.width = 1;
                    t.updateConnectivity = true;
                    t.isThrustDirty = true;
                    t.getThrusterData().setThrust(0);
                    BlockState liveState = SimulatedThrustAdapter.getBlockStateSafe(level, pos);
                    if (liveState.hasProperty(ThrusterBlock.MULTIBLOCK) && liveState.getValue(ThrusterBlock.MULTIBLOCK)) {
                        level.setBlock(pos, liveState.setValue(ThrusterBlock.MULTIBLOCK, false), Block.UPDATE_CLIENTS);
                    }
                    t.setChanged();
                    t.notifyUpdate();
                }
            }
        }
    }

    private boolean isValidFormedCube(BlockPos origin, int size, Direction facing) {
        if (level == null) return false;
        Block expectedBlock = getBlockState().getBlock();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = SimulatedThrustAdapter.getBlockStateSafe(level, pos);
                    if (!state.is(expectedBlock)) return false;
                    if (!state.hasProperty(AbstractThrusterBlock.FACING) || state.getValue(AbstractThrusterBlock.FACING) != facing) return false;
                    BlockEntity be = SimulatedThrustAdapter.getBlockEntitySafe(level, pos);
                    if (!(be instanceof CreativeThrusterBlockEntity t)) return false;
                    CreativeThrusterBlockEntity ctrl = t.getControllerBE();
                    if (ctrl == null || ctrl != this) return false;
                    if (t.width != size) return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isCreative() {
        return true;
    }


    @Override
    public PlumeType getPlumeType() {
        return plumeType;
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
    }

    // Particles

    public void cyclePlumeType() {
        int ordinal = plumeType.ordinal() + 1;
        if (ordinal >= PlumeType.values().length) {
            ordinal = 0;
        }
        plumeType = PlumeType.values()[ordinal];
        setChanged();
        sendData();
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return 0.55;
    }

    @Override
    protected double getBaseThrust() {
        return getConfiguredTargetThrustKn();
    }

    @Override
    protected double getRawThrustCap() {
        return getConfiguredTargetThrustKn();
    }

    @Override
    public boolean shouldEmitPlume() {
        return plumeType != PlumeType.NONE && super.shouldEmitPlume();
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
        if (isController() && isMultiblock()) {
            return getMultiblockCenterNozzlePositionLocal();
        }
        return super.getParticleDebugNozzlePositionLocal();
    }

    private boolean hasPlumeSpace() {
        if (level == null)
            return false;

        Direction facing = getBlockState().getValue(CreativeThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !SimulatedThrustAdapter.getBlockStateSafe(level,plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        if (plumeType == PlumeType.PLASMA) {
            return new PlasmaParticleData();
        }
        if (plumeType == PlumeType.ION) {
            return new IonParticleData();
        }
        // Default is plume :P
        return super.createParticleOptions();
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isController() && isMultiblock()) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null && ctrl != this) {
                ctrl.addThrusterDetails(tooltip, isPlayerSneaking);
                return;
            }
        }
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        addParticleCategory(tooltip);
    }

    private void addParticleCategory(List<Component> tooltip) {
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        Component particleValue = switch (plumeType) {
            case PLASMA -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plasma")
                    .withStyle(ChatFormatting.AQUA);
            case ION -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.ion")
                    .withStyle(ChatFormatting.BLUE);
            case PLUME -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plume")
                    .withStyle(ChatFormatting.GOLD);
            case NONE -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.none")
                    .withStyle(ChatFormatting.DARK_GRAY);
        };

        CreateLang.builder()
                .add(Component.literal("  "))
                .add(particleValue)
                .forGoggles(tooltip);
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                    .style(ChatFormatting.GREEN);
        }
        return CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                .style(ChatFormatting.GOLD);
    }

    // CC slop

    public void setThrustConfig(int value) {
        if (!isController() && isMultiblock()) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                ctrl.setThrustConfig(value);
            }
            return;
        }
        int clamped = Math.max(0, Math.min(value, 99));
        if (powerBehaviour.getValue() != clamped) {
            powerBehaviour.setValue(clamped);
            updateThrust(getBlockState());
            setChanged();
            sendData();
        }
    }

    public int getThrustConfig() {
        if (!isController() && isMultiblock()) {
            CreativeThrusterBlockEntity ctrl = getControllerBE();
            if (ctrl != null) {
                return ctrl.getThrustConfig();
            }
        }
        return powerBehaviour.getValue();
    }

    public float getTargetThrustNewtons() {
        return (float) getConfiguredTargetThrustKn();
    }

    public float getCreativeTargetThrust() {
        return (float) getConfiguredTargetThrustKn();
    }

    private double getConfiguredMaxThrustKn() {
        CreativeThrusterBlockEntity ctrl = isController() ? this : getControllerBE();
        int effectiveWidth = ctrl != null ? ctrl.width : width;
        return switch (effectiveWidth) {
            case 2 -> PropulsionConfig.CREATIVE_THRUSTER_MULTIBLOCK_2X2X2_MAX_THRUST.get();
            case 3 -> PropulsionConfig.CREATIVE_THRUSTER_MULTIBLOCK_3X3X3_MAX_THRUST.get();
            default -> PropulsionConfig.CREATIVE_THRUSTER_MAX_THRUST.get();
        };
    }

    private double getConfiguredTargetThrustKn() {
        int value = powerBehaviour != null ? powerBehaviour.getValue() : 0;
        double forcePerStep = getConfiguredMaxThrustKn() / (double) CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS;
        return (value + 1) * forcePerStep;
    }

    // NBT

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries,
            boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("plumeType", plumeType.ordinal());
        compound.putInt("Width", width);
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
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries,
            boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        width = Math.max(1, compound.getInt("Width"));
        if (compound.contains("plumeType")) {
            int idx = compound.getInt("plumeType");
            plumeType = PlumeType.values()[Mth.clamp(idx, 0, PlumeType.values().length - 1)];
        }
        if (compound.contains("ControllerOffX")) {
            controllerPos = worldPosition.offset(
                    compound.getInt("ControllerOffX"),
                    compound.getInt("ControllerOffY"),
                    compound.getInt("ControllerOffZ"));
        } else {
            controllerPos = null;
        }
        // Always retry connectivity on load for standalone blocks so existing placements can unify.
        boolean savedConnectivity = compound.getBoolean("UpdateConnectivity");
        updateConnectivity = width <= 1 || !compound.contains("UpdateConnectivity") || savedConnectivity;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (isController() && isMultiblock()) {
            return new AABB(
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    worldPosition.getX() + width, worldPosition.getY() + width, worldPosition.getZ() + width);
        }
        return super.getRenderBoundingBox();
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        super.afterMove(oldLevel, newLevel, state, oldPos, newPos);
        if (isMultiblock()) {
            width = 1;
            controllerPos = null;
            updateConnectivity = true;
            isThrustDirty = true;
            setChanged();
        }
        disassemblyCooldown = DISASSEMBLY_GRACE_TICKS;
    }
}

