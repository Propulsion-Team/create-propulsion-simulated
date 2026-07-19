package dev.createpropulsionsimulated.client.sound;

import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeResolver;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeSpec;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/** Coordinates startup, loop, and shutdown audio for every client-side thruster. */
public final class ThrusterLoopSoundController {
    private static final int TRANSITION_TICKS = 10;
    private static final int CROSSFADE_TICKS = 2;
    private static final float VOLUME_BOOST_MULTIPLIER = 1.5f;
    private static final int THRUSTER_SOUND_RANGE_BLOCKS = 100;
    private static final Map<String, ThrusterSoundState> ACTIVE_SOUNDS = new HashMap<>();

    private ThrusterLoopSoundController() {
    }

    /** Invalidates channels discarded by a sound-engine/device reload. */
    public static void onSoundEngineReload() {
        final Iterator<Map.Entry<String, ThrusterSoundState>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().prepareForSoundEngineReload()) {
                iterator.remove();
            }
        }
    }

    public static void tick(final AbstractThrusterBlockEntity blockEntity) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getSoundManager() == null) {
            return;
        }

        cleanupFinished();

        final String key = soundKey(blockEntity);
        final ThrusterPlumeSpec plume = ThrusterPlumeResolver.resolve(blockEntity);
        final ThrusterSoundProfile profile = ThrusterSoundProfile.forStyle(plume.style());
        ThrusterSoundState state = ACTIVE_SOUNDS.get(key);

        if (state != null && !state.isFor(blockEntity)) {
            state.dispose();
            ACTIVE_SOUNDS.remove(key);
            state = null;
        }

        if (plume.active()) {
            if (state == null) {
                state = new ThrusterSoundState(key, blockEntity, profile);
                ACTIVE_SOUNDS.put(key, state);
                state.activate();
            } else {
                state.tickActive(profile);
            }
            return;
        }

        if (state != null) {
            state.deactivate();
            if (state.isFinished()) {
                ACTIVE_SOUNDS.remove(key);
            }
        }
    }

    private static void cleanupFinished() {
        final Iterator<Map.Entry<String, ThrusterSoundState>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isFinished()) {
                iterator.remove();
            }
        }
    }

    private static String soundKey(final AbstractThrusterBlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        final ResourceKey<Level> dimension = level != null ? level.dimension() : Level.OVERWORLD;
        final BlockPos pos = blockEntity.getBlockPos();
        return dimension.location() + "|" + pos.asLong();
    }

    private enum Phase {
        STARTING,
        LOOPING,
        STOPPING
    }

    private static final class ThrusterSoundState {
        private final String key;
        private final AbstractThrusterBlockEntity blockEntity;
        private ThrusterSoundProfile profile;
        private Phase phase;
        private MovingThrusterSound sound;
        private int phaseTicks;
        private boolean active;

        private ThrusterSoundState(String key, AbstractThrusterBlockEntity blockEntity, ThrusterSoundProfile profile) {
            this.key = key;
            this.blockEntity = blockEntity;
            this.profile = profile;
        }

        private void activate() {
            active = true;
            beginStartup();
        }

        private void tickActive(ThrusterSoundProfile currentProfile) {
            if (!active || profile != currentProfile) {
                active = true;
                profile = currentProfile;
                beginStartup();
                return;
            }

            if (phase == Phase.STARTING) {
                phaseTicks++;
                if (phaseTicks >= TRANSITION_TICKS - CROSSFADE_TICKS) {
                    playLoop();
                }
            } else if (phase == Phase.LOOPING && (sound == null || sound.isStopped())) {
                playLoop();
            }
        }

        private void deactivate() {
            if (!active) {
                return;
            }

            active = false;
            fadeOutCurrentSound();
            final SoundEvent shutdown = profile.shutdownEvent();
            if (shutdown == null) {
                sound = null;
                phase = Phase.STOPPING;
                return;
            }

            phase = Phase.STOPPING;
            phaseTicks = 0;
            sound = new MovingThrusterSound(
                    shutdown, blockEntity, false, TRANSITION_TICKS + 1, CROSSFADE_TICKS, 0, this::soundCompleted);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        private void beginStartup() {
            fadeOutCurrentSound();
            phase = Phase.STARTING;
            phaseTicks = 0;
            sound = new MovingThrusterSound(
                    profile.startup().get(), blockEntity, false, TRANSITION_TICKS,
                    CROSSFADE_TICKS, CROSSFADE_TICKS, this::soundCompleted);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        private void playLoop() {
            // Leave the startup source alive for its short fade-out while the loop fades in.
            phase = Phase.LOOPING;
            phaseTicks = 0;
            sound = new MovingThrusterSound(
                    profile.loop().get(), blockEntity, true, -1, CROSSFADE_TICKS, 0, this::soundCompleted);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        private boolean isFor(AbstractThrusterBlockEntity candidate) {
            return blockEntity == candidate;
        }

        private boolean prepareForSoundEngineReload() {
            // SoundEngine.stopAll() drops its channels without stopping our instance object.
            sound = null;
            if (!active || !isBlockEntityUsable(blockEntity)) {
                active = false;
                return false;
            }
            phase = Phase.LOOPING;
            phaseTicks = 0;
            return true;
        }

        private boolean isFinished() {
            if (!isBlockEntityUsable(blockEntity)) {
                dispose();
                return true;
            }
            return !active && (sound == null || sound.isStopped());
        }

        private void dispose() {
            active = false;
            stopCurrentSound();
        }

        private void soundCompleted(MovingThrusterSound completedSound) {
            if (sound != completedSound) {
                return;
            }
            sound = null;
            if (!active || !isBlockEntityUsable(blockEntity)) {
                active = false;
                ACTIVE_SOUNDS.remove(key, this);
            }
        }

        private void stopCurrentSound() {
            if (sound != null) {
                sound.halt();
                sound = null;
            }
        }

        private void fadeOutCurrentSound() {
            if (sound != null) {
                sound.beginFadeOut(CROSSFADE_TICKS);
                sound = null;
            }
        }
    }

    private static final class MovingThrusterSound extends AbstractTickableSoundInstance {
        private final AbstractThrusterBlockEntity blockEntity;
        private final int lifetimeTicks;
        private final int fadeInTicks;
        private final int automaticFadeOutTicks;
        private final Consumer<MovingThrusterSound> completion;
        private int age;
        private int manualFadeOutTicks;
        private int manualFadeOutRemaining;

        private MovingThrusterSound(
                SoundEvent event,
                AbstractThrusterBlockEntity blockEntity,
                boolean looping,
                int lifetimeTicks,
                int fadeInTicks,
                int automaticFadeOutTicks,
                Consumer<MovingThrusterSound> completion) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.blockEntity = blockEntity;
            this.lifetimeTicks = lifetimeTicks;
            this.fadeInTicks = fadeInTicks;
            this.automaticFadeOutTicks = automaticFadeOutTicks;
            this.completion = completion;
            this.looping = looping;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            updateFromBlockEntity();
        }

        @Override
        public void tick() {
            if (!isBlockEntityUsable(blockEntity)) {
                stop();
                completion.accept(this);
                return;
            }
            age++;
            if (manualFadeOutRemaining > 0 && --manualFadeOutRemaining <= 0) {
                stop();
                completion.accept(this);
                return;
            }
            if (lifetimeTicks >= 0 && age >= lifetimeTicks) {
                stop();
                completion.accept(this);
                return;
            }
            updateFromBlockEntity();
        }

        @Override
        public boolean canStartSilent() {
            // Crossfades intentionally submit new sources at zero volume on their first frame.
            return true;
        }

        private void updateFromBlockEntity() {
            final BlockPos pos = blockEntity.getBlockPos();
            final float power = Math.max(blockEntity.getPower(), 5.0f / 15.0f);
            final Vec3 localCenter = Vec3.atCenterOf(pos);
            final Vec3 worldCenter = Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), localCenter);
            this.x = worldCenter.x;
            this.y = worldCenter.y;
            this.z = worldCenter.z;

            final Minecraft minecraft = Minecraft.getInstance();
            final float multiblockScale = multiblockSoundScale(blockEntity.width);
            final float baseVolume = Math.min(
                    1.0f,
                    (0.2f + (0.35f * power)) * VOLUME_BOOST_MULTIPLIER * multiblockScale
            );
            if (minecraft.player == null) {
                this.volume = 0.0f;
            } else {
                final double distance = Math.sqrt(minecraft.player.distanceToSqr(worldCenter));
                final float audibleRange = THRUSTER_SOUND_RANGE_BLOCKS * multiblockScale;
                final float proximityFactor = (float) Math.max(0.0d, 1.0d - (distance / audibleRange));
                this.volume = baseVolume * proximityFactor * envelopeVolume();
            }
            // The clips are authored as a matched set; pitch-shifting only the loop makes the seam audible.
            this.pitch = 1.0f;
        }

        private float envelopeVolume() {
            float envelope = fadeInTicks <= 0 ? 1.0f : Math.min(1.0f, (float) age / fadeInTicks);
            if (automaticFadeOutTicks > 0 && lifetimeTicks >= 0) {
                envelope = Math.min(envelope,
                        Math.max(0.0f, (float) (lifetimeTicks - age) / automaticFadeOutTicks));
            }
            if (manualFadeOutRemaining > 0 && manualFadeOutTicks > 0) {
                envelope = Math.min(envelope, (float) manualFadeOutRemaining / manualFadeOutTicks);
            }
            return envelope;
        }

        private void beginFadeOut(int ticks) {
            manualFadeOutTicks = Math.max(1, ticks);
            manualFadeOutRemaining = manualFadeOutTicks;
        }

        private void halt() {
            super.stop();
        }

        private static float multiblockSoundScale(int width) {
            return switch (Math.max(1, width)) {
                case 1 -> 1.0f;
                case 2 -> 1.25f;
                default -> 1.5f;
            };
        }
    }

    private static boolean isBlockEntityUsable(@Nullable AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved() || blockEntity.getLevel() == null) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null
                && minecraft.level.dimension().equals(blockEntity.getLevel().dimension());
    }
}
