package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Если моб дальше своего лимита из меню (или лимит 0), он просто не отрисовывается.
 * Это и даёт прирост FPS: сущность не попадает в рендер вообще.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void fpsboost$limitDistance(Entity entity, Frustum frustum, double camX, double camY, double camZ,
										CallbackInfoReturnable<Boolean> cir) {
		if (!MobConfig.hasLimits()) {
			return;
		}
		Integer limit = MobConfig.limit(entity.getType());
		if (limit == null) {
			return;
		}
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
		}
	}
}
