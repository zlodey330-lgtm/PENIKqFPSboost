package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import com.penikq.fpsboost.OcclusionCuller;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Решает, рисовать ли сущность:
 * 1) дальше своего лимита из вкладки 3 (или лимит 0) — не рисуем;
 * 2) спрятана за блоками (вкладка 2, включается кнопкой) — не рисуем.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void fpsboost$shouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ,
									   CallbackInfoReturnable<Boolean> cir) {
		Integer limit = MobConfig.hasLimits() ? MobConfig.limit(entity.getType()) : null;
		if (limit != null) {
			if (limit <= 0) {
				cir.setReturnValue(false);
				return;
			}
			double dx = entity.getX() - camX;
			double dy = entity.getY() - camY;
			double dz = entity.getZ() - camZ;
			double max = limit.doubleValue();
			if (dx * dx + dy * dy + dz * dz > max * max) {
				cir.setReturnValue(false);
				return;
			}
		}

		if (MobConfig.occlusionCull && OcclusionCuller.shouldHide(entity, camX, camY, camZ)) {
			cir.setReturnValue(false);
		}
	}
}
