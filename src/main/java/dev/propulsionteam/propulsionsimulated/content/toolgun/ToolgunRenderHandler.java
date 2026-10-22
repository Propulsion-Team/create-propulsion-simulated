package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.util.LinkedList;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ToolgunRenderHandler extends ZapperRenderHandler {
    private static ToolgunRenderHandler instance = new ToolgunRenderHandler();

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return stack.is(PropulsionItems.TOOLGUN.get());
    }

    public static void fire(InteractionHand hand, Vec3 start, Vec3 end, boolean hitBlock) {
        if (instance.cachedBeams == null) {
            instance.cachedBeams = new LinkedList<>();
        }
        LaserBeam beam = new LaserBeam(start, end);
        if (hitBlock) {
            instance.addBeam(beam);
        } else {
            instance.cachedBeams.add(beam);
        }
        instance.shoot(hand, start);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        instance.onRenderPlayerHand(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            instance.tick();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (instance.cachedBeams != null) {
            instance.cachedBeams.forEach(beam -> Outliner.getInstance().remove(beam));
        }
        instance = new ToolgunRenderHandler();
    }
}
