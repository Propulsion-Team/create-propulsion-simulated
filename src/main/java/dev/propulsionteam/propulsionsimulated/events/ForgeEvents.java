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
import dev.propulsionteam.propulsionsimulated.content.thruster.PlumeViewerPreferences;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = CreatePropulsion.ID)
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
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlumeViewerPreferences.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PlumeViewerPreferences.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Player player = event.getEntity();
        if (!hasFullNetheriteDivingProtection(player)
            && isTouchingFluid(serverLevel, player, PropulsionFluids.HYDRAZINE.get(), PropulsionFluids.FLOWING_HYDRAZINE.get())) {
            int exposureTicks = Math.min(player.getPersistentData().getInt("createpropulsion:hydrazine_exposure") + 1, 1200);
            player.getPersistentData().putInt("createpropulsion:hydrazine_exposure", exposureTicks);
            if (exposureTicks < 20) {
                applyHydrazineEffect(player, MobEffects.POISON, 100, 1);
            } else if (exposureTicks < 60) {
                applyHydrazineEffect(player, MobEffects.POISON, 200, 2);
                applyHydrazineEffect(player, MobEffects.CONFUSION, 200, 0);
            } else if (exposureTicks <= 100) {
                applyHydrazineEffect(player, MobEffects.POISON, 200, 3);
                applyHydrazineEffect(player, MobEffects.WEAKNESS, 200, 1);
            } else {
                applyHydrazineEffect(player, MobEffects.WITHER, 200, 1);
            }

            Vec3 eye = player.getEyePosition();
            BlockPos eyePos = BlockPos.containing(eye);
            FluidState eyeFluid = serverLevel.getFluidState(eyePos);
            if (isFluid(eyeFluid, PropulsionFluids.HYDRAZINE.get(), PropulsionFluids.FLOWING_HYDRAZINE.get())
                && eye.y < eyePos.getY() + eyeFluid.getHeight(serverLevel, eyePos)) {
                int immersionTicks = player.getPersistentData().getInt("createpropulsion:hydrazine_immersion") + 1;
                player.getPersistentData().putInt("createpropulsion:hydrazine_immersion", immersionTicks);
                if ((immersionTicks - 1) % 20 == 0) {
                    player.hurt(player.damageSources().magic(), 2.0f + Math.min(immersionTicks / 20, 10));
                }
            } else {
                player.getPersistentData().remove("createpropulsion:hydrazine_immersion");
                if (exposureTicks > 100 && player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().magic(), 2.0f);
                }
            }
        } else {
            player.getPersistentData().remove("createpropulsion:hydrazine_exposure");
            player.getPersistentData().remove("createpropulsion:hydrazine_immersion");
        }

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

    private static void applyHydrazineEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() <= 20) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private static boolean hasFullNetheriteDivingProtection(net.minecraft.world.entity.player.Player player) {
        return NetheriteDivingHandler.isNetheriteDivingHelmet(player.getItemBySlot(EquipmentSlot.HEAD))
            && NetheriteDivingHandler.isNetheriteBacktank(player.getItemBySlot(EquipmentSlot.CHEST))
            && NetheriteDivingHandler.isNetheriteArmor(player.getItemBySlot(EquipmentSlot.LEGS))
            && NetheriteDivingHandler.isNetheriteArmor(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isTouchingCoralFluid(ServerLevel level, Player player) {
        return isTouchingFluid(level, player, PropulsionFluids.CORAL.get(), PropulsionFluids.FLOWING_CORAL.get());
    }

    private static boolean isTouchingFluid(ServerLevel level, Player player, Fluid source, Fluid flowing) {
        Vec3 pos = player.position();
        double midY = player.getBoundingBox().minY + player.getBbHeight() * 0.5;
        double eyeY = player.getEyeY();
        return isFluid(level.getFluidState(BlockPos.containing(pos.x, player.getBoundingBox().minY + 0.01, pos.z)), source, flowing)
            || isFluid(level.getFluidState(BlockPos.containing(pos.x, midY, pos.z)), source, flowing)
            || isFluid(level.getFluidState(BlockPos.containing(pos.x, eyeY, pos.z)), source, flowing);
    }

    private static boolean isFluid(FluidState fluidState, Fluid source, Fluid flowing) {
        return fluidState.is(source) || fluidState.is(flowing);
    }

    private static DamageSource coralSubmersionDamageSource(ServerLevel level) {
        Holder<DamageType> damageType = level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(CORAL_SUBMERSION_DAMAGE_TYPE);
        return new DamageSource(damageType);
    }

}
