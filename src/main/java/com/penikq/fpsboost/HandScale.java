package com.penikq.fpsboost;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;

/** Какой размер должен быть у предмета в руке (1.0 = обычный). */
public final class HandScale {

	private HandScale() {
	}

	public static float scaleFor(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
		if (stack.isEmpty()) {
			return 1.0F;
		}
		if (stack.is(Items.TOTEM_OF_UNDYING)) {
			return percent(MobConfig.scaleTotem);
		}
		if (stack.is(ItemTags.SWORDS)) {
			return percent(MobConfig.scaleSword);
		}
		if (stack.is(Items.SHIELD)) {
			return percent(MobConfig.scaleShield);
		}
		// Зелье и еда уменьшаются только пока ты пьёшь или ешь
		if (player.isUsingItem() && player.getUsedItemHand() == hand) {
			ItemUseAnimation animation = stack.getUseAnimation();
			if (animation == ItemUseAnimation.DRINK) {
				return percent(MobConfig.scalePotion);
			}
			if (animation == ItemUseAnimation.EAT) {
				return percent(MobConfig.scaleFood);
			}
		}
		return 1.0F;
	}

	private static float percent(int value) {
		return Math.max(MobConfig.SCALE_MIN, Math.min(MobConfig.SCALE_MAX, value)) / 100.0F;
	}
}
