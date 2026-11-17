package dev.propulsionteam.propulsionsimulated.client;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.network.PlumeRenderModePacket;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tells the server this client's plume render mode on join and whenever the client config changes. */
@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class PlumeRenderModeSync {
    private PlumeRenderModeSync() {}

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        send();
    }

    @SubscribeEvent
    public static void onClientConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == PropulsionConfig.CLIENT_SPEC) {
            Minecraft.getInstance().execute(PlumeRenderModeSync::send);
        }
    }

    private static void send() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        PacketDistributor.sendToServer(new PlumeRenderModePacket(PropulsionConfig.useShaderPlumes()));
    }
}
