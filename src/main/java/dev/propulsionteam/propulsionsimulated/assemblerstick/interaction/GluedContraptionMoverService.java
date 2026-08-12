package dev.propulsionteam.propulsionsimulated.assemblerstick.interaction;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.foundation.utility.CreatePaths;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.assemblerstick.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GluedContraptionMoverService {
    private static final String ROOT_TAG = "AssemblystickMover";
    private static final String SESSION_ID = "SessionId";
    private static final String MODE = "Mode";
    private static final String SOURCE_BLOCKS = "SourceBlocks";
    private static final String SOURCE_SUPER_GLUE = "SourceSuperGlue";
    private static final String SOURCE_HONEY_GLUE = "SourceHoneyGlue";
    private static final String SCHEMATIC_FILE = "SchematicFile";
    private static final String SCHEMATIC_OWNER = "SchematicOwner";

    private static final String BB_MIN_X = "MinX";
    private static final String BB_MIN_Y = "MinY";
    private static final String BB_MIN_Z = "MinZ";
    private static final String BB_MAX_X = "MaxX";
    private static final String BB_MAX_Y = "MaxY";
    private static final String BB_MAX_Z = "MaxZ";

    private static final int SUPER_GLUE_RANGE = 16;
    private static final String MODE_MOVE = "move";
    private static final String MODE_CLONE = "clone";

    private GluedContraptionMoverService() {
    }

    public static boolean tryPrepare(
            final ServerLevel level,
            final ServerPlayer player,
            final BlockPos origin,
            final InteractionHand hand,
            final boolean deleteSourceAfterPlacement
    ) {
        if (level.getBlockState(origin).isAir()) {
            return false;
        }

        if (!isBlockGlued(level, origin)) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_not_glued"), true);
            return false;
        }

        final int maxBlocks = deleteSourceAfterPlacement
                ? PropulsionConfig.CONTRAPTION_MOVER_MAX_BLOCKS.get()
                : PropulsionConfig.CONTRAPTION_CLONER_MAX_BLOCKS.get();
        final GatherResult gatherResult = gatherConnected(level, origin, maxBlocks);
        if (gatherResult.tooLarge) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_too_large", maxBlocks), true);
            return false;
        }
        if (gatherResult.blocks.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_prepare_failed"), true);
            return false;
        }

        final String playerName = player.getGameProfile().getName();
        final String sessionId = UUID.randomUUID().toString();
        final String schematicName = "assemblystick_mover_" + sessionId + ".nbt";
        final Path playerDir = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(playerName).normalize();
        final SchematicExport.SchematicExportResult exportResult = SchematicExport.saveSchematic(
                playerDir,
                schematicName,
                true,
                level,
                gatherResult.min,
                gatherResult.max
        );

        if (exportResult == null) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_prepare_failed"), true);
            return false;
        }

        final ItemStack schematic = SchematicItem.create(level, schematicName, playerName);
        final List<GlueBoxSnapshot> sourceSuperGlue = collectGlueSnapshots(level, gatherResult.blocks, false);
        final List<GlueBoxSnapshot> sourceHoneyGlue = collectGlueSnapshots(level, gatherResult.blocks, true);
        writeMoverData(
                schematic,
                sessionId,
                deleteSourceAfterPlacement ? MODE_MOVE : MODE_CLONE,
                schematicName,
                playerName,
                gatherResult.blocks,
                sourceSuperGlue,
                sourceHoneyGlue
        );

        player.setItemInHand(hand, schematic);
        player.displayClientMessage(Component.translatable("message.assemblystick.mover_prepared"), true);
        return true;
    }

    public static boolean tryHardWipe(final ServerLevel level, final ServerPlayer player, final BlockPos origin) {
        if (level.getBlockState(origin).isAir()) {
            return false;
        }

        if (!isBlockGlued(level, origin)) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_not_glued"), true);
            return false;
        }

        final int maxBlocks = PropulsionConfig.CONTRAPTION_REMOVER_MAX_BLOCKS.get();
        final GatherResult gatherResult = gatherConnected(level, origin, maxBlocks);
        if (gatherResult.tooLarge) {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_too_large", maxBlocks), true);
            return false;
        }
        if (gatherResult.blocks.isEmpty()) {
            return false;
        }

        final List<GlueBoxSnapshot> sourceSuperGlue = collectGlueSnapshots(level, gatherResult.blocks, false);
        final List<GlueBoxSnapshot> sourceHoneyGlue = collectGlueSnapshots(level, gatherResult.blocks, true);

        boolean ok = true;
        ok &= removeSourceBlocksHard(level, gatherResult.blocks);
        ok &= removeGlueSnapshots(level, sourceSuperGlue, false);
        ok &= removeGlueSnapshots(level, sourceHoneyGlue, true);
        return ok;
    }

    public static void finalizePlacedMove(final ServerPlayer player, final ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof SchematicItem)) {
            return;
        }

        final CompoundTag moverData = readMoverData(stack);
        if (moverData == null) {
            return;
        }

        final ServerLevel level = player.serverLevel();

        final SchematicPrinter printer = new SchematicPrinter();
        printer.loadSchematic(stack, level, !player.canUseGameMasterBlocks());
        if (!printer.isLoaded() || printer.isErrored()) {
            return;
        }

        final List<GlueBoxSnapshot> placedSuperGlue = new ArrayList<>();
        final List<GlueBoxSnapshot> placedHoneyGlue = new ArrayList<>();
        while (printer.advanceCurrentPos()) {
            if (!printer.shouldPlaceCurrent(level)) {
                continue;
            }
            printer.handleCurrentTarget((pos, state, blockEntity) -> {
            }, (pos, entity) -> {
                if (entity instanceof final SuperGlueEntity superGlueEntity) {
                    placedSuperGlue.add(GlueBoxSnapshot.fromAabb(superGlueEntity.getBoundingBox()));
                } else if (entity instanceof final HoneyGlueEntity honeyGlueEntity) {
                    placedHoneyGlue.add(GlueBoxSnapshot.fromAabb(honeyGlueEntity.getBoundingBox()));
                }
            });
        }

        final Set<BlockPos> sourceBlocks = readSourceBlocks(moverData);
        final List<GlueBoxSnapshot> sourceSuperGlue = readGlueSnapshots(moverData.getList(SOURCE_SUPER_GLUE, Tag.TAG_COMPOUND));
        final List<GlueBoxSnapshot> sourceHoneyGlue = readGlueSnapshots(moverData.getList(SOURCE_HONEY_GLUE, Tag.TAG_COMPOUND));
        final boolean shouldDeleteSource = MODE_MOVE.equals(moverData.getString(MODE));

        boolean cleanupOk = true;
        if (shouldDeleteSource) {
            cleanupOk &= removeSourceBlocksHard(level, sourceBlocks);
            cleanupOk &= removeGlueSnapshots(level, sourceSuperGlue, false);
            cleanupOk &= removeGlueSnapshots(level, sourceHoneyGlue, true);
            cleanupOk &= ensureGlueSnapshots(level, placedSuperGlue, false);
            cleanupOk &= ensureGlueSnapshots(level, placedHoneyGlue, true);
        }

        cleanupOk &= cleanupSchematicFile(moverData);
        consumeSessionSchematicAndRestoreTool(player, moverData.getString(SESSION_ID), moverData.getString(MODE));
        if (cleanupOk) {
            player.displayClientMessage(Component.translatable(
                    shouldDeleteSource
                            ? "message.assemblystick.mover_finalize_success"
                            : "message.assemblystick.cloner_finalize_success"
            ), true);
        } else {
            player.displayClientMessage(Component.translatable("message.assemblystick.mover_finalize_failed"), true);
        }
    }

    private static GatherResult gatherConnected(final ServerLevel level, final BlockPos origin, final int maxBlocks) {
        final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        final Set<BlockPos> blocks = new HashSet<>();
        frontier.add(origin.immutable());

        int minX = origin.getX();
        int minY = origin.getY();
        int minZ = origin.getZ();
        int maxX = origin.getX();
        int maxY = origin.getY();
        int maxZ = origin.getZ();

        boolean tooLarge = false;
        while (!frontier.isEmpty()) {
            final BlockPos pos = frontier.poll();
            if (!visited.add(pos)) {
                continue;
            }

            if (visited.size() > maxBlocks) {
                tooLarge = true;
                break;
            }

            if (!level.isLoaded(pos)) {
                continue;
            }

            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            blocks.add(pos.immutable());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            for (final Direction direction : Direction.values()) {
                final BlockPos adjacent = pos.relative(direction);
                if (isConnectedByGlue(level, pos, adjacent)) {
                    frontier.add(adjacent.immutable());
                }
            }

            for (final SuperGlueEntity glue : getSuperGlueNear(level, pos)) {
                if (glue.contains(pos)) {
                    enqueueGlueVolume(frontier, glue.getBoundingBox());
                }
            }
            for (final HoneyGlueEntity glue : getHoneyGlueNear(level, pos)) {
                if (glue.contains(pos)) {
                    enqueueGlueVolume(frontier, glue.getBoundingBox());
                }
            }
        }

        return new GatherResult(blocks, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), tooLarge);
    }

    private static void enqueueGlueVolume(final ArrayDeque<BlockPos> frontier, final AABB aabb) {
        final BlockPos min = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        final BlockPos max = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    frontier.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static boolean isBlockGlued(final ServerLevel level, final BlockPos pos) {
        for (final SuperGlueEntity glue : getSuperGlueNear(level, pos)) {
            if (glue.contains(pos)) {
                return true;
            }
        }
        for (final HoneyGlueEntity glue : getHoneyGlueNear(level, pos)) {
            if (glue.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConnectedByGlue(final ServerLevel level, final BlockPos first, final BlockPos second) {
        final AABB between = SuperGlueEntity.span(first, second);

        for (final SuperGlueEntity glue : level.getEntitiesOfClass(SuperGlueEntity.class, between.inflate(SUPER_GLUE_RANGE))) {
            if (glue.contains(first) && glue.contains(second)) {
                return true;
            }
        }

        final int honeyRange = SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();
        for (final HoneyGlueEntity glue : level.getEntitiesOfClass(HoneyGlueEntity.class, between.inflate(honeyRange))) {
            if (glue.contains(first) && glue.contains(second)) {
                return true;
            }
        }

        return false;
    }

    private static Iterable<SuperGlueEntity> getSuperGlueNear(final ServerLevel level, final BlockPos pos) {
        final AABB scan = SuperGlueEntity.span(pos, pos).inflate(SUPER_GLUE_RANGE);
        return level.getEntitiesOfClass(SuperGlueEntity.class, scan);
    }

    private static Iterable<HoneyGlueEntity> getHoneyGlueNear(final ServerLevel level, final BlockPos pos) {
        final int range = SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();
        final AABB scan = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );
        return level.getEntitiesOfClass(HoneyGlueEntity.class, scan.inflate(range));
    }

    private static List<GlueBoxSnapshot> collectGlueSnapshots(final ServerLevel level, final Set<BlockPos> blocks, final boolean honey) {
        final List<GlueBoxSnapshot> result = new ArrayList<>();
        if (blocks.isEmpty()) {
            return result;
        }

        final int minX = blocks.stream().mapToInt(BlockPos::getX).min().orElse(0);
        final int minY = blocks.stream().mapToInt(BlockPos::getY).min().orElse(0);
        final int minZ = blocks.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        final int maxX = blocks.stream().mapToInt(BlockPos::getX).max().orElse(0);
        final int maxY = blocks.stream().mapToInt(BlockPos::getY).max().orElse(0);
        final int maxZ = blocks.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        final AABB scan = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(2);

        if (honey) {
            for (final HoneyGlueEntity glue : level.getEntitiesOfClass(HoneyGlueEntity.class, scan)) {
                if (containsAnyBlock(glue.getBoundingBox(), blocks)) {
                    result.add(GlueBoxSnapshot.fromAabb(glue.getBoundingBox()));
                }
            }
        } else {
            for (final SuperGlueEntity glue : level.getEntitiesOfClass(SuperGlueEntity.class, scan)) {
                if (containsAnyBlock(glue.getBoundingBox(), blocks)) {
                    result.add(GlueBoxSnapshot.fromAabb(glue.getBoundingBox()));
                }
            }
        }

        return result;
    }

    private static boolean containsAnyBlock(final AABB bb, final Set<BlockPos> blocks) {
        final BlockPos min = BlockPos.containing(bb.minX, bb.minY, bb.minZ);
        final BlockPos max = BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (blocks.contains(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void writeMoverData(
            final ItemStack stack,
            final String sessionId,
            final String mode,
            final String schematicFile,
            final String schematicOwner,
            final Set<BlockPos> sourceBlocks,
            final List<GlueBoxSnapshot> sourceSuperGlue,
            final List<GlueBoxSnapshot> sourceHoneyGlue
    ) {
        final CompoundTag root = new CompoundTag();
        root.putString(SESSION_ID, sessionId);
        root.putString(MODE, mode);
        root.putString(SCHEMATIC_FILE, schematicFile);
        root.putString(SCHEMATIC_OWNER, schematicOwner);
        root.putLongArray(SOURCE_BLOCKS, sourceBlocks.stream().mapToLong(BlockPos::asLong).toArray());
        root.put(SOURCE_SUPER_GLUE, writeGlueSnapshots(sourceSuperGlue));
        root.put(SOURCE_HONEY_GLUE, writeGlueSnapshots(sourceHoneyGlue));

        final CompoundTag custom = new CompoundTag();
        custom.put(ROOT_TAG, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    private static CompoundTag readMoverData(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        final CompoundTag copied = customData.copyTag();
        if (!copied.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return copied.getCompound(ROOT_TAG);
    }

    private static Set<BlockPos> readSourceBlocks(final CompoundTag root) {
        final Set<BlockPos> blocks = new HashSet<>();
        for (final long packed : root.getLongArray(SOURCE_BLOCKS)) {
            blocks.add(BlockPos.of(packed));
        }
        return blocks;
    }

    private static ListTag writeGlueSnapshots(final List<GlueBoxSnapshot> snapshots) {
        final ListTag list = new ListTag();
        for (final GlueBoxSnapshot snapshot : snapshots) {
            final CompoundTag tag = new CompoundTag();
            tag.putDouble(BB_MIN_X, snapshot.minX);
            tag.putDouble(BB_MIN_Y, snapshot.minY);
            tag.putDouble(BB_MIN_Z, snapshot.minZ);
            tag.putDouble(BB_MAX_X, snapshot.maxX);
            tag.putDouble(BB_MAX_Y, snapshot.maxY);
            tag.putDouble(BB_MAX_Z, snapshot.maxZ);
            list.add(tag);
        }
        return list;
    }

    private static List<GlueBoxSnapshot> readGlueSnapshots(final ListTag list) {
        final List<GlueBoxSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag tag = list.getCompound(i);
            snapshots.add(new GlueBoxSnapshot(
                    tag.getDouble(BB_MIN_X),
                    tag.getDouble(BB_MIN_Y),
                    tag.getDouble(BB_MIN_Z),
                    tag.getDouble(BB_MAX_X),
                    tag.getDouble(BB_MAX_Y),
                    tag.getDouble(BB_MAX_Z)
            ));
        }
        return snapshots;
    }

    private static boolean removeSourceBlocksHard(final ServerLevel level, final Set<BlockPos> sourceBlocks) {
        boolean ok = true;
        for (final BlockPos pos : sourceBlocks) {
            if (!level.isLoaded(pos)) {
                ok = false;
                continue;
            }
            level.removeBlockEntity(pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return ok;
    }

    private static boolean removeGlueSnapshots(final ServerLevel level, final List<GlueBoxSnapshot> snapshots, final boolean honey) {
        boolean removed = true;
        for (final GlueBoxSnapshot snapshot : snapshots) {
            final AABB scan = snapshot.toAabb().inflate(0.1d);
            if (honey) {
                for (final HoneyGlueEntity glue : level.getEntitiesOfClass(HoneyGlueEntity.class, scan)) {
                    if (snapshot.matches(glue.getBoundingBox())) {
                        glue.discard();
                    }
                }
            } else {
                for (final SuperGlueEntity glue : level.getEntitiesOfClass(SuperGlueEntity.class, scan)) {
                    if (snapshot.matches(glue.getBoundingBox())) {
                        glue.discard();
                    }
                }
            }
        }
        return removed;
    }

    private static boolean ensureGlueSnapshots(final ServerLevel level, final List<GlueBoxSnapshot> snapshots, final boolean honey) {
        boolean ok = true;
        for (final GlueBoxSnapshot snapshot : snapshots) {
            final AABB scan = snapshot.toAabb().inflate(0.1d);
            boolean exists = false;
            if (honey) {
                for (final HoneyGlueEntity glue : level.getEntitiesOfClass(HoneyGlueEntity.class, scan)) {
                    if (snapshot.matches(glue.getBoundingBox())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    level.addFreshEntity(new HoneyGlueEntity(level, snapshot.toAabb()));
                }
            } else {
                for (final SuperGlueEntity glue : level.getEntitiesOfClass(SuperGlueEntity.class, scan)) {
                    if (snapshot.matches(glue.getBoundingBox())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    level.addFreshEntity(new SuperGlueEntity(level, snapshot.toAabb()));
                }
            }
        }
        return ok;
    }

    private static void consumeSessionSchematicAndRestoreTool(final ServerPlayer player, final String sessionId, final String mode) {
        final Item replacementItem = MODE_MOVE.equals(mode) ? ModItems.GLUED_CONTRAPTION_MOVER.get() : ModItems.GLUED_CONTRAPTION_CLONER.get();
        final ItemStack replacement = new ItemStack(replacementItem);
        boolean restored = false;

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            final ItemStack invStack = player.getInventory().items.get(i);
            if (!isSessionStack(invStack, sessionId)) {
                continue;
            }
            if (!restored) {
                player.getInventory().items.set(i, replacement.copy());
                restored = true;
            } else {
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            final ItemStack offhandStack = player.getInventory().offhand.get(i);
            if (!isSessionStack(offhandStack, sessionId)) {
                continue;
            }
            if (!restored) {
                player.getInventory().offhand.set(i, replacement.copy());
                restored = true;
            } else {
                player.getInventory().offhand.set(i, ItemStack.EMPTY);
            }
        }

        if (!restored) {
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, replacement.copy());
            } else if (player.getOffhandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, replacement.copy());
            } else {
                player.getInventory().add(replacement.copy());
            }
        }
    }

    private static boolean isSessionStack(final ItemStack stack, final String sessionId) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SchematicItem)) {
            return false;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        final CompoundTag copied = customData.copyTag();
        if (!copied.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        return sessionId.equals(copied.getCompound(ROOT_TAG).getString(SESSION_ID));
    }

    private static boolean cleanupSchematicFile(final CompoundTag moverData) {
        final String schematicFile = moverData.getString(SCHEMATIC_FILE);
        final String owner = moverData.getString(SCHEMATIC_OWNER);
        if (schematicFile.isBlank() || owner.isBlank()) {
            return false;
        }

        final Path baseDir = CreatePaths.UPLOADED_SCHEMATICS_DIR;
        final Path playerDir = baseDir.resolve(owner).normalize();
        final Path filePath = playerDir.resolve(schematicFile).normalize();
        if (!playerDir.startsWith(baseDir) || !filePath.startsWith(playerDir)) {
            return false;
        }

        try {
            Files.deleteIfExists(filePath);
            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

    private record GatherResult(Set<BlockPos> blocks, BlockPos min, BlockPos max, boolean tooLarge) {
    }

    private record GlueBoxSnapshot(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private static final double EPS = 1.0E-4;

        static GlueBoxSnapshot fromAabb(final AABB aabb) {
            return new GlueBoxSnapshot(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        }

        AABB toAabb() {
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        boolean matches(final AABB aabb) {
            return almost(minX, aabb.minX)
                    && almost(minY, aabb.minY)
                    && almost(minZ, aabb.minZ)
                    && almost(maxX, aabb.maxX)
                    && almost(maxY, aabb.maxY)
                    && almost(maxZ, aabb.maxZ);
        }

        private static boolean almost(final double a, final double b) {
            return Math.abs(a - b) <= EPS;
        }
    }
}
