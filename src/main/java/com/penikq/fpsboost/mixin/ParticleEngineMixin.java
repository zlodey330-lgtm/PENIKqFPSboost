package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Отключение частиц из вкладки "Частицы и мир".
 * Обычные частицы создаются через createParticle. Осколки блоков (ломание, удары) игра может добавлять
 * напрямую через add, поэтому здесь ловим и их: любая частица-осколок блока (TerrainParticle).
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideParticle(ParticleOptions options, double x, double y, double z,
									   double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
		if (MobConfig.hideBlockParticles && options instanceof BlockParticleOption) {
			cir.setReturnValue(null);
			return;
		}
		if (MobConfig.hasHiddenParticles() && MobConfig.isParticleHidden(options.getType())) {
			cir.setReturnValue(null);
		}
	}

	/** Общее место, куда попадает любая готовая частица: отсюда убираем осколки блоков. */
	@Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideTerrain(Particle particle, CallbackInfo ci) {
		if (MobConfig.hideBlockParticles && particle instanceof TerrainParticle) {
			ci.cancel();
		}
	}
}
