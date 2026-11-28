package dev.propulsionteam.propulsionsimulated.client.tooltip;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class TooltipHandler {
    private static final List<ITooltipProvider> TOP_PROVIDERS = new ArrayList<>();

    static {
        TOP_PROVIDERS.add(new GenericSummaryTooltipProvider());
        TOP_PROVIDERS.add(new FuelTooltipProvider());
        TOP_PROVIDERS.add(new CoralFuelTooltipProvider());
    }

    private TooltipHandler() {
    }

    @SubscribeEvent
    public static void addToItemTooltip(final ItemTooltipEvent event) {
        if (event.getItemStack().isEmpty()) {
            return;
        }

        final List<Component> currentTooltip = event.getToolTip();
        final List<Component> topList = new ArrayList<>();
        for (final ITooltipProvider provider : TOP_PROVIDERS) {
            provider.addText(event, topList);
        }
        if (topList.isEmpty()) {
            return;
        }

        final int insertIndex = currentTooltip.isEmpty() ? 0 : 1;
        currentTooltip.addAll(insertIndex, topList);
    }

    @SubscribeEvent
    public static void addToTooltipComponents(final RenderTooltipEvent.GatherComponents event) {
        // Visual tooltip components are intentionally disabled; keep text-only tooltip flow.
    }

    public static void wrapShiftHoldText(final List<Component> tooltipList, final String langKey, final Runnable addDetailedContent) {
        wrapHoldText(tooltipList, langKey, "create.tooltip.keyShift", Screen.hasShiftDown(), addDetailedContent);
    }

    public static void wrapAltHoldText(final List<Component> tooltipList, final String langKey, final Runnable addDetailedContent) {
        wrapHoldText(tooltipList, langKey, "createpropulsion.tooltip.keyAlt", Screen.hasAltDown(), addDetailedContent);
    }

    private static void wrapHoldText(final List<Component> tooltipList, final String langKey, final String keyLangKey,
                                     final boolean isKeyDown, final Runnable addDetailedContent) {
        final Component keyComponent = Component.translatable(keyLangKey)
                .withStyle(isKeyDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        tooltipList.add(Component.translatable(langKey, keyComponent).withStyle(ChatFormatting.DARK_GRAY));

        if (isKeyDown) {
            tooltipList.add(Component.empty());
            addDetailedContent.run();
        }
    }
}
