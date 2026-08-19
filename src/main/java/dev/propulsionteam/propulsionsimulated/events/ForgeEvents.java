package dev.propulsionteam.propulsionsimulated.events;

import com.simibubi.create.content.equipment.armor.NetheriteDivingHandler;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelManager;
import dev.propulsionteam.propulsionsimulated.network.PropulsionPackets;
import dev.propulsionteam.propulsionsimulated.network.SyncSolidThrusterFuelsPacket;
import dev.propulsionteam.propulsionsimulated.network.SyncThrusterFuelsPacket;
import dev.propulsionteam.propulsionsimulated.content.thruster.SolidThrusterFuelManager;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionCommands;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.GAME)
public class ForgeEvents {
    private static final ResourceKey<DamageType> CORAL_SUBMERSION_DAMAGE_TYPE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "coral_submersion")
    );
    private static final int CORAL_DAMAGE_INTERVAL_TICKS = 20;
    private static final float CORAL_DAMAGE_AMOUNT = 8.0f;


    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        PropulsionCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
        event.addListener(new SolidThrusterFuelManager());
        event.addListener(new CoralGeneratorFuelManager());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PropulsionPackets.sendToPlayer(
                SyncThrusterFuelsPacket.create(
                    ThrusterFuelManager.getFuelPropertiesMap(),
                    ThrusterFuelManager.getRemovedFuelIds(),
                    ThrusterFuelManager.getEfficiencyOverrides()),
                serverPlayer
            );
            PropulsionPackets.sendToPlayer(
                SyncSolidThrusterFuelsPacket.create(SolidThrusterFuelManager.getFuelPropertiesMap(), SolidThrusterFuelManager.getRemovedFuelIds()),
                serverPlayer
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Player player = event.getEntity();
        if (player.tickCount % CORAL_DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }
        if (!isTouchingCoralFluid(serverLevel, player)) {
            return;
        }
        if (hasFullNetheriteDivingProtection(player)) {
            return;
        }

        player.hurt(coralSubmersionDamageSource(serverLevel), CORAL_DAMAGE_AMOUNT);
    }

    //Turpentine-lava interaction
    @SubscribeEvent
    public static void onNeighborBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getFluidState().isEmpty()) {
            return;
        }

        boolean isTurpentine = state.getFluidState().is(PropulsionFluids.TURPENTINE.get());
        boolean isLava = state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA);

        if (!isTurpentine && !isLava) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (isTurpentine && (neighborFluid.is(Fluids.LAVA) || neighborFluid.is(Fluids.FLOWING_LAVA))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }

            if (isLava && neighborFluid.is(PropulsionFluids.TURPENTINE.get())) {
                level.setBlock(neighborPos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }
        }
    }

    private static boolean hasFullNetheriteDivingProtection(net.minecraft.world.entity.player.Player player) {
        return NetheriteDivingHandler.isNetheriteDivingHelmet(player.getItemBySlot(EquipmentSlot.HEAD))
            && NetheriteDivingHandler.isNetheriteBacktank(player.getItemBySlot(EquipmentSlot.CHEST))
            && NetheriteDivingHandler.isNetheriteArmor(player.getItemBySlot(EquipmentSlot.LEGS))
            && NetheriteDivingHandler.isNetheriteArmor(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isTouchingCoralFluid(ServerLevel level, Player player) {
        Vec3 pos = player.position();
        double midY = player.getBoundingBox().minY + player.getBbHeight() * 0.5;
        double eyeY = player.getEyeY();
        return isCoralFluid(level.getFluidState(BlockPos.containing(pos.x, player.getBoundingBox().minY + 0.01, pos.z)))
            || isCoralFluid(level.getFluidState(BlockPos.containing(pos.x, midY, pos.z)))
            || isCoralFluid(level.getFluidState(BlockPos.containing(pos.x, eyeY, pos.z)));
    }

    private static boolean isCoralFluid(FluidState fluidState) {
        return fluidState.is(PropulsionFluids.CORAL.get()) || fluidState.is(PropulsionFluids.FLOWING_CORAL.get());
    }

    private static DamageSource coralSubmersionDamageSource(ServerLevel level) {
        Holder<DamageType> damageType = level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(CORAL_SUBMERSION_DAMAGE_TYPE);
        return new DamageSource(damageType);
    }

}
