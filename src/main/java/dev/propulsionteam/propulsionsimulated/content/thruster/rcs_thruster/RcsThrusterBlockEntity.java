package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.createpropulsionsimulated.client.sound.RcsThrusterSoundController;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;
import dev.propulsionteam.propulsionsimulated.content.thruster.SoundMufflerBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.PropulsionCoreBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkBehaviour;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.world.Clearable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;
import java.util.List;
import java.util.Locale;

public class RcsThrusterBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, BlockSubLevelAssemblyListener, IHaveGoggleInformation, Clearable {
    public SmartFluidTankBehaviour tank;
    public VectorRedstoneLinkBehaviour[] links;
    private final int[] wirelessPower = new int[4];
    private final float[] overridePower = {-1, -1, -1, -1};
    private final float[] nozzlePower = new float[4];
    private double drainAccumulator;
    public AbstractComputerBehaviour computerBehaviour;
    private volatile int activeMask;
    private volatile boolean hydrazineBoost;

    public RcsThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.RCS_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isSingle() { return ((RcsThrusterBlock) getBlockState().getBlock()).isSingle(); }

    public boolean isFiring(int nozzle) { return (activeMask & (1 << nozzle)) != 0; }

    public double thrust() {
        return (isSingle() ? PropulsionConfig.SINGLE_RCS_THRUST : PropulsionConfig.RCS_THRUST).get()
                * (hydrazineBoost ? PropulsionConfig.RCS_HYDRAZINE_THRUST_MULTIPLIER.get() : 1.0d);
    }

    public boolean isActive() { return activeMask != 0 && thrust() > 0; }

    public double currentThrust() {
        double sum = 0;
        for (int i = 0; i < links.length; i++) sum += nozzlePower[i];
        return sum * thrust();
    }

    public double nozzleThrust(int nozzle) { return nozzlePower[nozzle] * thrust(); }

    public float getNozzlePower(int nozzle) { return nozzlePower[nozzle]; }

    public boolean isBoosted() { return hydrazineBoost; }

    public void setNozzleOverride(int nozzle, float power) {
        overridePower[nozzle] = Math.max(0f, Math.min(1f, power));
        refreshPower();
    }

    public void applyPowerForScene(float power) {
        float clamped = Math.max(0f, Math.min(1f, power));
        for (int i = 0; i < links.length; i++) nozzlePower[i] = clamped;
        activeMask = clamped > 0 ? (1 << links.length) - 1 : 0;
    }

    public void applyNozzlePowerForScene(int nozzle, float power) {
        if (nozzle < 0 || nozzle >= links.length) return;
        float clamped = Math.max(0f, Math.min(1f, power));
        nozzlePower[nozzle] = clamped;
        if (clamped > 0) activeMask |= 1 << nozzle;
        else activeMask &= ~(1 << nozzle);
    }

    public void clearOverrides() {
        java.util.Arrays.fill(overridePower, -1f);
        refreshPower();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.title.thruster_stats"))
                .style(ChatFormatting.WHITE).forGoggles(tooltip);
        CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status")).text(": ")
                .add(Component.translatable(isActive() ? "createpropulsion.gui.goggles.thruster.status.working"
                        : "createpropulsion.gui.goggles.thruster.status.not_powered")
                        .withStyle(isActive() ? ChatFormatting.GREEN : ChatFormatting.GOLD)).forGoggles(tooltip);
        CreateLang.builder().text("  ")
                .add(Component.translatable("createpropulsion.tooltip.thrust1").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(String.format(Locale.ROOT, "%.2f", currentThrust())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" pN").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 1000);
        tank.getPrimaryHandler().setValidator(stack -> stack.getFluid() == PropulsionFluids.HYDRAZINE.get()
                || stack.getFluid() == PropulsionFluids.FLOWING_HYDRAZINE.get());
        behaviours.add(tank);
        behaviours.add(new SoundMufflerBehaviour(this));
        behaviours.add(new PropulsionCoreBehaviour(this));
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
        links = new VectorRedstoneLinkBehaviour[isSingle() ? 1 : 4];
        for (int i = 0; i < links.length; i++) {
            int nozzle = i;
            links[i] = VectorRedstoneLinkBehaviour.receiver(this,
                    ValueBoxTransform.Dual.makeSlots(first -> new RcsLinkTransform(first, nozzle)),
                    VectorRedstoneLinkBehaviour.RCS_TYPES.get(i), "Rcs" + i,
                    power -> {
                        wirelessPower[nozzle] = power;
                        refreshPower();
                    });
            behaviours.add(links[i]);
        }
    }

    @Override
    public void initialize() {
        for (VectorRedstoneLinkBehaviour link : links) link.newPosition = true;
        super.initialize();
        if (!level.isClientSide) refreshPower();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) RcsThrusterSoundController.tick(this);
        else {
            refreshPower();
            boolean active = isActive();
            boolean boost = active && !tank.getPrimaryHandler().isEmpty();
            drainAccumulator = boost ? drainAccumulator + PropulsionConfig.RCS_HYDRAZINE_DRAIN_PER_TICK.get() * currentThrust() / (thrust() * links.length) : 0;
            int drain = (int) drainAccumulator;
            if (drain > 0) {
                drainAccumulator -= tank.getPrimaryHandler().drain(drain, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
            if (hydrazineBoost != boost) {
                hydrazineBoost = boost;
                sendData();
            }
        }
    }

    public void refreshPower() {
        if (level == null || level.isClientSide) return;
        int mask = 0;
        float[] power = new float[4];
        for (int i = 0; i < links.length; i++) {
            Direction side = RcsOrientation.direction(RcsOrientation.exhaust(isSingle(), i), RcsThrusterBlock.normal(getBlockState()));
            int signal = isSingle() ? level.getBestNeighborSignal(worldPosition) : level.getSignal(worldPosition.relative(side), side);
            float level = overridePower[i] >= 0 ? overridePower[i] : Math.max(signal, wirelessPower[i]) / 15f;
            power[i] = level;
            if (level > 0) mask |= 1 << i;
        }
        boolean changed = mask != activeMask || !java.util.Arrays.equals(power, nozzlePower);
        System.arraycopy(power, 0, nozzlePower, 0, 4);
        activeMask = mask;
        if (changed) sendData();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        Direction normal = RcsThrusterBlock.normal(getBlockState());
        for (int i = 0; i < links.length; i++) {
            float power = nozzlePower[i];
            if (power <= 0) continue;
            Vector3d point = RcsOrientation.point(RcsOrientation.nozzle(isSingle(), i), normal)
                    .add(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            Vector3d impulse = RcsOrientation.impulse(isSingle(), i, normal, thrust() * power, timeStep);
            SimulatedThrustAdapter.applyImpulseAtPoint(subLevel, point, impulse, this);
        }
    }

    @Override
    public void clearContent() {
        getBehaviour(SoundMufflerBehaviour.TYPE).clearContent();
        getBehaviour(PropulsionCoreBehaviour.TYPE).clearContent();
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        java.util.Arrays.fill(wirelessPower, 0);
        for (VectorRedstoneLinkBehaviour link : links) {
            link.newPosition = true;
            link.notifySignalChange();
        }
        refreshPower();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putInt("ActiveNozzles", activeMask);
            for (int i = 0; i < 4; i++) tag.putFloat("NozzlePower" + i, nozzlePower[i]);
        }
        if (clientPacket) tag.putBoolean("HydrazineBoost", hydrazineBoost);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            activeMask = tag.getInt("ActiveNozzles");
            for (int i = 0; i < 4; i++) nozzlePower[i] = tag.getFloat("NozzlePower" + i);
        }
        if (clientPacket) hydrazineBoost = tag.getBoolean("HydrazineBoost");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(PropulsionConfig.RCS_FLAME_SCALE.get());
    }
}
