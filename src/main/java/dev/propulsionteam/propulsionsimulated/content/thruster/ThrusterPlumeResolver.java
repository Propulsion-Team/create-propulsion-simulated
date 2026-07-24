package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import net.minecraft.core.particles.ParticleOptions;

import java.util.List;

/** Resolves the exact plume profile once; emitters and renderers must not add their own gates. */
public final class ThrusterPlumeResolver {
    private ThrusterPlumeResolver() {}

    public static ThrusterPlumeSpec resolve(AbstractThrusterBlockEntity be) {
        if (be == null) return ThrusterPlumeSpec.inactive(ThrusterPlumeSpec.Style.FIRE);
        ThrusterPlumeSpec.Style style = styleFor(be);
        if (!be.isController() || be.getUnobstructedBlocks() <= 0) {
            return ThrusterPlumeSpec.inactive(style);
        }

        boolean active = isActive(be);
        if (!active) return ThrusterPlumeSpec.inactive(style);

        float visualPower = be.isFadingOut() ? be.getEffectiveThrottle() : be.getPower();
        return new ThrusterPlumeSpec(true, Math.max(0.0f, visualPower), scaleForMultiblock(particleFor(be), be.width), style);
    }

    private static boolean isActive(AbstractThrusterBlockEntity be) {
        if (be.getPower() <= 0.0f && !be.isFadingOut()) return false;
        if (be instanceof CreativeVectorThrusterBlockEntity creativeVector) {
            return creativeVector.getPlumeType() != CreativeThrusterBlockEntity.PlumeType.NONE;
        }
        if (be instanceof CreativeThrusterBlockEntity creative) {
            return creative.getPlumeType() != CreativeThrusterBlockEntity.PlumeType.NONE;
        }
        if (be instanceof SolidFuelThrusterBlockEntity solid) {
            return solid.getBurnTime() > 0 && solid.validFuel();
        }
        if (be instanceof LiquidVectorThrusterBlockEntity liquid) {
            return liquid.validFluid();
        }
        if (be instanceof IonThrusterBlockEntity ion) {
            return ion.getFuelAmountMb() > 0;
        }
        if (be instanceof ThrusterBlockEntity thruster) {
            return thruster.validFluid();
        }
        return be.isVisuallyActive();
    }

    private static ThrusterPlumeSpec.Style styleFor(AbstractThrusterBlockEntity be) {
        if (be instanceof SolidFuelThrusterBlockEntity solid)
            return solid.isSuperHeated() ? ThrusterPlumeSpec.Style.SUPERHEATED_SOLID : ThrusterPlumeSpec.Style.SOLID;
        if (be instanceof CreativeVectorThrusterBlockEntity creativeVector)
            return styleForCreative(creativeVector.getPlumeType());
        if (be instanceof CreativeThrusterBlockEntity creative)
            return styleForCreative(creative.getPlumeType());
        if (be instanceof LiquidVectorThrusterBlockEntity) return ThrusterPlumeSpec.Style.FIRE;
        if (be instanceof VectorThrusterBlockEntity) return ThrusterPlumeSpec.Style.VECTOR;
        if (be instanceof IonThrusterBlockEntity) return ThrusterPlumeSpec.Style.ION;
        return ThrusterPlumeSpec.Style.FIRE;
    }

    private static ThrusterPlumeSpec.Style styleForCreative(CreativeThrusterBlockEntity.PlumeType type) {
        return switch (type) {
            case ION -> ThrusterPlumeSpec.Style.ION;
            case PLASMA -> ThrusterPlumeSpec.Style.PLASMA;
            case PLUME, NONE -> ThrusterPlumeSpec.Style.FIRE;
        };
    }

    private static ParticleOptions particleFor(AbstractThrusterBlockEntity be) {
        if (be instanceof CreativeVectorThrusterBlockEntity creativeVector)
            return creativeVector.createCreativeVectorPlumeParticleOptions();
        if (be instanceof CreativeThrusterBlockEntity creative)
            return particleForCreative(creative.getPlumeType(), creative.getDyeColor());
        if (be instanceof LiquidVectorThrusterBlockEntity liquid)
            return liquid.createResolvedParticleOptions();
        if (be instanceof ThrusterBlockEntity thruster && !(be instanceof IonThrusterBlockEntity))
            return thruster.createResolvedParticleOptions();
        if (be instanceof VectorThrusterBlockEntity vector)
            return vector.createVectorPlumeParticleOptions();
        if (be instanceof IonThrusterBlockEntity ion)
            return new IonParticleData(List.of(), ion.getDyeColor(), null);
        return new PlumeParticleData();
    }

    private static ParticleOptions particleForCreative(CreativeThrusterBlockEntity.PlumeType type, Integer color) {
        return switch (type) {
            case PLASMA -> new PlasmaParticleData(List.of(), color);
            case ION -> new IonParticleData(List.of(), color, null);
            case PLUME, NONE -> new PlumeParticleData(List.of(), color);
        };
    }

    private static ParticleOptions scaleForMultiblock(ParticleOptions particle, int width) {
        float scale = switch (Math.max(1, width)) {
            case 1 -> 1.0f;
            case 2 -> 1.46f;
            default -> 2.08f;
        };
        if (particle instanceof PlumeParticleData plume)
            return new PlumeParticleData(plume.overrideTextures(), plume.overrideColor(), scale, plume.startupProgress());
        if (particle instanceof PlasmaParticleData plasma)
            return new PlasmaParticleData(plasma.overrideTextures(), plasma.overrideColor(), scale, plasma.startupProgress());
        if (particle instanceof IonParticleData ion) {
            float baseSize = ion.overrideSize() == null ? 0.95f : ion.overrideSize();
            return new IonParticleData(ion.overrideTextures(), ion.overrideColor(), baseSize * scale, ion.startupProgress());
        }
        return particle;
    }
}
