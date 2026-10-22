package dev.propulsionteam.propulsionsimulated.mixin.client;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Particle.class, priority = 500)
public interface ParticleTrackingAccessor {
    @Accessor(value = "sable$trackingSubLevel", remap = false)
    void propulsion$setTrackingSubLevel(ClientSubLevel subLevel);
}
