package dev.propulsionteam.propulsionsimulated.content.lidar;

import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.network.LidarSettingsPacket;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class LidarScreen extends AbstractSimiContainerScreen<LidarMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "textures/gui/lidar.png");
    private static final String KEY = "gui." + CreatePropulsion.ID + ".lidar.";
    private static final int WIDTH = 226, HEIGHT = 150;
    private static final int TOGGLE_X = 24, TOGGLE_Y = 34, TOGGLE_STEP = 20;
    private static final int TOGGLE_OFF_U = 0, TOGGLE_ON_U = 20, TOGGLE_V = 152, TOGGLE_W = 18, TOGGLE_H = 24;
    private static final int FILTER_MODE_X = 24, FILTER_MODE_Y = 96;
    private static final int OUTPUT_MODE_X = 4, OUTPUT_MODE_Y = 129;
    private static final int SWITCH_W = 49, SWITCH_H = 18;
    private static final int INVERT_X = 204, INVERT_Y = 128;
    private static final int INVERT_OFF_U = 40, INVERT_ON_U = 60, INVERT_V = 152;
    private static final int INVERT_ICON_U = 80, INVERT_ICON_V = 152, INVERT_ICON_W = 12, INVERT_ICON_H = 6;
    private static final int INVERT_ICON_X = 3, INVERT_ICON_Y = 6;
    private static final int TITLE_COLOR = 0xE2E2E2;

    private int detection;
    private boolean exclude;
    private boolean binary;
    private boolean inverted;

    public LidarScreen(LidarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT + 4 + 108);
        super.init();
        LidarBlockEntity lidar = menu.contentHolder;
        detection = lidar.getDetection();
        exclude = lidar.isExclude();
        binary = lidar.isBinary();
        inverted = lidar.isInverted();

        LidarDetection[] options = LidarDetection.values();
        for (int i = 0; i < options.length; i++)
            addRenderableWidget(new Toggle(leftPos + TOGGLE_X + i * TOGGLE_STEP, topPos + TOGGLE_Y, options[i]));

        addSwitch(FILTER_MODE_X, FILTER_MODE_Y, "filter", exclude ? 1 : 0,
            List.of(text("filter.include"), text("filter.exclude")),
            state -> text(state == 0 ? "filter.include.next" : "filter.exclude.next"),
            state -> {
                exclude = state == 1;
                sendSettings();
            });
        addSwitch(OUTPUT_MODE_X, OUTPUT_MODE_Y, "output", binary ? 1 : 0,
            List.of(text("output.analog"), text("output.binary")),
            state -> text(state == 0 ? "output.analog.next" : "output.binary.next"),
            state -> {
                binary = state == 1;
                sendSettings();
            });
        addRenderableWidget(new InvertButton(leftPos + INVERT_X, topPos + INVERT_Y));
    }

    private void addSwitch(int x, int y, String key, int state, List<MutableComponent> options,
                           IntFunction<MutableComponent> next, Consumer<Integer> onChange) {
        Label label = new Label(leftPos + x + 5, topPos + y + 5, Component.empty()).withShadow();
        Switch input = new Switch(leftPos + x, topPos + y, next);
        input.forOptions(options);
        input.titled(text(key));
        input.writingTo(label);
        input.setState(state);
        input.calling(onChange);
        label.active = false;
        addRenderableWidget(input);
        addRenderableWidget(label);
    }

    private void sendSettings() {
        PacketDistributor.sendToServer(new LidarSettingsPacket(detection, exclude, binary, inverted));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        renderPlayerInventory(graphics, leftPos + (WIDTH - 176) / 2, topPos + HEIGHT + 4);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        graphics.drawString(font, title, leftPos + (WIDTH - font.width(title)) / 2, topPos + 3, TITLE_COLOR, false);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index >= 36)
            graphics.renderComponentTooltip(font, List.of(
                header(text("filter.slot")),
                text("filter.slot.description").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
    }

    private static MutableComponent text(String key) {
        return Component.translatable(KEY + key);
    }

    private static MutableComponent header(MutableComponent text) {
        return text.withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB()));
    }

    private static MutableComponent hint(MutableComponent text) {
        return text.withStyle(s -> s.withColor(AbstractSimiWidget.HINT_RGB.getRGB()));
    }

    private static MutableComponent state(boolean on) {
        return text("state").withStyle(ChatFormatting.GRAY)
            .append(text(on ? "state.on" : "state.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static ItemStack icon(LidarDetection detection) {
        ItemLike item = switch (detection) {
            case BLOCKS -> Items.GRASS_BLOCK;
            case FLUIDS -> Items.WATER_BUCKET;
            case NON_SOLID -> Items.SHORT_GRASS;
            case SEE_THROUGH -> Items.GLASS;
            case ENTITIES -> Items.ZOMBIE_HEAD;
            case PLAYERS -> Items.PLAYER_HEAD;
            case SHIPS -> Items.OAK_BOAT;
            case OWN_SHIP -> PropulsionBlocks.LIDAR.get();
            case PRECISE -> Items.OAK_FENCE;
        };
        return new ItemStack(item);
    }

    private class Toggle extends AbstractSimiWidget {
        private final LidarDetection option;
        private final ItemStack icon;

        Toggle(int x, int y, LidarDetection option) {
            super(x, y, TOGGLE_W, TOGGLE_H);
            this.option = option;
            this.icon = icon(option);
            updateTooltip();
        }

        private boolean on() {
            return option.in(detection);
        }

        private void updateTooltip() {
            String base = "detect." + option.id;
            List<Component> lines = new ArrayList<>();
            lines.add(header(text(base)));
            lines.add(text(base + ".description").withStyle(ChatFormatting.GRAY));
            lines.add(state(on()));
            lines.add(hint(text(base + (on() ? ".disable" : ".enable"))));
            toolTip.clear();
            toolTip.addAll(lines);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            detection ^= option.bit();
            updateTooltip();
            sendSettings();
        }

        @Override
        protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            boolean on = on();
            graphics.blit(TEXTURE, getX(), getY(), on ? TOGGLE_ON_U : TOGGLE_OFF_U, TOGGLE_V, TOGGLE_W, TOGGLE_H);
            if (isHovered) graphics.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x30FFFFFF);
            graphics.renderItem(icon, getX() + 1, getY() + 1);
        }
    }

    private class InvertButton extends AbstractSimiWidget {
        InvertButton(int x, int y) {
            super(x, y, 18, 18);
            updateTooltip();
        }

        private void updateTooltip() {
            toolTip.clear();
            toolTip.add(header(text("invert")));
            toolTip.add(text("invert.description").withStyle(ChatFormatting.GRAY));
            toolTip.add(state(inverted));
            toolTip.add(hint(text(inverted ? "invert.disable" : "invert.enable")));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            inverted = !inverted;
            updateTooltip();
            sendSettings();
        }

        @Override
        protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.blit(TEXTURE, getX(), getY(), inverted ? INVERT_ON_U : INVERT_OFF_U, INVERT_V, 18, 18);
            if (isHovered) graphics.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x30FFFFFF);
            graphics.blit(TEXTURE, getX() + INVERT_ICON_X, getY() + INVERT_ICON_Y,
                INVERT_ICON_U, INVERT_ICON_V, INVERT_ICON_W, INVERT_ICON_H);
        }
    }

    private static class Switch extends SelectionScrollInput {
        private final IntFunction<MutableComponent> next;

        Switch(int x, int y, IntFunction<MutableComponent> next) {
            super(x, y, SWITCH_W, SWITCH_H);
            this.next = next;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            state = state == 0 ? 1 : 0;
            onChanged();
        }

        @Override
        protected void updateTooltip() {
            if (next != null) hint = next.apply(state);
            super.updateTooltip();
        }
    }
}
