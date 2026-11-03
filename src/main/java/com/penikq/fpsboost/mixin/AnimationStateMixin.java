package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.AnimFreeze;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Останавливает кадры анимации воды, лавы и портала: текстура остаётся на текущем кадре. */
@Mixin(SpriteContents.AnimationState.class)
public abstract class AnimationStateMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void fpsboost$freeze(CallbackInfo ci) {
		if (AnimFreeze.isFrozen(this)) {
			ci.cancel();
		}
	}
}
