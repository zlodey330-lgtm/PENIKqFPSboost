package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает тыкву на экране и искажение от портала. */
@Mixin(Gui.class)
public abstract class GuiOverlayMixin {

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noPumpkin(GuiGraphics graphics, Identifier texture, float opacity, CallbackInfo ci) {
		if (MobConfig.hidePumpkin && texture.getPath().contains("pumpkinblur")) {
			ci.cancel();
		}
	}

	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noPortal(GuiGraphics graphics, float strength, CallbackInfo ci) {
		if (MobConfig.hidePortalOverlay) {
			ci.cancel();
		}
	}
}
