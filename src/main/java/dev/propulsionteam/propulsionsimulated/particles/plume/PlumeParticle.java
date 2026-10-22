package dev.propulsionteam.propulsionsimulated.particles.plume;

import java.util.List;

import javax.annotation.Nonnull;

import dev.propulsionteam.propulsionsimulated.particles.ThrusterParticle;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
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
import net.minecraft.world.phys.Vec3;

public class PlumeParticle extends ThrusterParticle {
    //Plume
    protected float getPlumeSpread() { return 0.05f; }
    protected float getPlumeBaseQuadSize() { return 2.0f; }
    protected float getPlumeFriction() { return 0.99f; }
    protected float getPlumeSpeedMultiplier() { return 0.144f; }
    protected int getPlumeBaseLifetime() { return 40; }
    //Smoke
    protected float getSmokeSpreadMagnitude() { return 0.15f; }
    protected float getSmokeFriction() { return 0.96f; }
    protected float getSmokeBaseLift() { return 0.02f; }

    private enum ParticleState {
        PLUME, SMOKE
    }

    //State
    private final SpriteSet spriteSet;
    private static final int PLUME_SPRITE_COUNT = 6;
    private static final int SMOKE_SPRITE_COUNT = 7; //Yes, should be 10 but this looks better imo
    private static final int TOTAL_SPRITE_COUNT = PLUME_SPRITE_COUNT + SMOKE_SPRITE_COUNT;
    private static final int BASE_SMOKE_TRANSITION_AGE = 20;

    private ParticleState currentState;
    private float currentSpeedMultiplier;
    private float currentFriction;

    private final int smokeTransitionAge;
    private final float smokeLift;
    private final Vec3 spreadDirection;
    private final float spreadMagnitude;
    private final List<ResourceLocation> overrideTextures;
    private TextureAtlasSprite[] cachedOverrideSprites;
    private final float startupProgress;
    private final float trailCoverage;
    private Vec3 sableResolvedSpreadDirection;

    float baseSize;


    protected PlumeParticle(ClientLevel level, double x, double y, double z,
                            double dxSource, double dySource, double dzSource,
                            SpriteSet spriteSet, PlumeParticleData data) {
        super(level, x, y, z, spriteSet);
        this.spriteSet = spriteSet;
        this.overrideTextures = data.overrideTextures();
        this.startupProgress = data.startupProgress() == null ? 1.0f : Mth.clamp(data.startupProgress(), 0.0f, 1.0f);
        this.inheritedVelocity = data.inheritedVelocity();
        this.trailCoverage = data.trailCoverage();
        //Initialize plume state
        this.quadSize *= getPlumeBaseQuadSize()
                * (data.overrideSize() == null ? 1.0f : data.overrideSize())
                * Mth.lerp(this.startupProgress, 1.7f, 1.0f);
        this.baseSize = this.quadSize;
        this.lifetime = Math.round(Mth.lerp(this.startupProgress, 28.0f, getPlumeBaseLifetime())) + random.nextInt(5);
        this.friction = getPlumeFriction();
        float ignitionSpread = Mth.lerp(this.startupProgress, 4.0f, 1.0f);
        this.xd = dxSource + getRandomSpread() * ignitionSpread;
        this.yd = dySource + getRandomSpread() * ignitionSpread;
        this.zd = dzSource + getRandomSpread() * ignitionSpread;
        this.hasPhysics = true;
        this.currentSpeedMultiplier = getPlumeSpeedMultiplier() * Mth.lerp(this.startupProgress, 1.45f, 1.0f);
        this.currentFriction = getPlumeFriction();
        this.currentState = ParticleState.PLUME;
        //Calculate spread direction
        Vec3 initialVel = new Vec3(this.xd, this.yd, this.zd).normalize();
        Vec3 nonParallel = new Vec3(1, 0, 0);
        if (Math.abs(initialVel.dot(nonParallel)) > 0.99) {
            nonParallel = new Vec3(0, 1, 0);
        }
        Vec3 u = initialVel.cross(nonParallel).normalize();
        Vec3 v = initialVel.cross(u).normalize();
        double randomAngle = this.random.nextDouble() * 2.0 * Math.PI;
        this.spreadDirection = u.scale(Math.cos(randomAngle)).add(v.scale(Math.sin(randomAngle)));
        this.spreadMagnitude = (0.1f + this.random.nextFloat() * (0.8f - 0.1f))
                * Mth.lerp(this.startupProgress, 2.0f, 1.0f); // ignition begins wide, then settles
        this.smokeTransitionAge = BASE_SMOKE_TRANSITION_AGE + this.random.nextIntBetweenInclusive(-2, 2);
        this.smokeLift = getSmokeBaseLift() + -0.01f + this.random.nextFloat() * (0.03f - -0.01f); //-0.1 - 0.03

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
        this.yo /* gurt */ = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        var trackingSubLevel = ((ParticleExtension) this).sable$getTrackingSubLevel();
        if (trackingSubLevel != null) {
            this.sableResolvedSpreadDirection =
                    trackingSubLevel.logicalPose().transformNormal(this.spreadDirection).normalize();
        }
        this.move(this.xd * this.currentSpeedMultiplier, this.yd * this.currentSpeedMultiplier,
                this.zd * this.currentSpeedMultiplier);
        //Smoke update
        if (currentState == ParticleState.PLUME && this.age >= this.smokeTransitionAge) {
            currentState = ParticleState.SMOKE;
            this.baseSize *= 1.2f;
            this.friction = getSmokeFriction();
        }
        if (currentState == ParticleState.SMOKE) {
            this.yd += this.smokeLift;
        }
        //Visual update
        float percent = (float)this.age / (float)this.lifetime;
        if (currentState == ParticleState.PLUME) {
            this.quadSize = (this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f) * particleSize();
        } else {
            this.quadSize = (this.baseSize - percent * 2.0f + 2.5f) * particleSize();
        }
        //Additional smoke spread (that starts a bit before particle becomes smoke)
        int presmokeAge = this.smokeTransitionAge - 5;
        if (this.age >= presmokeAge && !this.hasCollided) {
            float smoke_percent = (this.age - presmokeAge) / (this.lifetime - presmokeAge);
            float aged_spread_magnitude = (0.8f - smoke_percent) * this.spreadMagnitude; //It slows down at last frames actually
            Vec3 resolvedSpread = this.sableResolvedSpreadDirection == null
                    ? this.spreadDirection
                    : this.sableResolvedSpreadDirection;
            this.xd += resolvedSpread.x * getSmokeSpreadMagnitude() * aged_spread_magnitude;
            this.yd += resolvedSpread.y * getSmokeSpreadMagnitude() * aged_spread_magnitude;
            this.zd += resolvedSpread.z * getSmokeSpreadMagnitude() * aged_spread_magnitude;
        }

        //Friction
        this.xd *= this.currentFriction;
        this.yd *= this.currentFriction;
        this.zd *= this.currentFriction;

        this.pickSprite();
        float reveal = Mth.clamp(this.age / 2.0f, 0.0f, 1.0f);
        this.setAlpha(Mth.lerp(reveal, 1.0f - this.trailCoverage, 1.0f));
    }

    //Helpers

    private void pickSprite() {
        int frameIndex;
        if (currentState == ParticleState.PLUME) {
            float plumeProgress = (float) this.age / (float) this.smokeTransitionAge;
            int plumeFrame = (int) (plumeProgress * PLUME_SPRITE_COUNT);
            frameIndex = Mth.clamp(plumeFrame, 0, PLUME_SPRITE_COUNT - 1);
        } else {
            int ageInSmokePhase = this.age - this.smokeTransitionAge;
            int smokePhaseDuration = this.lifetime - this.smokeTransitionAge;

            int smokeFrame;
            if (smokePhaseDuration <= 0) {
                smokeFrame = 0;
            } else {
                smokeFrame = (ageInSmokePhase * SMOKE_SPRITE_COUNT) / smokePhaseDuration;
            }

            smokeFrame = Mth.clamp(smokeFrame, 0, SMOKE_SPRITE_COUNT - 1);
            frameIndex = PLUME_SPRITE_COUNT + smokeFrame;
        }

        if (this.cachedOverrideSprites != null) {
            this.setSprite(this.cachedOverrideSprites[frameIndex % this.cachedOverrideSprites.length]);
            return;
        }
        this.setSprite(this.spriteSet.get(frameIndex, TOTAL_SPRITE_COUNT));
    }

    float getRandomSpread(){
        return (random.nextFloat() * 2.0f - 1.0f) * getPlumeSpread();
    }

    @Nonnull
    public ParticleRenderType getRenderType(){
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    //Factory
    public static class Factory implements ParticleProvider<PlumeParticleData>{
        private final SpriteSet spriteSet;
        public Factory(SpriteSet plumeSpriteSet) {
            this.spriteSet = plumeSpriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull PlumeParticleData data, @Nonnull ClientLevel level,
        double x, double y, double z, double dx, double dy, double dz){
            return new PlumeParticle(level, x, y, z, dx, dy, dz, this.spriteSet, data);
        }
    }
}
