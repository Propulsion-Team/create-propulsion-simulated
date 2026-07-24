package dev.propulsionteam.propulsionsimulated.mixin.client;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionCreativeTab;
import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = SimulatedCreativeTab.class, remap = false)
public class SimulatedCreativeTabMixin {
    @ModifyVariable(method = "renderBanners", at = @At("STORE"), ordinal = 0)
    private static List<SimulatedSection> createpropulsion$hidePropulsionSections(
            List<SimulatedSection> sections) {
        return sections.stream()
                .filter(section -> !PropulsionCreativeTab.SECTIONS.contains(
                        SimResourceManagers.SIMULATED_SECTION.getId(section)))
                .toList();
    }
}
