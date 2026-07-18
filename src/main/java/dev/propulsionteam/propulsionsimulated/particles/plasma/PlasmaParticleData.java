package dev.propulsionteam.propulsionsimulated.particles.plasma;

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

public class PlasmaParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PlasmaParticleData> {
    private final List<ResourceLocation> overrideTextures;
    private final Integer overrideColor;
    private final Float overrideSize;

    public PlasmaParticleData() {
        this(List.of(), null);
    }

    public PlasmaParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor) {
        this(overrideTextures, overrideColor, null);
    }

    public PlasmaParticleData(List<ResourceLocation> overrideTextures, Integer overrideColor, Float overrideSize) {
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
        return ParticleTypes.getPlasmaType();
    }

    public MapCodec<PlasmaParticleData> getCodec(ParticleType<PlasmaParticleData> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("override_textures", List.of()).forGetter(PlasmaParticleData::overrideTextures),
            Codec.INT.optionalFieldOf("override_color").forGetter(data -> java.util.Optional.ofNullable(data.overrideColor)),
            Codec.FLOAT.optionalFieldOf("override_size").forGetter(data -> java.util.Optional.ofNullable(data.overrideSize))
        ).apply(instance, (textures, color, size) -> new PlasmaParticleData(textures, color.orElse(null), size.orElse(null))));
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, PlasmaParticleData> getStreamCodec() {
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
            return new PlasmaParticleData(textures, color, size);
        });
    }

    @Override
    public SpriteParticleRegistration<PlasmaParticleData> getMetaFactory() {
        return PlasmaParticle.Factory::new;
    }
}
