package dev.propulsionteam.propulsionsimulated.particles.smoke;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleData;
import dev.propulsionteam.propulsionsimulated.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public record ThrusterSmokeParticleData(
        float scale,
        int lifetime,
        float r,
        float g,
        float b
) implements ParticleOptions, ICustomParticleData<ThrusterSmokeParticleData> {

    public ThrusterSmokeParticleData() {
        this(2.5f, 55, 0.24f, 0.24f, 0.27f);
    }

    public static final MapCodec<ThrusterSmokeParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.FLOAT.fieldOf("scale").forGetter(ThrusterSmokeParticleData::scale),
            com.mojang.serialization.Codec.INT.fieldOf("lifetime").forGetter(ThrusterSmokeParticleData::lifetime),
            com.mojang.serialization.Codec.FLOAT.fieldOf("r").forGetter(ThrusterSmokeParticleData::r),
            com.mojang.serialization.Codec.FLOAT.fieldOf("g").forGetter(ThrusterSmokeParticleData::g),
            com.mojang.serialization.Codec.FLOAT.fieldOf("b").forGetter(ThrusterSmokeParticleData::b)
    ).apply(instance, ThrusterSmokeParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrusterSmokeParticleData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeFloat(data.scale());
                buf.writeVarInt(data.lifetime());
                buf.writeFloat(data.r());
                buf.writeFloat(data.g());
                buf.writeFloat(data.b());
            },
            buf -> new ThrusterSmokeParticleData(
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    @Override
    public ParticleType<?> getType() {
        return ParticleTypes.getSmokeType();
    }

    @Override
    public MapCodec<ThrusterSmokeParticleData> getCodec(ParticleType<ThrusterSmokeParticleData> type) {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ThrusterSmokeParticleData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public net.minecraft.client.particle.ParticleProvider<ThrusterSmokeParticleData> getFactory() {
        return (data, level, x, y, z, xd, yd, zd) -> null;
    }

    @Override
    public void register(ParticleType<ThrusterSmokeParticleData> type, RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(type, ThrusterSmokeParticle.Provider::new);
    }
}