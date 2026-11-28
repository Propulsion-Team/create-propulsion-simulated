package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ThrusterSoundHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONTROLLER_CLASS = "dev.createpropulsionsimulated.client.sound.ThrusterLoopSoundController";

    private static MethodHandle tickHandle;
    private static boolean resolutionFailed;
    private static boolean tickFailureLogged;

    private ThrusterSoundHooks() {
    }

    public static void clientTick(final AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null || !blockEntity.getLevel().isClientSide()) {
            return;
        }

        final MethodHandle handle = resolveTickHandle();
        if (handle == null) {
            return;
        }

        try {
            handle.invokeExact(blockEntity);
        } catch (final Error e) {
            if (!(e instanceof LinkageError)) {
                throw e;
            }
            logTickFailure(e);
        } catch (final Throwable t) {
            logTickFailure(t);
        }
    }

    private static void logTickFailure(final Throwable t) {
        if (!tickFailureLogged) {
            tickFailureLogged = true;
            LOGGER.error("Thruster sound controller tick failed; further failures will not be logged", t);
        }
    }

    private static MethodHandle resolveTickHandle() {
        if (tickHandle != null || resolutionFailed) {
            return tickHandle;
        }
        try {
            final Class<?> controller = Class.forName(CONTROLLER_CLASS);
            tickHandle = MethodHandles.publicLookup().findStatic(
                    controller, "tick", MethodType.methodType(void.class, AbstractThrusterBlockEntity.class));
        } catch (final ReflectiveOperationException | LinkageError e) {
            resolutionFailed = true;
            LOGGER.error("Failed to resolve thruster sound controller; thruster sounds are disabled", e);
        }
        return tickHandle;
    }
}
