package dev.propulsionteam.propulsionsimulated.particles.plasma;

import java.util.List;

import javax.annotation.Nonnull;

import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PlasmaParticle extends SimpleAnimatedParticle implements ParticleSubLevelKickable {

    //Config
    protected float getPlasmaSpread() { return 0.05f; }
    protected float getPlasmaBaseQuadSize() { return 2.0f; }
    protected float getPlasmaFriction() { return 0.99f; }
    protected float getPlasmaSpeedMultiplier() { return 0.144f; }
    protected int getPlasmaBaseLifetime() { return 40; }

    //Physics
    private static final float COLLISION_SPEED_RETENTION = 0.9f;
    private static final double COLLISION_DETECTION_EPSILON = 0.001;
    private static final float COLLISION_PERPENDICULAR_DAMPEN = 0.1f;
    private static final Vec3 AXIS_X = new Vec3(1, 0, 0);
    private static final Vec3 AXIS_Y = new Vec3(0, 1, 0);
    private static final Vec3 AXIS_Z = new Vec3(0, 0, 1);

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
    /** World-space nozzle motion, kept separate so plasma drag cannot make the craft overtake its exhaust. */
    private final Vec3 inheritedVelocity;
    private final float trailCoverage;
    private boolean sableManagedMotion;

    protected PlasmaParticle(ClientLevel level, double x, double y, double z,
                            double dxSource, double dySource, double dzSource,
                            SpriteSet spriteSet, PlasmaParticleData data) {
        super(level, x, y, z, spriteSet, 0);
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

        //Calculate spread direction (perpendicular to velocity)
        Vec3 initialVel = new Vec3(this.xd, this.yd, this.zd).normalize();
        Vec3 nonParallel = new Vec3(1, 0, 0);
        if (Math.abs(initialVel.dot(nonParallel)) > 0.99) {
            nonParallel = new Vec3(0, 1, 0);
        }

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
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        final double COLLISION_IGNORE_DOT_THRESHOLD = -1.0E-5D;

        //Die young
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        //Velocity before possible collision
        double intendedMoveX = this.xd * this.currentSpeedMultiplier + this.inheritedVelocity.x;
        double intendedMoveY = this.yd * this.currentSpeedMultiplier + this.inheritedVelocity.y;
        double intendedMoveZ = this.zd * this.currentSpeedMultiplier + this.inheritedVelocity.z;

        double prevX = this.x;
        double prevY = this.y;
        double prevZ = this.z;

        if (((ParticleExtension) this).sable$getTrackingSubLevel() != null) {
            this.sableManagedMotion = true;
        }

        this.move(intendedMoveX, intendedMoveY, intendedMoveZ);
        double actualMoveX = this.x - prevX;
        double actualMoveY = this.y - prevY;
        double actualMoveZ = this.z - prevZ;

        //Determine collision and its normal
        boolean collisionDetected = false;
        Vec3 collisionNormal = null;
        if (this.sableManagedMotion) {
            // Sable already resolved pose movement and world/sub-level collisions.
        } else if (this.onGround) {
            collisionDetected = true;
            collisionNormal = new Vec3(0, 1, 0);
        } else {
            final float COLLISION_DETECTION_FACTOR = 0.95f;
            boolean blockedX = Math.abs(intendedMoveX) > COLLISION_DETECTION_EPSILON && Math.abs(actualMoveX) < Math.abs(intendedMoveX) * COLLISION_DETECTION_FACTOR;
            boolean blockedZ = Math.abs(intendedMoveZ) > COLLISION_DETECTION_EPSILON && Math.abs(actualMoveZ) < Math.abs(intendedMoveZ) * COLLISION_DETECTION_FACTOR;
            boolean blockedYCeiling = Math.abs(intendedMoveY) > COLLISION_DETECTION_EPSILON && intendedMoveY > 0 && Math.abs(actualMoveY) < Math.abs(intendedMoveY) * COLLISION_DETECTION_FACTOR;
            if (blockedYCeiling) {
                collisionDetected = true;
                collisionNormal = new Vec3(0, -1, 0);
            } else if (blockedX) {
                collisionDetected = true;
                collisionNormal = new Vec3(intendedMoveX < 0 ? 1 : -1, 0, 0);
            } else if (blockedZ) {
                collisionDetected = true;
                collisionNormal = new Vec3(0, 0, intendedMoveZ < 0 ? 1 : -1);
            }
        }

        //We actually collided with something, lets resolve velocity!
        if (collisionDetected && collisionNormal != null) {
            Vec3 incomingVel = new Vec3(this.xd, this.yd, this.zd);
            if (incomingVel.normalize().dot(collisionNormal) > COLLISION_IGNORE_DOT_THRESHOLD) {
                //Nothing ever happens, we collide backwards here, which should not be resolved
            } else {
                double incomingSpeedSq = incomingVel.lengthSqr();
                if (incomingSpeedSq > 1e-7) {
                    Vec3 incomingVelNormalized = incomingVel.normalize();
                    double dot = incomingVelNormalized.dot(collisionNormal);

                    //0 - perpendicular, PI/2 - parallel
                    double angleOfIncidence = Math.acos(Mth.clamp(Math.abs(dot), 0.0, 1.0));
                    float spreadBlendFactor = (float)Math.cos(angleOfIncidence);
                    float slideBlendFactor = (float)Math.sin(angleOfIncidence);

                    //Velocity decomposition
                    Vec3 V_normal_comp = collisionNormal.scale(incomingVel.dot(collisionNormal));
                    Vec3 V_tangential_comp = incomingVel.subtract(V_normal_comp);

                    //Reflect + dampen
                    Vec3 desiredNormalVel;
                    if (incomingVel.dot(collisionNormal) < 0) { //Moving into the surface
                        desiredNormalVel = V_normal_comp.scale(-COLLISION_PERPENDICULAR_DAMPEN);
                    } else {
                        desiredNormalVel = V_normal_comp;
                    }

                    //Calculate spread velocity
                    Vec3 spreadPlaneDirection;
                    double randomAngle = this.random.nextDouble() * Math.PI * 2.0D;

                    //Determine two axes perpendicular to normal
                    Vec3 axis1, axis2;
                    if (Math.abs(collisionNormal.y) > 0.9) { //Ground/Ceiling
                        axis1 = AXIS_X;
                        axis2 = collisionNormal.cross(axis1).normalize();
                    } else { //Wall
                        axis1 = AXIS_Y;
                        axis2 = collisionNormal.cross(axis1).normalize();
                    }
                    if (axis2.lengthSqr() < 0.1) { //Fallback
                        axis1 = Math.abs(collisionNormal.x) > 0.9 ? AXIS_Z : AXIS_X;
                        axis2 = collisionNormal.cross(axis1).normalize();
                    }

                    spreadPlaneDirection = axis1.scale(Math.cos(randomAngle)).add(axis2.scale(Math.sin(randomAngle))).normalize();

                    Vec3 spreadComponent = spreadPlaneDirection.scale(incomingVel.length() * spreadBlendFactor);
                    Vec3 slideComponent = V_tangential_comp.scale(slideBlendFactor);

                    Vec3 desiredTangentialVel = slideComponent.add(spreadComponent);

                    //Combine and apply new velocity
                    Vec3 newVel = desiredNormalVel.add(desiredTangentialVel);
                    double newVelMagnitude = newVel.length();
                    if (newVelMagnitude > 1e-5) {
                        this.xd = (newVel.x / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.yd = (newVel.y / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.zd = (newVel.z / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                    } else { //Fallback
                        this.xd = spreadPlaneDirection.x * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.yd = spreadPlaneDirection.y * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.zd = spreadPlaneDirection.z * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                    }

                } else { //Incoming speed too low, slow down
                    this.xd *= 0.1; this.yd *= 0.1; this.zd *= 0.1;
                }
            }
        }

        //Visual update
        float percent = (float)this.age / (float)this.lifetime;

        if (percent < PLASMA_SHRINK_START) {
            this.quadSize = this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f;
        } else {
            float sizeAtTransition = this.baseSize + (float)Math.pow(PLASMA_SHRINK_START, 0.8f) * 2.0f;
            float sizeAtEnd = this.baseSize * PLASMA_END_SCALE_MULTIPLIER;
            float shrinkProgress = (percent - PLASMA_SHRINK_START) / (1.0f - PLASMA_SHRINK_START);
            this.quadSize = Mth.lerp(shrinkProgress, sizeAtTransition, sizeAtEnd);
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

    @Override
    public boolean sable$shouldKickFromTracking() {
        return true;
    }

    @Override
    public boolean sable$shouldCollideWithTrackingSubLevel() {
        return false;
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
