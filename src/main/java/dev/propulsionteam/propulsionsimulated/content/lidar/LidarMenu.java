package dev.propulsionteam.propulsionsimulated.content.lidar;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class LidarMenu extends GhostItemMenu<LidarBlockEntity> {
    public static final int FILTER_X = 77, FILTER_Y = 78, FILTER_COLUMNS = 7, FILTER_STEP_X = 18, FILTER_STEP_Y = 19;
    public static final int INVENTORY_X = 33, INVENTORY_Y = 172;

    public LidarMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public LidarMenu(MenuType<?> type, int id, Inventory inv, LidarBlockEntity lidar) {
        super(type, id, inv, lidar);
    }

    public static LidarMenu create(int id, Inventory inv, LidarBlockEntity lidar) {
        return new LidarMenu(PropulsionMenuTypes.LIDAR.get(), id, inv, lidar);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected LidarBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        return Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos()) instanceof LidarBlockEntity lidar ? lidar : null;
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler inventory = new ItemStackHandler(LidarBlockEntity.FILTER_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                if (contentHolder != null && !player.level().isClientSide)
                    contentHolder.setFilterStack(slot, getStackInSlot(slot));
            }
        };
        if (contentHolder != null)
            for (int i = 0; i < LidarBlockEntity.FILTER_SLOTS; i++)
                inventory.setStackInSlot(i, contentHolder.getFilterStack(i).copy());
        return inventory;
    }

    @Override
    protected boolean allowRepeats() {
        return false;
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(INVENTORY_X, INVENTORY_Y);
        for (int i = 0; i < LidarBlockEntity.FILTER_SLOTS; i++)
            addSlot(new SlotItemHandler(ghostInventory, i,
                FILTER_X + (i % FILTER_COLUMNS) * FILTER_STEP_X, FILTER_Y + (i / FILTER_COLUMNS) * FILTER_STEP_Y));
    }

    @Override
    protected void saveData(LidarBlockEntity lidar) {
        if (lidar == null || player.level().isClientSide) return;
        for (int i = 0; i < ghostInventory.getSlots(); i++)
            lidar.setFilterStack(i, ghostInventory.getStackInSlot(i));
    }

    @Override
    public boolean stillValid(Player player) {
        return contentHolder != null && contentHolder.canPlayerUse(player);
    }

    public ItemStack filterStack(int slot) {
        return ghostInventory.getStackInSlot(slot);
    }
}
