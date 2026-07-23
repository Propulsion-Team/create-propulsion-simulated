package dev.propulsionteam.propulsionsimulated.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionCreativeTab;
import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.mixin.accessor.CreativeModeInventoryScreenAccessor;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.color.Colorc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PropulsionCreativeTabRenderer {
    private PropulsionCreativeTabRenderer() {
    }

    public static void render(CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        int left = ((CreativeModeInventoryScreenAccessor) screen).getLeftPos() + 8;
        int top = ((CreativeModeInventoryScreenAccessor) screen).getTopPos() + 17;
        poseStack.translate(left, top, 0);

        for (ResourceLocation sectionId : PropulsionCreativeTab.SECTIONS) {
            SimulatedSection section = SimResourceManagers.SIMULATED_SECTION.get(sectionId);
            Integer sectionY = PropulsionCreativeTab.SECTION_ROWS.get(sectionId);
            if (section == null || sectionY == null) {
                continue;
            }

            int sectionRow = sectionY - SimulatedCreativeTab.CURRENT_ROW;
            if (sectionRow < 0 || sectionRow > 4) {
                continue;
            }

            renderSection(section, graphics, mouseX, mouseY, left, top, sectionRow * 18);
        }

        poseStack.popPose();
        RenderSystem.disableDepthTest();
    }

    private static void renderSection(
            SimulatedSection section, GuiGraphics graphics, int mouseX, int mouseY, int left, int top, int y) {
        int width = 162;
        int height = 18;
        ResourceLocation bannerTexture = section.sprite();

        if (section.animateOnHover()) {
            boolean hovering = mouseX >= left
                    && mouseX <= left + width
                    && mouseY >= top + y
                    && mouseY <= top + y + height;
            SimulatedCreativeTab.setPlaying(bannerTexture, hovering);
        }

        graphics.blitSprite(bannerTexture, 0, y, width, height);

        Font font = Minecraft.getInstance().font;
        Component text = section.title().text();
        int textWidth = font.width(text);
        Colorc background = section.title().background();
        graphics.fill(2, y + 2, textWidth + 8, y + height - 2, background.argb());

        Colorc light = section.title().color();
        Colorc dark = section.title().secondaryColor().orElse(light.darken(0.2f, new Color()));
        SimulatedCreativeTab.drawAuraText(graphics, text, dark.argb(), light.argb(), 5, y + 5);
    }
}
