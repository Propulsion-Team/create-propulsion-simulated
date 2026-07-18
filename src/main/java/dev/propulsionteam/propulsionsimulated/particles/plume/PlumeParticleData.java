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
import dev.propulsionteam.propulsionsimulated.particles.ParticleTypes;

public class PlumeParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PlumeParticleData> {
    private final List<ResourceLocation> overrideTextures;
    private final Integer overrideColor;
    private final Float overrideSize;

    public PlumeParticleData() {
        this(List.of(), null);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor) {
        this(overrideTextures, overrideColor, null);
    }

    public PlumeParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize) {
        this.overrideTextures = overrideTextures == null ? List.of() : List.copyOf(overrideTextures);
        this.overrideColor = overrideColor;
        this.overrideSize = overrideSize;
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

    @Override
    public ParticleType<?> getType(){
        return ParticleTypes.getPlumeType();
    }

    public MapCodec<PlumeParticleData> getCodec(ParticleType<PlumeParticleData> type) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("override_textures", List.of()).forGetter(PlumeParticleData::overrideTextures),
            Codec.INT.optionalFieldOf("override_color").forGetter(data -> java.util.Optional.ofNullable(data.overrideColor)),
            Codec.FLOAT.optionalFieldOf("override_size").forGetter(data -> java.util.Optional.ofNullable(data.overrideSize))
        ).apply(instance, (textures, color, size) -> new PlumeParticleData(textures, color.orElse(null), size.orElse(null))));
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
        }, buf -> {
            List<ResourceLocation> textures = buf.readCollection(ArrayList::new, b -> b.readResourceLocation());
            Integer color = buf.readBoolean() ? buf.readInt() : null;
            Float size = buf.readBoolean() ? buf.readFloat() : null;
            return new PlumeParticleData(textures, color, size);
        });
    }

    @Override
	public SpriteParticleRegistration<PlumeParticleData> getMetaFactory() {
        return PlumeParticle.Factory::new;
	}
}
