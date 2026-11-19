package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.createpropulsionsimulated.client.sound.RcsThrusterSoundController;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkRenderer;
import dev.propulsionteam.propulsionsimulated.utility.value_boxes.DualRowValueRenderer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.InteractionKeyMappingTriggered event) {
        // Removed assembly gauge click handling.
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        DualRowValueRenderer.tick();
        VectorRedstoneLinkRenderer.tick();
        RcsThrusterSoundController.cleanup();
    }

    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> propulsionCommand = Commands.literal("propulsion");
        event.getDispatcher().register(propulsionCommand
                .then(Commands.literal("config")
                        .executes((ctx) -> {
                            openConfig();
                            return 1;
                        })));
    }

    private static void openConfig() {
        Screen parent = Minecraft.getInstance().screen;
        ScreenOpener.open(new BaseConfigScreen(parent, CreatePropulsion.ID));
    }
}
