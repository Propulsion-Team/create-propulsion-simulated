package dev.propulsionteam.propulsionsimulated.particles.ion;

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

public class IonParticle extends SimpleAnimatedParticle {

    protected float getBaseQuadSize() { return 0.95f; }
    protected float getEndQuadSize() { return 0.2f; }
    protected float getSpeedMultiplier() { return 0.144f; }
    protected float getParticleFriction() { return 0.995f; }
    protected int getBaseLifetime() { return 20; }
    protected double getFluctuation() { return 0.0025d; }

    //Physics
    private static final float COLLISION_SPEED_RETENTION = 0.9f;
    private static final double COLLISION_DETECTION_EPSILON = 0.001;
    private static final float COLLISION_PERPENDICULAR_DAMPEN = 0.1f;
    private static final Vec3 AXIS_X = new Vec3(1, 0, 0);
    private static final Vec3 AXIS_Y = new Vec3(0, 1, 0);
    private static final Vec3 AXIS_Z = new Vec3(0, 0, 1);

    private static final int SPRITE_COUNT = 9;
    private final SpriteSet spriteSet;
    private final float startSize;
    private final float endSize;
    private final List<ResourceLocation> overrideTextures;
    private TextureAtlasSprite[] cachedOverrideSprites;
    private final float startupProgress;
    /** World-space nozzle motion, kept separate so ion drag cannot make the craft overtake its exhaust. */
    private final Vec3 inheritedVelocity;
    private final float trailCoverage;
    double dx;
    double dy;
    double dz;

    protected IonParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz,
                            SpriteSet spriteSet, IonParticleData data) {
        super(level, x, y, z, spriteSet, 0);
        this.spriteSet = spriteSet;
        this.overrideTextures = data.overrideTextures();
        this.startupProgress = data.startupProgress() == null ? 1.0f : Mth.clamp(data.startupProgress(), 0.0f, 1.0f);
        this.inheritedVelocity = data.inheritedVelocity();
        this.trailCoverage = data.trailCoverage();
        this.hasPhysics = true;
        this.friction = getParticleFriction();
        this.lifetime = Math.round(Mth.lerp(this.startupProgress, 10.0f, getBaseLifetime()));

        this.dx = dx;
        this.dy = dy;
        this.dz = dz;

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
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        final double COLLISION_IGNORE_DOT_THRESHOLD = -1.0E-5D;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        //Velocity before possible collision
        double intendedMoveX = this.dx * getSpeedMultiplier() + this.inheritedVelocity.x;
        double intendedMoveY = this.dy * getSpeedMultiplier() + this.inheritedVelocity.y;
        double intendedMoveZ = this.dz * getSpeedMultiplier() + this.inheritedVelocity.z;

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

        double controlledFluctuation = getFluctuation() * Mth.lerp(this.startupProgress, 0.12f, 1.0f);
        this.dx += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.dy += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.dz += (this.random.nextDouble() - 0.5d) * controlledFluctuation;
        this.dx *= this.friction;
        this.dy *= this.friction;
        this.dz *= this.friction;

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

        this.quadSize = Mth.lerp(progress, this.startSize, this.endSize);
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
