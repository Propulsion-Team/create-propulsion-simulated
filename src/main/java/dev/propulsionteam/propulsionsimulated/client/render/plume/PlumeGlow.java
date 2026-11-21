package dev.propulsionteam.propulsionsimulated.client.render.plume;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class PlumeGlow {
    private static final ResourceLocation BUFFERS = ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "plume_glow");
    private static final float RADIUS = 10.0f;
    private static final int[] PIXEL_STORE = {3314, 3315, 3316, 32878, 32877, 3312, 3313, 3317};
    private static final Map<BlockPos, Light> LIGHTS = new HashMap<>();
    private static boolean ready;
    private static boolean failed;

    private PlumeGlow() {
    }

    public static void keep(BlockPos pos, Vec3 at, long gameTime, float time, float power, float r, float g, float b) {
        if (!ready) return;
        try {
            Light light = LIGHTS.get(pos);
            if (light == null || !light.handle.isValid()) {
                PointLightData created = new PointLightData();
                light = new Light(VeilRenderSystem.renderer().getLightRenderer().addLight(created));
                LIGHTS.put(pos.immutable(), light);
            }
            PointLightData data = light.handle.getLightData();
            float flicker = 1.0f + 0.12f * (float) Math.sin(time * 1.7f + pos.hashCode()) + 0.05f * (float) Math.sin(time * 4.3f);
            data.setColor(r, g, b);
            data.setPosition(at.x, at.y, at.z);
            data.setBrightness(1.5f * power * flicker);
            data.setRadius(RADIUS * (0.4f + 0.6f * power));
            light.handle.markDirty();
            light.seen = gameTime;
        } catch (Throwable t) {
            failed = true;
            shutDown();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean wanted = mc.level != null && !failed && PropulsionConfig.useVeilLight();
        if (!wanted) {
            if (ready) shutDown();
            return;
        }
        if (!ready) {
            startUp();
            return;
        }
        long now = mc.level.getGameTime();
        Iterator<Light> it = LIGHTS.values().iterator();
        while (it.hasNext()) {
            Light light = it.next();
            if (now - light.seen <= 3L && now >= light.seen) continue;
            try {
                light.handle.free();
            } catch (Throwable ignored) {
            }
            it.remove();
        }
    }

    private static void startUp() {
        try {
            createShadowTexture();
            VeilRenderSystem.renderer().enableBuffers(BUFFERS, DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL);
            ready = true;
        } catch (Throwable t) {
            failed = true;
        }
    }

    private static void shutDown() {
        for (Light light : LIGHTS.values()) {
            try {
                light.handle.free();
            } catch (Throwable ignored) {
            }
        }
        LIGHTS.clear();
        if (ready) {
            try {
                VeilRenderSystem.renderer().disableBuffers(BUFFERS);
            } catch (Throwable ignored) {
            }
        }
        ready = false;
    }

    private static void createShadowTexture() throws ReflectiveOperationException {
        Class<?> grid;
        try {
            grid = Class.forName("foundry.veil.impl.client.render.light.VoxelShadowGrid");
        } catch (ClassNotFoundException e) {
            return;
        }
        if ((Integer) grid.getMethod("getTextureId").invoke(null) != 0) return;
        Method create = grid.getDeclaredMethod("ensureTexture");
        create.setAccessible(true);

        int[] saved = new int[PIXEL_STORE.length];
        for (int i = 0; i < PIXEL_STORE.length; i++) {
            saved[i] = GL11C.glGetInteger(PIXEL_STORE[i]);
            GL11C.glPixelStorei(PIXEL_STORE[i], i == PIXEL_STORE.length - 1 ? 1 : 0);
        }
        int unpackBuffer = GL11C.glGetInteger(35055);
        int texture3d = GL11C.glGetInteger(32874);
        GL15C.glBindBuffer(35052, 0);
        try {
            create.invoke(null);
        } finally {
            GL11C.glBindTexture(32879, texture3d);
            GL15C.glBindBuffer(35052, unpackBuffer);
            for (int i = 0; i < PIXEL_STORE.length; i++) {
                GL11C.glPixelStorei(PIXEL_STORE[i], saved[i]);
            }
        }
    }

    private static final class Light {
        final LightRenderHandle<PointLightData> handle;
        long seen;

        Light(LightRenderHandle<PointLightData> handle) {
            this.handle = handle;
        }
    }
}
