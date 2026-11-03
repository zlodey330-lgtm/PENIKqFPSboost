package com.penikq.fpsboost;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** Очистка памяти одной кнопкой (кнопка в меню и клавиша, которую можно поменять в управлении). */
public final class MemoryCleaner {
	private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

	private MemoryCleaner() {
	}

	public static long usedMb() {
		Runtime rt = Runtime.getRuntime();
		return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
	}

	public static long maxMb() {
		return Runtime.getRuntime().maxMemory() / (1024 * 1024);
	}

	/** Запускает очистку в отдельном потоке, чтобы игра не зависала. */
	public static void run() {
		if (!RUNNING.compareAndSet(false, true)) {
			return;
		}
		long before = usedMb();
		Thread thread = new Thread(() -> {
			try {
				System.gc();
			} finally {
				long after = usedMb();
				long freed = Math.max(0, before - after);
				Minecraft mc = Minecraft.getInstance();
				mc.execute(() -> {
					if (mc.player != null) {
						mc.player.displayClientMessage(
								Component.literal("Память очищена: освобождено ~" + freed + " МБ (занято " + after + " МБ)"),
								true);
					}
					RUNNING.set(false);
				});
			}
		}, "fpsboost-memory-clean");
		thread.setDaemon(true);
		thread.start();
	}
}
