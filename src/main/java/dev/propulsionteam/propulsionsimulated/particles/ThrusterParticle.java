package dev.propulsionteam.propulsionsimulated.particles;

import dev.propulsionteam.propulsionsimulated.mixin.client.ParticleTrackingAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public abstract class ThrusterParticle extends SimpleAnimatedParticle implements ParticleSubLevelKickable {
    private static final boolean PYM = ModList.get().isLoaded("pym");
    private final ParticleScale particleScale = new ParticleScale();
    private final double initialOwnerScale;
    private boolean initialized;
    private Vec3 anchor;
    protected Vec3 inheritedVelocity = Vec3.ZERO;
    protected boolean hasCollided;

    protected ThrusterParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0f);
        SubLevel owner = Sable.HELPER.getContaining(level, new Vec3(x, y, z));
        initialOwnerScale = owner == null ? 1.0d : ParticleScale.valid(owner.logicalPose().scale().x());
        if (owner != null) anchor = new Vec3(x, y, z);
    }

    @Override
    public Particle scale(float factor) {
        super.scale(factor);
        particleScale.multiply(factor);
        setSize(0.2f * particleScale.size(), 0.2f * particleScale.size());
        return this;
    }

    protected float particleSize() {
        return particleScale.size();
    }

    protected void updateScale() {
        SubLevel owner = ((ParticleExtension) this).sable$getTrackingSubLevel();
        if (owner != null && owner.isRemoved()) {
            detach(owner);
            owner = null;
        }
        double scale = owner == null ? 1.0d : ParticleScale.valid(owner.logicalPose().scale().x());
        if (!initialized) {
            initialized = true;
            particleScale.initialize(initialOwnerScale, PYM);
        }
        if (owner != null) particleScale.resize(scale);
        setSize(0.2f * particleScale.size(), 0.2f * particleScale.size());
    }

    @Override
    public void move(double dx, double dy, double dz) {
        SubLevel owner = ((ParticleExtension) this).sable$getTrackingSubLevel();
        Vec3 movement = new Vec3(dx, dy, dz).scale(particleScale.motion()).add(inheritedVelocity);
        if (owner != null && !owner.isRemoved()) {
            Vec3 position = new Vec3(x, y, z);
            Vec3 local = owner.lastPose().transformPositionInverse(position);
            movement = movement.add(owner.logicalPose().transformPosition(local).subtract(position));
        }
        ParticleCollision.Hit hit = ParticleCollision.sweep(level, getBoundingBox(), movement, owner);
        onGround = false;
        if (hit != null) {
            double epsilon = 1.0e-5d * particleScale.owner();
            Vec3 normal = hit.normal();
            movement = hit.movement().add(normal.scale(epsilon));
            onGround = normal.y > 0.6d;
            hasCollided = true;
            collide(normal);
            double inheritedNormal = inheritedVelocity.dot(normal);
            if (inheritedNormal < 0.0d) inheritedVelocity = inheritedVelocity.subtract(normal.scale(inheritedNormal));
        }
        setBoundingBox(getBoundingBox().move(movement));
        setLocationFromBoundingbox();
        if (owner != null && anchor != null
                && owner.logicalPose().transformPositionInverse(new Vec3(x, y, z)).distanceToSqr(anchor) > 0.25d) {
            detach(owner);
        }
    }

    protected void collide(Vec3 normal) {
        Vec3 velocity = new Vec3(xd, yd, zd);
        double dot = velocity.dot(normal);
        if (dot >= 0.0d) return;
        Vec3 tangent = velocity.subtract(normal.scale(dot));
        Vec3 axis = Math.abs(normal.y) > 0.9d ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = normal.cross(axis).normalize();
        axis = side.cross(normal).normalize();
        double angle = random.nextDouble() * Math.PI * 2.0d;
        Vec3 spread = axis.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
        double incidence = -dot / velocity.length();
        Vec3 result = normal.scale(-dot * 0.1d)
                .add(tangent.scale(Math.sqrt(Math.max(0.0d, 1.0d - incidence * incidence))))
                .add(spread.scale(velocity.length() * incidence)).normalize().scale(velocity.length() * 0.9d);
        xd = result.x;
        yd = result.y;
        zd = result.z;
    }

    private void detach(SubLevel owner) {
        if (!owner.isRemoved()) {
            Vec3 local = owner.logicalPose().transformPositionInverse(new Vec3(x, y, z));
            try {
                Vec3 velocity = Sable.HELPER.getVelocity(level, local).scale(0.05d);
                if (Double.isFinite(velocity.x) && Double.isFinite(velocity.y) && Double.isFinite(velocity.z)) {
                    inheritedVelocity = velocity;
                }
            } catch (RuntimeException ignored) {
            }
        }
        ((ParticleTrackingAccessor) this).propulsion$setTrackingSubLevel(null);
        anchor = null;
    }

    @Override
    public boolean sable$shouldKickFromTracking() {
        return true;
    }

    @Override
    public boolean sable$shouldCollideWithTrackingSubLevel() {
        return false;
    }
}
