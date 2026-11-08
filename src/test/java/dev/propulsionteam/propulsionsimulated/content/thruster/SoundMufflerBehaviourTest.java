package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundMufflerBehaviourTest {
    @BeforeAll
    static void bootstrap() {
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void emptyThrustersHaveNormalVolumeAndOneUpgradeSlot() {
        var blockEntity = new TestBlockEntity();
        var inventory = blockEntity.getBehaviour(SoundMufflerBehaviour.TYPE).getItemHandler();

        assertEquals(1, inventory.getSlots());
        assertEquals(1, inventory.getSlotLimit(0));
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(1.0f, SoundMufflerBehaviour.getVolumeMultiplier(blockEntity));
    }

    @Test
    void storageIsVisibleButCannotBeChangedByAutomation() {
        var behaviour = new TestBlockEntity().getBehaviour(SoundMufflerBehaviour.TYPE);
        var inventory = behaviour.getItemHandler();
        // Use a vanilla stand-in: unit tests do not load the mod's deferred item registry.
        ItemStack stored = new ItemStack(Items.GRAY_WOOL);
        inventory.setStackInSlot(0, stored);
        for (boolean simulate : List.of(false, true)) {
            assertEquals(1, inventory.insertItem(0, stored.copy(), simulate).getCount());
            assertTrue(inventory.extractItem(0, 1, simulate).isEmpty());
            assertEquals(1, inventory.getStackInSlot(0).getCount());
        }
    }

    @Test
    void mufflerInEveryMemberQuietsTheWholeMultiblock() {
        for (boolean creative : List.of(false, true)) {
            for (int size : List.of(2, 3)) {
                Map<BlockPos, BlockEntity> members = multiblock(creative, size);
                for (BlockEntity installedIn : members.values()) {
                    var inventory = BlockEntityBehaviour.get(installedIn, SoundMufflerBehaviour.TYPE).getItemHandler();
                    inventory.setStackInSlot(0, new ItemStack(Items.GRAY_WOOL));
                    for (BlockEntity listener : members.values()) {
                        assertEquals(0.3f, SoundMufflerBehaviour.getVolumeMultiplier(listener, members::get),
                                "Muffler at " + installedIn.getBlockPos() + ", listener at " + listener.getBlockPos());
                    }
                    inventory.setStackInSlot(0, ItemStack.EMPTY);
                    assertEquals(1.0f, SoundMufflerBehaviour.getVolumeMultiplier(installedIn, members::get));
                }
            }
        }
    }

    @Test
    void multipleMufflersNeverStackAndRemovingOneLeavesTheOthersEffective() {
        for (boolean creative : List.of(false, true)) {
            Map<BlockPos, BlockEntity> members = multiblock(creative, 3);
            for (BlockEntity member : members.values()) {
                BlockEntityBehaviour.get(member, SoundMufflerBehaviour.TYPE).getItemHandler()
                        .setStackInSlot(0, new ItemStack(Items.GRAY_WOOL));
            }
            int remaining = members.size();
            for (BlockEntity removedFrom : members.values()) {
                for (BlockEntity listener : members.values()) {
                    assertEquals(0.3f, SoundMufflerBehaviour.getVolumeMultiplier(listener, members::get));
                }
                BlockEntityBehaviour.get(removedFrom, SoundMufflerBehaviour.TYPE).getItemHandler()
                        .setStackInSlot(0, ItemStack.EMPTY);
                remaining--;
                assertEquals(remaining > 0 ? 0.3f : 1.0f,
                        SoundMufflerBehaviour.getVolumeMultiplier(removedFrom, members::get));
            }
        }
    }

    @Test
    void mufflersInOtherThrustersAreNotShared() {
        for (boolean creative : List.of(false, true)) {
            Map<BlockPos, BlockEntity> members = multiblock(creative, 3);
            BlockPos origin = new BlockPos(-9, 42, 14);
            BlockEntity controller = members.get(origin);
            AbstractThrusterBlockEntity unrelated = member(creative, origin.offset(1, 1, 1), null, 1);
            BlockEntityBehaviour.get(unrelated, SoundMufflerBehaviour.TYPE).getItemHandler()
                    .setStackInSlot(0, new ItemStack(Items.GRAY_WOOL));
            members.put(unrelated.getBlockPos(), unrelated);

            assertEquals(1.0f, SoundMufflerBehaviour.getVolumeMultiplier(controller, members::get));
        }
    }

    private static Map<BlockPos, BlockEntity> multiblock(boolean creative, int size) {
        BlockPos origin = new BlockPos(-9, 42, 14);
        AbstractThrusterBlockEntity controller = member(creative, origin, null, size);
        Map<BlockPos, BlockEntity> members = new HashMap<>();
        members.put(origin, controller);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            if (!pos.equals(origin)) members.put(pos.immutable(), member(creative, pos.immutable(), controller, size));
        }
        return members;
    }

    private static AbstractThrusterBlockEntity member(boolean creative, BlockPos pos,
                                                       AbstractThrusterBlockEntity controller, int size) {
        return creative ? new TestCreativeThruster(pos, (TestCreativeThruster) controller, size)
                : new TestRegularThruster(pos, (TestRegularThruster) controller, size);
    }

    private static class TestRegularThruster extends ThrusterBlockEntity {
        private final TestRegularThruster controller;

        TestRegularThruster(BlockPos pos, TestRegularThruster controller, int size) {
            super(BlockEntityType.CHEST, pos, Blocks.CHEST.defaultBlockState());
            this.controller = controller;
            width = size;
        }

        @Override
        public TestRegularThruster getControllerBE() {
            return controller == null ? this : controller;
        }

        @Override
        public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
            behaviours.add(new SoundMufflerBehaviour(this));
        }
    }

    private static class TestCreativeThruster extends CreativeThrusterBlockEntity {
        private final TestCreativeThruster controller;

        TestCreativeThruster(BlockPos pos, TestCreativeThruster controller, int size) {
            super(BlockEntityType.CHEST, pos, Blocks.CHEST.defaultBlockState());
            this.controller = controller;
            width = size;
        }

        @Override
        public TestCreativeThruster getControllerBE() {
            return controller == null ? this : controller;
        }

        @Override
        public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
            behaviours.add(new SoundMufflerBehaviour(this));
        }
    }

    @Test
    void itemAndComponentsSurviveSaveAndClientSync() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (boolean clientPacket : List.of(false, true)) {
            var original = new TestBlockEntity();
            var behaviour = original.getBehaviour(SoundMufflerBehaviour.TYPE);
            ItemStack stored = new ItemStack(Items.GRAY_WOOL);
            stored.set(DataComponents.CUSTOM_NAME, Component.literal("Quiet please"));
            behaviour.getItemHandler().setStackInSlot(0, stored);
            CompoundTag tag = new CompoundTag();
            behaviour.write(tag, registries, clientPacket);

            var restored = new TestBlockEntity();
            var restoredBehaviour = restored.getBehaviour(SoundMufflerBehaviour.TYPE);
            restoredBehaviour.read(tag, registries, clientPacket);

            assertTrue(ItemStack.isSameItemSameComponents(stored, restoredBehaviour.getItemHandler().getStackInSlot(0)));
            assertEquals(0.3f, SoundMufflerBehaviour.getVolumeMultiplier(restored));
            restoredBehaviour.read(new CompoundTag(), registries, clientPacket);
            assertTrue(restoredBehaviour.getItemHandler().getStackInSlot(0).isEmpty());
            assertEquals(1.0f, SoundMufflerBehaviour.getVolumeMultiplier(restored));
        }
    }

    private static class TestBlockEntity extends SmartBlockEntity {
        TestBlockEntity() {
            super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        }

        @Override
        public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
            behaviours.add(new SoundMufflerBehaviour(this));
        }
    }
}
