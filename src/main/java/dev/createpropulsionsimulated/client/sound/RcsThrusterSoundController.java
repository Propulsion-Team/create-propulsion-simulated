package dev.createpropulsionsimulated.client.sound;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.SoundMufflerBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionSoundEvents;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RcsThrusterSoundController {
    private static final Map<RcsThrusterBlockEntity, RcsSound> SOUNDS = new IdentityHashMap<>();

    private RcsThrusterSoundController() {}

    public static void tick(RcsThrusterBlockEntity be) {
        var manager = Minecraft.getInstance().getSoundManager();
        RcsSound sound = SOUNDS.get(be);
        if (be.isActive()) {
            if (sound != null && sound.isLooping() && !sound.isStopped()) return;
            if (sound != null) manager.stop(sound);
            sound = new RcsSound(be, true);
        } else {
            if (sound == null || !sound.isLooping()) return;
            manager.stop(sound);
            sound = new RcsSound(be, false);
        }
        SOUNDS.put(be, sound);
        manager.play(sound);
    }

    public static void cleanup() {
        var manager = Minecraft.getInstance().getSoundManager();
        SOUNDS.entrySet().removeIf(entry -> {
            RcsSound sound = entry.getValue();
            if (!isUsable(entry.getKey())) {
                manager.stop(sound);
                return true;
            }
            return !sound.isLooping() && !manager.isActive(sound);
        });
    }

    public static void reset() {
        SOUNDS.clear();
    }

    private static boolean isUsable(RcsThrusterBlockEntity be) {
        var level = Minecraft.getInstance().level;
        return !be.isRemoved() && !be.isChunkUnloaded() && level != null && be.getLevel() != null
                && level.dimension().equals(be.getLevel().dimension());
    }

    private static class RcsSound extends AbstractTickableSoundInstance {
        private final RcsThrusterBlockEntity be;

        private RcsSound(RcsThrusterBlockEntity be, boolean loop) {
            super((loop ? PropulsionSoundEvents.RCS_LOOP : PropulsionSoundEvents.RCS_OFF).get(), SoundSource.BLOCKS, RandomSource.create());
            this.be = be;
            looping = loop;
            delay = 0;
            attenuation = SoundInstance.Attenuation.NONE;
            updatePosition();
        }

        @Override
        public void tick() {
            if (!isUsable(be)) {
                stop();
                return;
            }
            updatePosition();
        }

        @Override
        public boolean canStartSilent() { return true; }

        private void updatePosition() {
            Vec3 center = Sable.HELPER.projectOutOfSubLevel(be.getLevel(), Vec3.atCenterOf(be.getBlockPos()));
            x = center.x;
            y = center.y;
            z = center.z;
            var player = Minecraft.getInstance().player;
            double distance = player == null ? Double.POSITIVE_INFINITY : Math.sqrt(player.distanceToSqr(center));
            volume = PropulsionConfig.RCS_SOUND_VOLUME.get().floatValue()
                    * (float) Math.max(0, 1 - distance / PropulsionConfig.RCS_SOUND_RANGE.get())
                    * SoundMufflerBehaviour.getVolumeMultiplier(be);
        }
    }
}
