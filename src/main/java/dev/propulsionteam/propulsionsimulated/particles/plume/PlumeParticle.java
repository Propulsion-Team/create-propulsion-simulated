package dev.propulsionteam.propulsionsimulated.particles.plume;

import java.util.List;

import javax.annotation.Nonnull;

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

public class PlumeParticle extends SimpleAnimatedParticle {
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

    //Physics
    private static final float COLLISION_SPEED_RETENTION = 0.9f;
    private static final double COLLISION_DETECTION_EPSILON = 0.001;
    private static final float COLLISION_PERPENDICULAR_DAMPEN = 0.1f;
    private static final Vec3 AXIS_X = new Vec3(1, 0, 0);
    private static final Vec3 AXIS_Y = new Vec3(0, 1, 0);
    private static final Vec3 AXIS_Z = new Vec3(0, 0, 1);

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
    private boolean hasCollided = false;
    private final List<ResourceLocation> overrideTextures;
    private TextureAtlasSprite[] cachedOverrideSprites;
    private final float startupProgress;
    /** World-space nozzle motion, kept separate so plume drag cannot make the craft overtake its exhaust. */
    private final Vec3 inheritedVelocity;
    private final float trailCoverage;

    double dx; double dy; double dz;
    float baseSize;


    protected PlumeParticle(ClientLevel level, double x, double y, double z,
                            double dxSource, double dySource, double dzSource,
                            SpriteSet spriteSet, PlumeParticleData data) {
        super(level, x, y, z, spriteSet, 0);
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
        this.dx = dxSource + getRandomSpread() * ignitionSpread;
        this.dy = dySource + getRandomSpread() * ignitionSpread;
        this.dz = dzSource + getRandomSpread() * ignitionSpread;
        this.hasPhysics = true;
        this.currentSpeedMultiplier = getPlumeSpeedMultiplier() * Mth.lerp(this.startupProgress, 1.45f, 1.0f);
        this.currentFriction = getPlumeFriction();
        this.currentState = ParticleState.PLUME;
        //Calculate spread direction
        Vec3 initialVel = new Vec3(this.dx, this.dy, this.dz).normalize();
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
        this.xo = this.x;
        this.yo /* gurt */ = this.y;
        this.zo = this.z;
        final double COLLISION_IGNORE_DOT_THRESHOLD = -1.0E-5D;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        //Velocity before possible collision
        double intendedMoveX = this.dx * this.currentSpeedMultiplier + this.inheritedVelocity.x;
        double intendedMoveY = this.dy * this.currentSpeedMultiplier + this.inheritedVelocity.y;
        double intendedMoveZ = this.dz * this.currentSpeedMultiplier + this.inheritedVelocity.z;

        double prevX = this.x;
        double prevY = this.y;
        double prevZ = this.z;

        //Actual movement
        Vec3 totalMove = new Vec3(intendedMoveX, intendedMoveY, intendedMoveZ);
        double totalDist = totalMove.length();
        if (totalDist > 0.0D) {
            int steps = (int)Math.ceil(totalDist / 0.5D);
            Vec3 stepMove = totalMove.scale(1.0D / steps);
            for (int i = 0; i < steps; ++i) {
                double stepPrevX = this.x;
                double stepPrevY = this.y;
                double stepPrevZ = this.z;
                this.move(stepMove.x, stepMove.y, stepMove.z);
                double movedX = this.x - stepPrevX;
                double movedY = this.y - stepPrevY;
                double movedZ = this.z - stepPrevZ;
                final double check = 1.0E-6D;
                if (Math.abs(movedX - stepMove.x) > check || Math.abs(movedY - stepMove.y) > check || Math.abs(movedZ - stepMove.z) > check) {
                    break;
                }
            }
        }
        double actualMoveX = this.x - prevX;
        double actualMoveY = this.y - prevY;
        double actualMoveZ = this.z - prevZ;

        //Determine collision and its normal
        boolean collisionDetected = false;
        Vec3 collisionNormal = null;
        if (this.onGround) {
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
            Vec3 incomingVel = new Vec3(this.dx, this.dy, this.dz);
            if (incomingVel.normalize().dot(collisionNormal) > COLLISION_IGNORE_DOT_THRESHOLD) {
                //Nothing ever happens, we collide backwards here, which should not be resolved
            } else {
                this.hasCollided = true;
                double incomingSpeedSq = incomingVel.lengthSqr();
                if (incomingSpeedSq > 1e-7) {
                    Vec3 incomingVelNormalized = incomingVel.normalize();
                    double dot = incomingVelNormalized.dot(collisionNormal);
                    //0 - perpendicular, PI/2 - parallel
                    double angleOfIncidence = Math.acos(org.joml.Math.clamp(Math.abs(dot), 0.0, 1.0));
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
                    Vec3 slideComponent = V_tangential_comp.scale(slideBlendFactor); //For sliding use original tangential component

                    Vec3 desiredTangentialVel = slideComponent.add(spreadComponent);

                    //Combine and apply new velocity
                    Vec3 newVel = desiredNormalVel.add(desiredTangentialVel);
                    double newVelMagnitude = newVel.length();
                    if (newVelMagnitude > 1e-5) {
                        this.dx = (newVel.x / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.dy = (newVel.y / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.dz = (newVel.z / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                    } else { //Fallback
                        this.dx = spreadPlaneDirection.x * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.dy = spreadPlaneDirection.y * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.dz = spreadPlaneDirection.z * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                    }

                } else { //Incoming speed too low, slow down
                    this.dx *= 0.1; this.dy *= 0.1; this.dz *= 0.1;
                }
            }
        }
        //Smoke update
        if (currentState == ParticleState.PLUME && this.age >= this.smokeTransitionAge) {
            currentState = ParticleState.SMOKE;
            this.baseSize *= 1.2f;
            this.friction = getSmokeFriction();
        }
        if (currentState == ParticleState.SMOKE) {
            this.dy += this.smokeLift;
        }
        //Visual update
        float percent = (float)this.age / (float)this.lifetime;
        if (currentState == ParticleState.PLUME) {
            this.quadSize = this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f;
        } else {
            this.quadSize = this.baseSize - (float)percent * 2.0f + 2.5f;
        }
        //Additional smoke spread (that starts a bit before particle becomes smoke)
        int presmokeAge = this.smokeTransitionAge - 5;
        if (this.age >= presmokeAge && !this.hasCollided) {
            float smoke_percent = (this.age - presmokeAge) / (this.lifetime - presmokeAge);
            float aged_spread_magnitude = (0.8f - smoke_percent) * this.spreadMagnitude; //It slows down at last frames actually
            this.dx += this.spreadDirection.x * getSmokeSpreadMagnitude() * aged_spread_magnitude;
            this.dy += this.spreadDirection.y * getSmokeSpreadMagnitude() * aged_spread_magnitude;
            this.dz += this.spreadDirection.z * getSmokeSpreadMagnitude() * aged_spread_magnitude;
        }

        //Friction
        this.dx *= this.currentFriction;
        this.dy *= this.currentFriction;
        this.dz *= this.currentFriction;

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
