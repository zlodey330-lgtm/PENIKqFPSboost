package com.penikq.fpsboost;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FpsBoostClient implements ClientModInitializer {
	public static final String MOD_ID = "fpsboost";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openMenuKey;
	private static KeyMapping cleanMemoryKey;

	public static KeyMapping openMenuKey() {
		return openMenuKey;
	}

	public static KeyMapping cleanMemoryKey() {
		return cleanMemoryKey;
	}

	@Override
	public void onInitializeClient() {
		long startNs = System.nanoTime();
		MobConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.fpsboost.open_menu",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_H,
				category
		));
		// Клавиша очистки памяти по умолчанию не назначена: выбери её в настройках управления.
		cleanMemoryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.fpsboost.clean_memory",
				InputConstants.Type.KEYSYM,
				-1,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new FpsBoostScreen());
				}
			}
			while (cleanMemoryKey.consumeClick()) {
				MemoryCleaner.run();
			}
		});

		// Замер: сколько времени мод тратит на свой запуск (смотри в latest.log).
		LOGGER.info("[PENIKqFPSboost] запуск мода занял {} мс", (System.nanoTime() - startNs) / 1_000_000);
	}
}
