package com.penikq.fpsboost;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Учёт анимированных текстур, которые можно остановить (вода, лава, портал в Ад).
 * Анимация останавливается "на лету": состояние текстуры просто перестаёт обновляться,
 * поэтому перезагружать ресурсы не нужно.
 */
public final class AnimFreeze {

	private enum Kind {
		WATER, LAVA, PORTAL
	}

	private static final Map<Object, Kind> TRACKED = Collections.synchronizedMap(new WeakHashMap<>());

	private AnimFreeze() {
	}

	/** Запоминает состояние анимации, если это текстура воды, лавы или портала. */
	public static void track(Object animationState, String spriteName) {
		if (animationState == null || spriteName == null) {
			return;
		}
		Kind kind = null;
		if (spriteName.endsWith("block/water_still") || spriteName.endsWith("block/water_flow")) {
			kind = Kind.WATER;
		} else if (spriteName.endsWith("block/lava_still") || spriteName.endsWith("block/lava_flow")) {
			kind = Kind.LAVA;
		} else if (spriteName.endsWith("block/nether_portal")) {
			kind = Kind.PORTAL;
		}
		if (kind != null) {
			TRACKED.put(animationState, kind);
		}
	}

	public static boolean isFrozen(Object animationState) {
		Kind kind = TRACKED.get(animationState);
		if (kind == null) {
			return false;
		}
		return switch (kind) {
			case WATER -> MobConfig.hideWaterAnim;
			case LAVA -> MobConfig.hideLavaAnim;
			case PORTAL -> MobConfig.hidePortalAnim;
		};
	}
}
