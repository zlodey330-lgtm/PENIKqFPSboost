package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.penikq.fpsboost.AnimFreeze;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Запоминает, какое состояние анимации принадлежит воде, лаве и порталу. */
@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin {

	@Inject(method = "createAnimationState", at = @At("RETURN"))
	private void fpsboost$track(GpuBufferSlice slice, int size,
								CallbackInfoReturnable<SpriteContents.AnimationState> cir) {
		SpriteContents self = (SpriteContents) (Object) this;
		AnimFreeze.track(cir.getReturnValue(), self.name().toString());
	}
}
