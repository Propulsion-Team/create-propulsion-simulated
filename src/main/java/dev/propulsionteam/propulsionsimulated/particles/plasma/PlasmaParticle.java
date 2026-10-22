package dev.propulsionteam.propulsionsimulated.particles.plasma;

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

public class PlasmaParticle extends ThrusterParticle {

    //Config
    protected float getPlasmaSpread() { return 0.05f; }
    protected float getPlasmaBaseQuadSize() { return 2.0f; }
    protected float getPlasmaFriction() { return 0.99f; }
    protected float getPlasmaSpeedMultiplier() { return 0.144f; }
    protected int getPlasmaBaseLifetime() { return 40; }

    //Visuals
    private final SpriteSet spriteSet;
    private static final int PLASMA_SPRITE_COUNT = 9;

    private static final float PLASMA_SHRINK_START = 0.6f;
    private static final float PLASMA_END_SCALE_MULTIPLIER = 3.0f;

    private float currentSpeedMultiplier;
    private float baseSize;
    private final List<ResourceLocation> overrideTextures;
    private TextureAtlasSprite[] cachedOverrideSprites;
    private final float startupProgress;
    private final boolean startupHalo;
    private final float trailCoverage;

    protected PlasmaParticle(ClientLevel level, double x, double y, double z,
                            double dxSource, double dySource, double dzSource,
                            SpriteSet spriteSet, PlasmaParticleData data) {
        super(level, x, y, z, spriteSet);
        this.spriteSet = spriteSet;
        this.overrideTextures = data.overrideTextures();
        this.startupProgress = data.startupProgress() == null ? 1.0f : Mth.clamp(data.startupProgress(), 0.0f, 1.0f);
        this.inheritedVelocity = data.inheritedVelocity();
        this.trailCoverage = data.trailCoverage();
        this.startupHalo = this.startupProgress < 1.0f
                && this.random.nextFloat() < (1.0f - this.startupProgress) * 0.55f;

        //Initialize plasma state
        float startupSize = this.startupHalo
                ? Mth.lerp(this.startupProgress, 1.65f, 1.0f)
                : Mth.lerp(this.startupProgress, 0.62f, 1.0f);
        this.quadSize *= getPlasmaBaseQuadSize()
                * (data.overrideSize() == null ? 1.0f : data.overrideSize())
                * startupSize;
        this.baseSize = this.quadSize;
        this.lifetime = Math.round(Mth.lerp(this.startupProgress,
                this.startupHalo ? 24.0f : 32.0f, getPlasmaBaseLifetime()));
        this.friction = getPlasmaFriction();
        float startupSpread = this.startupHalo
                ? Mth.lerp(this.startupProgress, 4.0f, 1.0f)
                : Mth.lerp(this.startupProgress, 0.35f, 1.0f);
        this.xd = dxSource + getRandomSpread() * startupSpread;
        this.yd = dySource + getRandomSpread() * startupSpread;
        this.zd = dzSource + getRandomSpread() * startupSpread;
        this.hasPhysics = true;
        this.currentSpeedMultiplier = getPlasmaSpeedMultiplier() * (this.startupHalo
                ? Mth.lerp(this.startupProgress, 1.35f, 1.0f)
                : Mth.lerp(this.startupProgress, 0.82f, 1.0f));

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

        setSpriteFromAge(this.spriteSet);
        if (data.overrideColor() == null) {
            setColor(0xFFFFFF);
        } else {
            int rgb = data.overrideColor() & 0xFFFFFF;
            this.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
        setAlpha(1.0f - this.trailCoverage);
    }

    @Override
    public void tick() {
        updateScale();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        //Die young
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd * this.currentSpeedMultiplier, this.yd * this.currentSpeedMultiplier,
                this.zd * this.currentSpeedMultiplier);

        //Visual update
        float percent = (float)this.age / (float)this.lifetime;

        if (percent < PLASMA_SHRINK_START) {
            this.quadSize = (this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f) * particleSize();
        } else {
            float sizeAtTransition = this.baseSize + (float)Math.pow(PLASMA_SHRINK_START, 0.8f) * 2.0f;
            float sizeAtEnd = this.baseSize * PLASMA_END_SCALE_MULTIPLIER;
            float shrinkProgress = (percent - PLASMA_SHRINK_START) / (1.0f - PLASMA_SHRINK_START);
            this.quadSize = Mth.lerp(shrinkProgress, sizeAtTransition, sizeAtEnd) * particleSize();
        }

        //Friction
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        this.pickSprite();
        float reveal = Mth.clamp(this.age / 2.0f, 0.0f, 1.0f);
        this.setAlpha(Mth.lerp(reveal, 1.0f - this.trailCoverage, 1.0f));
    }

    private void pickSprite() {
        int frameIndex = (int) (((float)this.age / (float)this.lifetime) * PLASMA_SPRITE_COUNT);
        frameIndex = Mth.clamp(frameIndex, 0, PLASMA_SPRITE_COUNT - 1);
        if (this.cachedOverrideSprites != null) {
            this.setSprite(this.cachedOverrideSprites[frameIndex % this.cachedOverrideSprites.length]);
            return;
        }
        this.setSprite(this.spriteSet.get(frameIndex, PLASMA_SPRITE_COUNT));
    }

    float getRandomSpread(){
        return (random.nextFloat() * 2.0f - 1.0f) * getPlasmaSpread();
    }

    @Nonnull
    public ParticleRenderType getRenderType(){
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Factory implements ParticleProvider<PlasmaParticleData>{
        private final SpriteSet spriteSet;
        public Factory(SpriteSet plasmaSpriteSet) {
            this.spriteSet = plasmaSpriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull PlasmaParticleData data, @Nonnull ClientLevel level,
        double x, double y, double z, double dx, double dy, double dz){
            return new PlasmaParticle(level, x, y, z, dx, dy, dz, this.spriteSet, data);
        }
    }
}
