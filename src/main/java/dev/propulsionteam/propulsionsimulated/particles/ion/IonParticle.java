package dev.propulsionteam.propulsionsimulated.particles.ion;

import java.util.List;

import javax.annotation.Nonnull;

import dev.propulsionteam.propulsionsimulated.particles.ThrusterParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class IonParticle extends ThrusterParticle {

    protected float getBaseQuadSize() { return 0.95f; }
    protected float getEndQuadSize() { return 0.2f; }
    protected float getSpeedMultiplier() { return 0.144f; }
    protected float getParticleFriction() { return 0.995f; }
    protected int getBaseLifetime() { return 20; }
    protected double getFluctuation() { return 0.0025d; }

    private static final int SPRITE_COUNT = 9;
    private final SpriteSet spriteSet;
    private final float startSize;
    private final float endSize;
    private final List<ResourceLocation> overrideTextures;
    private TextureAtlasSprite[] cachedOverrideSprites;
    private final float startupProgress;
    private final float trailCoverage;
    protected IonParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz,
                            SpriteSet spriteSet, IonParticleData data) {
        super(level, x, y, z, spriteSet);
        this.spriteSet = spriteSet;
        this.overrideTextures = data.overrideTextures();
        this.startupProgress = data.startupProgress() == null ? 1.0f : Mth.clamp(data.startupProgress(), 0.0f, 1.0f);
        this.inheritedVelocity = data.inheritedVelocity();
        this.trailCoverage = data.trailCoverage();
        this.hasPhysics = true;
        this.friction = getParticleFriction();
        this.lifetime = Math.round(Mth.lerp(this.startupProgress, 10.0f, getBaseLifetime()));

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.quadSize = (data.overrideSize() != null ? data.overrideSize() : getBaseQuadSize())
                * Mth.lerp(this.startupProgress, 0.38f, 1.0f);
        float scale = data.overrideSize() != null ? (data.overrideSize() / getBaseQuadSize()) : 1.0f;
        this.endSize = getEndQuadSize() * scale;
        this.startSize = this.quadSize;

        if (!this.overrideTextures.isEmpty()) {
            try {
                var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_PARTICLES);
                this.cachedOverrideSprites = new TextureAtlasSprite[this.overrideTextures.size()];
                for (int i = 0; i < this.overrideTextures.size(); i++) {
                    this.cachedOverrideSprites[i] = atlas.apply(this.overrideTextures.get(i));
                }
            } catch (Exception ignored) {
                this.cachedOverrideSprites = null;
            }
        }

        if (data.overrideColor() == null) {
            setColor(0xFFFFFF);
        } else {
            int rgb = data.overrideColor() & 0xFFFFFF;
            this.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
        this.setAlpha(1.0f - this.trailCoverage);
        this.pickSpriteAndSize();
    }

    @Override
    public void tick() {
        updateScale();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd * getSpeedMultiplier(), this.yd * getSpeedMultiplier(), this.zd * getSpeedMultiplier());

        double controlledFluctuation = getFluctuation() * Mth.lerp(this.startupProgress, 0.12f, 1.0f);
        this.xd += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.yd += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.zd += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        this.pickSpriteAndSize();
    }

    private void pickSpriteAndSize() {
        final float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0f, 1.0f);
        if (startupProgress < 1.0f) {
            float pulse = 0.82f + 0.18f * Mth.sin((this.age + startupProgress * 10.0f) * 1.7f);
            this.setAlpha(Mth.clamp(pulse, 0.55f, 1.0f) * presentationReveal());
        } else {
            this.setAlpha(presentationReveal());
        }
        final int frameIndex = Mth.clamp((int) (progress * SPRITE_COUNT), 0, SPRITE_COUNT - 1);

        if (this.cachedOverrideSprites != null) {
            this.setSprite(this.cachedOverrideSprites[frameIndex % this.cachedOverrideSprites.length]);
        } else {
            this.setSprite(this.spriteSet.get(frameIndex, SPRITE_COUNT));
        }

        this.quadSize = Mth.lerp(progress, this.startSize, this.endSize) * particleSize();
    }

    private float presentationReveal() {
        float reveal = Mth.clamp(this.age / 2.0f, 0.0f, 1.0f);
        return Mth.lerp(reveal, 1.0f - this.trailCoverage, 1.0f);
    }

    @Nonnull
    @Override
    public ParticleRenderType getRenderType(){
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Factory implements ParticleProvider<IonParticleData>{
        private final SpriteSet spriteSet;
        public Factory(SpriteSet ionSpriteSet) {
            this.spriteSet = ionSpriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull IonParticleData data, @Nonnull ClientLevel level,
        double x, double y, double z, double dx, double dy, double dz){
            return new IonParticle(level, x, y, z, dx, dy, dz, this.spriteSet, data);
        }
    }
}
