package dev.propulsionteam.propulsionsimulated.assemblerstick.interaction;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.config.server.blocks.SimAssembly;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.assembly.SimAssemblyException;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.assemblerstick.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = CreatePropulsion.ID)
public final class AssemblerStickInteractionHandler {
    private static final int COOLDOWN_TICKS = 10;

    private AssemblerStickInteractionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        final boolean usingAssemblerStick = event.getItemStack().is(ModItems.ASSEMBLER_STICK.get());
        final boolean usingAutoGlue = event.getItemStack().is(ModItems.AUTO_GLUE.get());
        final boolean usingMover = event.getItemStack().is(ModItems.GLUED_CONTRAPTION_MOVER.get());
        final boolean usingCloner = event.getItemStack().is(ModItems.GLUED_CONTRAPTION_CLONER.get());
        final boolean usingRemover = event.getItemStack().is(ModItems.CONTRAPTION_REMOVER.get());
        if (!usingAssemblerStick && !usingAutoGlue && !usingMover && !usingCloner && !usingRemover) {
            return;
        }
        final Item usedItem;
        if (usingMover) {
            usedItem = ModItems.GLUED_CONTRAPTION_MOVER.get();
        } else if (usingCloner) {
            usedItem = ModItems.GLUED_CONTRAPTION_CLONER.get();
        } else if (usingRemover) {
            usedItem = ModItems.CONTRAPTION_REMOVER.get();
        } else if (usingAutoGlue) {
            usedItem = ModItems.AUTO_GLUE.get();
        } else {
            usedItem = ModItems.ASSEMBLER_STICK.get();
        }

        if (event.getLevel().isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity() instanceof final ServerPlayer player)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (player.isSpectator() || player.getCooldowns().isOnCooldown(usedItem)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        final ServerLevel level = (ServerLevel) event.getLevel();
        final BlockPos clickedPos = event.getPos();

        if (!level.isLoaded(clickedPos)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        final boolean success;
        if (usingMover || usingCloner || usingRemover) {
            if (!ModList.get().isLoaded("create")) {
                player.displayClientMessage(Component.translatable("message.assemblystick.mover_create_missing"), true);
                success = false;
            } else if (usingRemover) {
                success = GluedContraptionMoverService.tryHardWipe(level, player, clickedPos);
            } else {
                success = GluedContraptionMoverService.tryPrepare(level, player, clickedPos, event.getHand(), usingMover);
            }
        } else if (usingAutoGlue) {
            success = tryAutoGlue(level, player, clickedPos);
        } else {
            final SubLevel containing = Sable.HELPER.getContaining(level, clickedPos);
            if (containing instanceof final ServerSubLevel serverSubLevel) {
                success = tryDisassemble(level, player, serverSubLevel, clickedPos);
            } else {
                success = tryAssemble(level, player, clickedPos);
            }
        }

        if (success) {
            player.getCooldowns().addCooldown(usedItem, COOLDOWN_TICKS);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else {
            event.setCancellationResult(InteractionResult.FAIL);
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getItemStack().is(ModItems.AUTO_GLUE.get())) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity() instanceof final ServerPlayer player)) {
            event.setCanceled(true);
            return;
        }

        if (player.isSpectator() || player.getCooldowns().isOnCooldown(ModItems.AUTO_GLUE.get())) {
            event.setCanceled(true);
            return;
        }

        final ServerLevel level = (ServerLevel) event.getLevel();
        final BlockPos clickedPos = event.getPos();
        if (!level.isLoaded(clickedPos)) {
            event.setCanceled(true);
            return;
        }

        if (tryRemoveHoneyGlue(level, clickedPos)) {
            player.getCooldowns().addCooldown(ModItems.AUTO_GLUE.get(), COOLDOWN_TICKS);
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);
    }

    private static boolean tryAutoGlue(final ServerLevel level, final ServerPlayer player, final BlockPos origin) {
        if (level.getBlockState(origin).isAir()) {
            return false;
        }

        final SubLevel targetSubLevel = Sable.HELPER.getContaining(level, origin);
        final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin.immutable());

        int minX = origin.getX();
        int minY = origin.getY();
        int minZ = origin.getZ();
        int maxX = origin.getX();
        int maxY = origin.getY();
        int maxZ = origin.getZ();
        final int maxBlocks = PropulsionConfig.AUTO_GLUE_MAX_BLOCKS.get();

        while (!frontier.isEmpty()) {
            final BlockPos pos = frontier.poll();
            if (!visited.add(pos)) {
                continue;
            }

            if (visited.size() > maxBlocks) {
                player.displayClientMessage(Component.translatable("message.assemblystick.auto_glue_too_large", maxBlocks), true);
                return false;
            }

            if (!level.isLoaded(pos)) {
                continue;
            }

            if (Sable.HELPER.getContaining(level, pos) != targetSubLevel) {
                continue;
            }

            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            for (final Direction direction : Direction.values()) {
                frontier.add(pos.relative(direction).immutable());
            }

            // Honey glue connections: if this block is inside any honey glue box,
            // all blocks within that glue volume are treated as connected.
            for (final HoneyGlueEntity glue : getHoneyGlueNear(level, pos)) {
                if (!glue.contains(pos)) {
                    continue;
                }

                final AABB bb = glue.getBoundingBox();
                final BlockPos glueMin = BlockPos.containing(bb.minX, bb.minY, bb.minZ);
                final BlockPos glueMax = BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ);

                minX = Math.min(minX, glueMin.getX());
                minY = Math.min(minY, glueMin.getY());
                minZ = Math.min(minZ, glueMin.getZ());
                maxX = Math.max(maxX, glueMax.getX());
                maxY = Math.max(maxY, glueMax.getY());
                maxZ = Math.max(maxZ, glueMax.getZ());

                for (int x = glueMin.getX(); x <= glueMax.getX(); x++) {
                    for (int y = glueMin.getY(); y <= glueMax.getY(); y++) {
                        for (int z = glueMin.getZ(); z <= glueMax.getZ(); z++) {
                            frontier.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return false;
        }

        final BlockPos from = new BlockPos(minX, minY, minZ);
        final BlockPos to = new BlockPos(maxX, maxY, maxZ);
        final HoneyGlueEntity glue = new HoneyGlueEntity(level, SuperGlueEntity.span(from, to));
        level.addFreshEntity(glue);
        return true;
    }

    private static Iterable<HoneyGlueEntity> getHoneyGlueNear(final ServerLevel level, final BlockPos pos) {
        final int range = SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();
        final AABB scan = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );
        return level.getEntitiesOfClass(HoneyGlueEntity.class, scan.inflate(range));
    }

    private static boolean tryRemoveHoneyGlue(final ServerLevel level, final BlockPos clickedPos) {
        boolean removedAny = false;
        for (final HoneyGlueEntity glue : getHoneyGlueNear(level, clickedPos)) {
            if (glue.contains(clickedPos)) {
                glue.discard();
                removedAny = true;
            }
        }
        return removedAny;
    }

    private static boolean tryAssemble(final ServerLevel level, final ServerPlayer player, final BlockPos clickedPos) {
        if (level.getBlockState(clickedPos).isAir()) {
            return false;
        }

        try {
            final SimAssemblyHelper.AssemblyResult result = SimAssemblyHelper.assembleFromSingleBlock(level, clickedPos, clickedPos, true, true);
            if (result == null) {
                return false;
            }
            return true;
        } catch (final AssemblyException e) {
            player.displayClientMessage(Component.translatable("message.assemblystick.assemble_failed"), true);
            return false;
        }
    }

    private static boolean tryDisassemble(final ServerLevel level, final ServerPlayer player, final ServerSubLevel subLevel, final BlockPos clickedPos) {
        try {
            validateDisassembly(level, subLevel);
        } catch (final AssemblyException e) {
            player.displayClientMessage(Component.translatable("message.assemblystick.disassemble_failed"), true);
            return false;
        }

        final double closestYRotation = SimMathUtils.getClosestYaw(subLevel.logicalPose().orientation());
        final double ninety = Math.PI / 2.0;
        final int turns = -(Mth.floor(closestYRotation / ninety + 0.5));
        final Rotation rotation = SimAssemblyHelper.rotationFrom90DegRots(turns);

        final BlockPos goal = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(clickedPos)));
        SimAssemblyHelper.disassembleSubLevel(level, subLevel, clickedPos, goal, rotation, true);
        return true;
    }

    private static void validateDisassembly(final ServerLevel level, final ServerSubLevel subLevel) throws AssemblyException {
        final BoundingBox3dc bounds = subLevel.boundingBox();
        if (bounds.maxY() > level.getMaxBuildHeight() || bounds.minY() < level.getMinBuildHeight()) {
            throw SimAssemblyException.outOfWorld();
        }

        final SimAssembly config = SimConfigService.INSTANCE.server().assembly;

        final var handle = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(subLevel);
        if (handle.getLinearVelocity(new Vector3d()).lengthSquared() > Mth.square(config.disassemblyMaxVelocity.getF()) ||
                handle.getAngularVelocity(new Vector3d()).lengthSquared() > Mth.square(config.disassemblyMaxAngularVelocity.getF())) {
            throw SimAssemblyException.tooFast();
        }

        if (config.disallowMidAirDisassembly.get()) {
            final BoundingBox3i chunkBounds = new BoundingBox3i(
                    (Mth.floor(bounds.minX()) >> 4) - 1,
                    (Mth.floor(bounds.minY()) >> 4) - 1,
                    (Mth.floor(bounds.minZ()) >> 4) - 1,
                    (Mth.floor(bounds.maxX()) >> 4) + 1,
                    (Mth.floor(bounds.maxY()) >> 4) + 1,
                    (Mth.floor(bounds.maxZ()) >> 4) + 1
            );

            boolean nearGround = false;

            scanSectionsLoop:
            for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
                for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
                    final LevelChunk chunk = level.getChunk(x, z);

                    for (int y = chunkBounds.minY(); y <= chunkBounds.maxY(); y++) {
                        final int index = chunk.getSectionIndexFromSectionY(y);

                        if (index < 0 || index >= chunk.getSectionsCount()) {
                            continue;
                        }

                        if (!chunk.getSection(index).hasOnlyAir()) {
                            nearGround = true;
                            break scanSectionsLoop;
                        }
                    }
                }
            }

            if (!nearGround) {
                throw SimAssemblyException.tooFarFromGround();
            }
        }
    }
}
