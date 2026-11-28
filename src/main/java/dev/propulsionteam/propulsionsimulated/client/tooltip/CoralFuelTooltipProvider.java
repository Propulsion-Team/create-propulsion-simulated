package dev.propulsionteam.propulsionsimulated.client.tooltip;

import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelManager;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelProperties;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

public final class CoralFuelTooltipProvider implements ITooltipProvider {
    @Override
    public void addText(final ItemTooltipEvent event, final List<Component> tooltipList) {
        final ItemStack stack = event.getItemStack();
        final IFluidHandlerItem fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) {
            return;
        }

        final FluidStack fluidStack = fluidHandler.getFluidInTank(0);
        if (fluidStack.isEmpty()) {
            return;
        }

        final CoralGeneratorFuelProperties properties = CoralGeneratorFuelManager.getProperties(fluidStack.getFluid());
        if (properties == null || properties.fePerMb() <= 0) {
            return;
        }

        TooltipHandler.wrapAltHoldText(tooltipList, "createpropulsion.tooltip.holdForCoralConversionSummary", () -> {
            final double fePerMb = properties.fePerMb();
            final String formatted = (fePerMb == Math.floor(fePerMb))
                    ? Integer.toString((int) fePerMb)
                    : String.format(java.util.Locale.ROOT, "%.1f", fePerMb);

            final Component conversionLine = Component.translatable("createpropulsion.tooltip.conversion_rate")
                    .append(": ")
                    .withStyle(Palette.STANDARD_CREATE.primary())
                    .append(Component.literal(formatted).withStyle(Palette.STANDARD_CREATE.highlight()))
                    .append(Component.literal(" FE/mB").withStyle(Palette.STANDARD_CREATE.primary()));
            tooltipList.add(conversionLine);
            tooltipList.add(Component.empty());
        });
    }
}
