package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
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
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkBehaviour;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import java.util.List;
import java.util.Locale;

public class RcsThrusterBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, BlockSubLevelAssemblyListener, IHaveGoggleInformation {
    public VectorRedstoneLinkBehaviour[] links;
    private int wirelessMask;
    private volatile int activeMask;

    public RcsThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.RCS_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isSingle() { return ((RcsThrusterBlock) getBlockState().getBlock()).isSingle(); }

    public boolean isFiring(int nozzle) { return (activeMask & (1 << nozzle)) != 0; }

    public double thrust() {
        return (isSingle() ? PropulsionConfig.SINGLE_RCS_THRUST : PropulsionConfig.RCS_THRUST).get();
    }

    public boolean isActive() { return activeMask != 0 && thrust() > 0; }

    public double currentThrust() { return Integer.bitCount(activeMask) * thrust(); }

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
        behaviours.add(new SoundMufflerBehaviour(this));
        links = new VectorRedstoneLinkBehaviour[isSingle() ? 1 : 4];
        for (int i = 0; i < links.length; i++) {
            int nozzle = i;
            links[i] = VectorRedstoneLinkBehaviour.receiver(this,
                    ValueBoxTransform.Dual.makeSlots(first -> new RcsLinkTransform(first, nozzle)),
                    VectorRedstoneLinkBehaviour.RCS_TYPES.get(i), "Rcs" + i,
                    power -> {
                        if (power > 0) wirelessMask |= 1 << nozzle;
                        else wirelessMask &= ~(1 << nozzle);
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
        else refreshPower();
    }

    public void refreshPower() {
        if (level == null || level.isClientSide) return;
        int mask = wirelessMask;
        for (int i = 0; i < links.length; i++) {
            Direction side = RcsOrientation.direction(RcsOrientation.exhaust(isSingle(), i), RcsThrusterBlock.normal(getBlockState()));
            int signal = isSingle() ? level.getBestNeighborSignal(worldPosition) : level.getSignal(worldPosition.relative(side), side);
            if (signal > 0) mask |= 1 << i;
        }
        if (mask == activeMask) return;
        activeMask = mask;
        sendData();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        int mask = activeMask;
        Direction normal = RcsThrusterBlock.normal(getBlockState());
        for (int i = 0; i < links.length; i++) {
            if ((mask & (1 << i)) == 0) continue;
            Vector3d point = RcsOrientation.point(RcsOrientation.nozzle(isSingle(), i), normal)
                    .add(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            Vector3d impulse = RcsOrientation.impulse(isSingle(), i, normal, thrust(), timeStep);
            SimulatedThrustAdapter.applyImpulseAtPoint(subLevel, point, impulse);
        }
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        wirelessMask = 0;
        for (VectorRedstoneLinkBehaviour link : links) {
            link.newPosition = true;
            link.notifySignalChange();
        }
        refreshPower();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) tag.putInt("ActiveNozzles", activeMask);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) activeMask = tag.getInt("ActiveNozzles");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(PropulsionConfig.RCS_FLAME_SCALE.get());
    }
}
