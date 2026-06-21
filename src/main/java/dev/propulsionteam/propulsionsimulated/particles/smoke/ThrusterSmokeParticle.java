package dev.propulsionteam.propulsionsimulated.particles.smoke;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class ThrusterSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseScale;

    protected ThrusterSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ThrusterSmokeParticleData data, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.baseScale = data.scale();

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = Math.max(12, data.lifetime());
        this.quadSize = data.scale() * (0.42f + this.random.nextFloat() * 0.22f);

        this.rCol = data.r() * (0.88f + this.random.nextFloat() * 0.16f);
        this.gCol = data.g() * (0.88f + this.random.nextFloat() * 0.16f);
        this.bCol = data.b() * (0.88f + this.random.nextFloat() * 0.16f);

        this.alpha = 0.0f;
        this.hasPhysics = true;
        this.gravity = -0.002f;
        this.friction = 0.935f;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime) {
            remove();
            return;
        }

        float life = age / (float) lifetime;

        float fadeIn = smoothstep(0.0f, 0.18f, life);
        float fadeOut = 1.0f - smoothstep(0.62f, 1.0f, life);

        alpha = 0.46f * fadeIn * fadeOut;

        quadSize = baseScale * (0.42f + life * 0.95f);

        double swirl = Math.sin(age * 0.13d + lifetime * 0.17d) * 0.0018d;
        xd += swirl;
        zd -= swirl;

        yd += 0.0008d + life * 0.0009d;

        move(xd, yd, zd);

        xd *= friction;
        yd *= 0.965d;
        zd *= friction;

        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public static class Provider implements ParticleProvider<ThrusterSmokeParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public ThrusterSmokeParticle createParticle(ThrusterSmokeParticleData data, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ThrusterSmokeParticle(level, x, y, z, xd, yd, zd, data, sprites);
        }
    }
}