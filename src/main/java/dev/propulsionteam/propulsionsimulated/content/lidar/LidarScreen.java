package dev.propulsionteam.propulsionsimulated.content.lidar;

import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.network.LidarSettingsPacket;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
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
    private static final int WIDTH = 227, HEIGHT = 157;
    private static final int TITLE_Y = 6;
    private static final int BUTTON_SIZE = 18, SPRITE_V = 160, BUTTON_OFF_U = 0, BUTTON_ON_U = 18;
    private static final int TOGGLE_X = 11, TOGGLE_Y = 32;
    private static final int TOGGLE_LIGHT_U = 36, TOGGLE_LIGHT_OFF_V = 160, TOGGLE_LIGHT_ON_V = 166, TOGGLE_LIGHT_H = 6;
    private static final int FILTER_MODE_X = 11, FILTER_MODE_Y = 72, FILTER_MODE_W = 40, FILTER_MODE_H = 16;
    private static final int OUTPUT_MODE_X = 179, OUTPUT_MODE_Y = 35, OUTPUT_MODE_W = 42, OUTPUT_MODE_H = 18;
    private static final int INVERT_X = 155, INVERT_Y = 71;
    private static final int INVERT_LIGHT_OFF_U = 54, INVERT_LIGHT_ON_U = 60, INVERT_LIGHT_W = 6;
    private static final int INVERT_ICON_U = 66, INVERT_ICON_W = 12, INVERT_ICON_H = 6;
    private static final int KNOB_U = 80, KNOB_W = 18, KNOB_H = 11, KNOB_X = 191;
    private static final int FILL_U = 100, FILL_W = 6, FILL_H = 69, FILL_X = 197, FILL_TOP = 65;
    private static final long SLIDER_SOUND_COOLDOWN_MS = 40;

    private int detection;
    private boolean exclude;
    private boolean binary;
    private boolean inverted;
    private int range;
    private Slider slider;

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
        range = lidar.getRange();

        LidarDetection[] options = LidarDetection.values();
        for (int i = 0; i < options.length; i++)
            addRenderableWidget(new Toggle(leftPos + TOGGLE_X + i * BUTTON_SIZE, topPos + TOGGLE_Y, options[i]));

        addSwitch(FILTER_MODE_X, FILTER_MODE_Y, FILTER_MODE_W, FILTER_MODE_H, "filter", exclude ? 1 : 0,
            List.of(text("filter.include"), text("filter.exclude")),
            state -> text(state == 0 ? "filter.include.next" : "filter.exclude.next"),
            state -> {
                exclude = state == 1;
                sendSettings();
            });
        addSwitch(OUTPUT_MODE_X, OUTPUT_MODE_Y, OUTPUT_MODE_W, OUTPUT_MODE_H, "output", binary ? 1 : 0,
            List.of(text("output.analog"), text("output.binary")),
            state -> text(state == 0 ? "output.analog.next" : "output.binary.next"),
            state -> {
                binary = state == 1;
                sendSettings();
            });
        addRenderableWidget(new InvertButton(leftPos + INVERT_X - INVERT_LIGHT_W, topPos + INVERT_Y));
        slider = addRenderableWidget(new Slider(leftPos + KNOB_X, topPos + FILL_TOP - KNOB_H / 2));
    }

    private void addSwitch(int x, int y, int width, int height, String key, int state, List<MutableComponent> options,
                           IntFunction<MutableComponent> next, Consumer<Integer> onChange) {
        Switch input = new Switch(leftPos + x, topPos + y, width, height, next);
        input.forOptions(options);
        input.titled(text(key));
        input.setState(state);
        input.calling(onChange);
        addRenderableWidget(input);
    }

    private void sendSettings() {
        PacketDistributor.sendToServer(new LidarSettingsPacket(detection, exclude, binary, inverted, range));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (slider != null && slider.dragging) {
            slider.moveTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (slider != null && slider.dragging) {
            slider.dragging = false;
            sendSettings();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        renderPlayerInventory(graphics, leftPos + (WIDTH - 176) / 2, topPos + HEIGHT + 4);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        graphics.drawString(font, title, leftPos + (WIDTH - font.width(title)) / 2, topPos + TITLE_Y, 0xFFFFFF, true);
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
            super(x, y, BUTTON_SIZE, BUTTON_SIZE + TOGGLE_LIGHT_H);
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
            graphics.blit(TEXTURE, getX(), getY(), on ? BUTTON_ON_U : BUTTON_OFF_U, SPRITE_V, BUTTON_SIZE, BUTTON_SIZE);
            graphics.blit(TEXTURE, getX(), getY() + BUTTON_SIZE, TOGGLE_LIGHT_U, on ? TOGGLE_LIGHT_ON_V : TOGGLE_LIGHT_OFF_V,
                BUTTON_SIZE, TOGGLE_LIGHT_H);
            if (isHovered) highlight(graphics, getX(), getY(), BUTTON_SIZE, BUTTON_SIZE);
            graphics.renderItem(icon, getX() + 1, getY() + 1);
        }
    }

    private class InvertButton extends AbstractSimiWidget {
        InvertButton(int x, int y) {
            super(x, y, INVERT_LIGHT_W + BUTTON_SIZE, BUTTON_SIZE);
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
            int x = getX() + INVERT_LIGHT_W;
            graphics.blit(TEXTURE, getX(), getY(), inverted ? INVERT_LIGHT_ON_U : INVERT_LIGHT_OFF_U, SPRITE_V,
                INVERT_LIGHT_W, BUTTON_SIZE);
            graphics.blit(TEXTURE, x, getY(), inverted ? BUTTON_ON_U : BUTTON_OFF_U, SPRITE_V, BUTTON_SIZE, BUTTON_SIZE);
            graphics.blit(TEXTURE, x + (BUTTON_SIZE - INVERT_ICON_W) / 2, getY() + (BUTTON_SIZE - INVERT_ICON_H) / 2,
                INVERT_ICON_U, SPRITE_V, INVERT_ICON_W, INVERT_ICON_H);
            if (isHovered) highlight(graphics, x, getY(), BUTTON_SIZE, BUTTON_SIZE);
        }
    }

    private static void highlight(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x30FFFFFF);
    }

    private static float sliderPitch(int range) {
        return 0.25f + (range * 15f / LidarBlockEntity.MAX_RANGE + 5) / 15f * 0.5f;
    }

    private class Slider extends AbstractSimiWidget {
        private static final int BOTTOM = FILL_TOP + FILL_H;
        boolean dragging;
        private long lastSound;

        Slider(int x, int y) {
            super(x, y, KNOB_W, FILL_H + KNOB_H);
            updateTooltip();
        }

        private int fillHeight() {
            return Math.round(range * (float) FILL_H / LidarBlockEntity.MAX_RANGE);
        }

        private void updateTooltip() {
            toolTip.clear();
            toolTip.add(header(text("range")));
            toolTip.add(text("range.description").withStyle(ChatFormatting.GRAY));
            toolTip.add(text("state").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(KEY + "range.value", range).withStyle(ChatFormatting.WHITE)));
            toolTip.add(hint(text("range.change")));
        }

        void moveTo(double mouseY) {
            int filled = topPos + BOTTOM - (int) Math.round(mouseY);
            setRange(Math.round(filled * (float) LidarBlockEntity.MAX_RANGE / FILL_H));
        }

        private void setRange(int value) {
            int clamped = Math.max(0, Math.min(LidarBlockEntity.MAX_RANGE, value));
            if (clamped == range) return;
            range = clamped;
            updateTooltip();
            long now = Util.getMillis();
            if (now - lastSound < SLIDER_SOUND_COOLDOWN_MS) return;
            lastSound = now;
            Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, sliderPitch(range), 0.2f));
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            dragging = true;
            moveTo(mouseY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (scrollY == 0) return false;
            int before = range;
            setRange(range + (int) Math.signum(scrollY) * (hasShiftDown() ? 8 : 1));
            if (range != before) sendSettings();
            return true;
        }

        @Override
        protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int filled = fillHeight();
            int fillTop = topPos + BOTTOM - filled;
            if (filled > 0)
                graphics.blit(TEXTURE, leftPos + FILL_X, fillTop, FILL_U, SPRITE_V + FILL_H - filled, FILL_W, filled);
            int knobY = fillTop - KNOB_H / 2;
            graphics.blit(TEXTURE, getX(), knobY, KNOB_U, SPRITE_V, KNOB_W, KNOB_H);
            if (isHovered || dragging) highlight(graphics, getX(), knobY, KNOB_W, KNOB_H);
            String value = String.valueOf(range);
            graphics.drawString(font, value, getX() + (KNOB_W - font.width(value) + 1) / 2, knobY + 2, 0xFFFFFF, true);
        }
    }

    private static class Switch extends SelectionScrollInput {
        private final IntFunction<MutableComponent> next;

        Switch(int x, int y, int width, int height, IntFunction<MutableComponent> next) {
            super(x, y, width, height);
            this.next = next;
        }

        @Override
        protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (state < 0 || state >= options.size()) return;
            Component label = options.get(state);
            Font font = Minecraft.getInstance().font;
            float scale = Math.min(1f, (width - 4f) / font.width(label));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + width / 2f, getY() + (height - 8 * scale) / 2f + 0.5f, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, label, -font.width(label) / 2, 0, 0xFFFFFF, true);
            graphics.pose().popPose();
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
