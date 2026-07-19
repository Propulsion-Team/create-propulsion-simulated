package dev.propulsionteam.propulsionsimulated.particles.plume;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.propulsionteam.propulsionsimulated.particles.ParticleTypes;

public class PlumeParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PlumeParticleData> {
    private final List<ResourceLocation> overrideTextures;
    private final Integer overrideColor;
    private final Float overrideSize;
    private final Float startupProgress;
    private final Vec3 inheritedVelocity;
    private final float trailCoverage;

    public PlumeParticleData() {
        this(List.of(), null, null, null, Vec3.ZERO);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor) {
        this(overrideTextures, overrideColor, null, null, Vec3.ZERO);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize) {
        this(overrideTextures, overrideColor, overrideSize, null, Vec3.ZERO);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize, Float startupProgress) {
        this(overrideTextures, overrideColor, overrideSize, startupProgress, Vec3.ZERO);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize,
                             Float startupProgress, Vec3 inheritedVelocity) {
        this(overrideTextures, overrideColor, overrideSize, startupProgress, inheritedVelocity, 0.0f);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize,
                             Float startupProgress, Vec3 inheritedVelocity, float trailCoverage) {
        this.overrideTextures = overrideTextures == null ? List.of() : List.copyOf(overrideTextures);
        this.overrideColor = overrideColor;
        this.overrideSize = overrideSize;
        this.startupProgress = startupProgress;
        this.inheritedVelocity = inheritedVelocity == null ? Vec3.ZERO : inheritedVelocity;
        this.trailCoverage = Mth.clamp(trailCoverage, 0.0f, 1.0f);
    }

    public List<ResourceLocation> overrideTextures() {
        return overrideTextures;
    }

    public Integer overrideColor() {
        return overrideColor;
    }

    public Float overrideSize() {
        return overrideSize;
    }

    public Float startupProgress() { return startupProgress; }

    public Vec3 inheritedVelocity() { return inheritedVelocity; }
    public float trailCoverage() { return trailCoverage; }

    @Override
    public ParticleType<?> getType(){
        return ParticleTypes.getPlumeType();
    }

    public MapCodec<PlumeParticleData> getCodec(ParticleType<PlumeParticleData> type) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("override_textures", List.of()).forGetter(PlumeParticleData::overrideTextures),
            Codec.INT.optionalFieldOf("override_color").forGetter(data -> java.util.Optional.ofNullable(data.overrideColor)),
            Codec.FLOAT.optionalFieldOf("override_size").forGetter(data -> java.util.Optional.ofNullable(data.overrideSize)),
            Codec.FLOAT.optionalFieldOf("startup_progress").forGetter(data -> java.util.Optional.ofNullable(data.startupProgress)),
            Codec.DOUBLE.optionalFieldOf("inherited_velocity_x", 0.0d).forGetter(data -> data.inheritedVelocity.x),
            Codec.DOUBLE.optionalFieldOf("inherited_velocity_y", 0.0d).forGetter(data -> data.inheritedVelocity.y),
            Codec.DOUBLE.optionalFieldOf("inherited_velocity_z", 0.0d).forGetter(data -> data.inheritedVelocity.z),
            Codec.FLOAT.optionalFieldOf("trail_coverage", 0.0f).forGetter(PlumeParticleData::trailCoverage)
        ).apply(instance, (textures, color, size, startup, vx, vy, vz, coverage) -> new PlumeParticleData(
                textures, color.orElse(null), size.orElse(null), startup.orElse(null), new Vec3(vx, vy, vz), coverage)));
	}

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, PlumeParticleData> getStreamCodec() {
        return StreamCodec.of((buf, data) -> {
            buf.writeCollection(data.overrideTextures, (b, rl) -> b.writeResourceLocation(rl));
            buf.writeBoolean(data.overrideColor != null);
            if (data.overrideColor != null) {
                buf.writeInt(data.overrideColor);
            }
            buf.writeBoolean(data.overrideSize != null);
            if (data.overrideSize != null) {
                buf.writeFloat(data.overrideSize);
            }
            buf.writeBoolean(data.startupProgress != null);
            if (data.startupProgress != null) buf.writeFloat(data.startupProgress);
            buf.writeDouble(data.inheritedVelocity.x);
            buf.writeDouble(data.inheritedVelocity.y);
            buf.writeDouble(data.inheritedVelocity.z);
            buf.writeFloat(data.trailCoverage);
        }, buf -> {
            List<ResourceLocation> textures = buf.readCollection(ArrayList::new, b -> b.readResourceLocation());
            Integer color = buf.readBoolean() ? buf.readInt() : null;
            Float size = buf.readBoolean() ? buf.readFloat() : null;
            Float startup = buf.readBoolean() ? buf.readFloat() : null;
            Vec3 inheritedVelocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            float coverage = buf.readFloat();
            return new PlumeParticleData(textures, color, size, startup, inheritedVelocity, coverage);
        });
    }

    @Override
	public SpriteParticleRegistration<PlumeParticleData> getMetaFactory() {
        return PlumeParticle.Factory::new;
	}
}
