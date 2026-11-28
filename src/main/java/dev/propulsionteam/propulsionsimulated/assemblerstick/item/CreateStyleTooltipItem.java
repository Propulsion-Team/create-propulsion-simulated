package dev.propulsionteam.propulsionsimulated.assemblerstick.item;

import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class CreateStyleTooltipItem extends Item {
    public CreateStyleTooltipItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> tooltipComponents,
            final TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        final ItemDescription description = ItemDescription.create(this, Palette.STANDARD_CREATE);
        if (description == null) {
            return;
        }
        final List<Component> lines = new ArrayList<>(description.getCurrentLines());
        if (description.getCurrentLines() == description.linesOnShift()) {
            lines.add(Component.empty());
        }
        tooltipComponents.addAll(Math.min(1, tooltipComponents.size()), lines);
    }
}
