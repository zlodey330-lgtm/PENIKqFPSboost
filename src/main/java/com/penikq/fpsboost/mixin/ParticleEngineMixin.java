package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Частицы, которые ты отключил во вкладке "Частицы и мир", просто не создаются. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideParticle(ParticleOptions options, double x, double y, double z,
									   double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
		if (MobConfig.hasHiddenParticles() && MobConfig.isParticleHidden(options.getType())) {
			cir.setReturnValue(null);
		}
	}
}
