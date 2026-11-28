package dev.propulsionteam.propulsionsimulated.client;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.Backends;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.InstancingPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.engine.CommonCrumbling;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class FlywheelProgramPrewarm {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<InstanceType<?>> INSTANCE_TYPES = List.of(AllInstanceTypes.ROTATING, InstanceTypes.TRANSFORMED);
    private static final List<ContextShader> CONTEXTS = List.of(ContextShader.DEFAULT, ContextShader.EMBEDDED, ContextShader.CRUMBLING);

    private static Object warmedPrograms;
    private static boolean failed;

    private FlywheelProgramPrewarm() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (failed || Minecraft.getInstance().level == null) return;

        try {
            Backend backend = BackendManager.currentBackend();
            if (backend == Backends.INSTANCING) {
                InstancingPrograms programs = InstancingPrograms.get();
                if (programs == null || programs == warmedPrograms) return;
                warmedPrograms = programs;
                for (InstanceType<?> type : INSTANCE_TYPES) {
                    for (ContextShader context : CONTEXTS) {
                        programs.get(type, context, materialFor(context), PipelineCompiler.OitMode.OFF);
                    }
                }
            } else if (backend == Backends.INDIRECT) {
                IndirectPrograms programs = IndirectPrograms.get();
                if (programs == null || programs == warmedPrograms) return;
                warmedPrograms = programs;
                for (InstanceType<?> type : INSTANCE_TYPES) {
                    programs.getCullingProgram(type);
                    for (ContextShader context : CONTEXTS) {
                        programs.getIndirectProgram(type, context, materialFor(context), PipelineCompiler.OitMode.OFF);
                    }
                }
            }
        } catch (Throwable t) {
            failed = true;
            LOGGER.warn("Failed to prewarm Flywheel shader programs", t);
        }
    }

    private static Material materialFor(ContextShader context) {
        if (context != ContextShader.CRUMBLING) return Materials.SOLID_BLOCK;
        SimpleMaterial.Builder crumbling = SimpleMaterial.builder();
        CommonCrumbling.applyCrumblingProperties(crumbling, Materials.SOLID_BLOCK);
        return crumbling.build();
    }
}
