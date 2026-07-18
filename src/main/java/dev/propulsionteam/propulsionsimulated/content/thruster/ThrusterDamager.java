package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.debug.PropulsionDebug;
import dev.propulsionteam.propulsionsimulated.debug.routes.MainDebugRoute;
import dev.propulsionteam.propulsionsimulated.utility.AbstractAreaDamagerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

import java.awt.Color;
import java.util.Optional;

public class ThrusterDamager extends AbstractAreaDamagerBehaviour {
    private record ThrusterDamageContext(Vec3 nozzlePos, float visualPowerPercent) {}

    public ThrusterDamager(SmartBlockEntity be) {
        super(be);
    }

    private AbstractThrusterBlockEntity getThruster() {
        return (AbstractThrusterBlockEntity) this.blockEntity;
    }

    @Override
    protected int getTickFrequency() {
        return 5;
    }

    @Override
    protected boolean shouldDamage() {
        return getThruster().shouldDamageEntities();
    }

    @Override
    protected DamageSource getDamageSource() {
        return getWorld().damageSources().onFire();
    }

    @Override
    protected Optional<DamageZone> calculateDamageZone() {
        AbstractThrusterBlockEntity thruster = getThruster();
        Direction plumeDirection = thruster.getFacing().getOpposite();

        float currentPower = thruster.getEffectiveThrottle();
        float threshold = AbstractThrusterBlockEntity.LOWEST_POWER_THRESHOLD;
        if (currentPower < threshold) return Optional.empty();

        float visualPowerPercent = Math.max(0, currentPower - threshold) / (1.0f - threshold);

        float distanceByPower = Math.lerp(0.55f, 1.5f, visualPowerPercent);
        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        if (potentialPlumeLength <= 0.01d) {
            return Optional.empty();
        }

        AbstractThrusterBlockEntity.WorldExhaustRay worldRay = thruster.getWorldExhaustRay();
        if (worldRay == null) {
            return Optional.empty();
        }

        Vec3 nozzlePos = worldRay.nozzlePos();
        Vec3 worldPlumeDirection = worldRay.direction();

        double correctedPlumeLength = performRaycastCheck(worldRay.level(), nozzlePos, worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) {
            return Optional.empty();
        }

        Vec3 boxDimensions = new Vec3(1.4, 1.4, correctedPlumeLength);
        double plumeStartOffset = 0.8;
        double centerOffsetDistance = plumeStartOffset + (correctedPlumeLength / 2.0);
        Vec3 normalizedWorldDirection = worldPlumeDirection.lengthSqr() < 1.0e-8
            ? Vec3.atLowerCornerOf(plumeDirection.getNormal())
            : worldPlumeDirection.normalize();
        Vec3 worldBoxCenter = nozzlePos.add(normalizedWorldDirection.scale(centerOffsetDistance));
        WorldOrientedBox worldBox = new WorldOrientedBox(worldRay.level(), worldBoxCenter, normalizedWorldDirection);

        ThrusterDamageContext context = new ThrusterDamageContext(nozzlePos, visualPowerPercent);
        return Optional.of(new DamageZone(
            boxDimensions,
            Vec3.ZERO,
            normalizedWorldDirection,
            Vec3.atLowerCornerOf(Direction.SOUTH.getNormal()),
            worldBox,
            context
        ));
    }

    @Override
    protected void applyDamage(LivingEntity entity, DamageSource source, DamageZone zone) {
        ThrusterDamageContext context = (ThrusterDamageContext) zone.context();
        
        float invSqrDistance = (context.visualPowerPercent() * 15.0f) * 8.0f / (float)java.lang.Math.max(1, entity.position().distanceToSqr(context.nozzlePos()));
        float damageAmount = 3 + invSqrDistance;

        entity.igniteForSeconds(3);
        entity.hurt(source, damageAmount);
    }

    @Override
    protected boolean shouldDebug() {
        return PropulsionDebug.isDebug(MainDebugRoute.THRUSTER);
    }

    @Override
    protected Color getDebugColor() {
        return Color.ORANGE;
    }

    private double performRaycastCheck(Level level, Vec3 nozzlePos, Vec3 worldPlumeDirection, double maxDistance) {
        Vec3 endPos = nozzlePos.add(worldPlumeDirection.scale(maxDistance));

        var clipContext = new net.minecraft.world.level.ClipContext(
            nozzlePos,
            endPos,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        );

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == net.minecraft.world.phys.BlockHitResult.Type.BLOCK) {
            return nozzlePos.distanceTo(hitResult.getLocation());
        }

        return maxDistance;
    }
}
