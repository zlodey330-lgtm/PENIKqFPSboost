package com.penikq.fpsboost;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Исключения для частиц блоков: у отмеченных блоков частицы показываются всегда,
 * даже если общая кнопка "Частицы блоков" выключена.
 */
public class BlockExceptionsScreen extends Screen {

	/** {id блока, название}. Важные блоки для PvP и баз. */
	private static final String[][] BLOCKS = {
			{"minecraft:obsidian", "Обсидиан"},
			{"minecraft:crying_obsidian", "Плачущий обсидиан"},
			{"minecraft:bedrock", "Бедрок"},
			{"minecraft:ender_chest", "Сундук Края"},
			{"minecraft:respawn_anchor", "Якорь возрождения"},
			{"minecraft:tnt", "Динамит"},
			{"minecraft:anvil", "Наковальня"},
			{"minecraft:chest", "Сундук"},
			{"minecraft:trapped_chest", "Сундук-ловушка"},
			{"minecraft:spawner", "Спавнер"}
	};

	private static final int BG_DIM = 0xA0000000;
	private static final int PANEL = 0xF0121218;
	private static final int PANEL_BORDER = 0xFF2A2A36;
	private static final int ACCENT = 0xFFE5384F;
	private static final int ACCENT_DARK = 0xFF8E2030;
	private static final int TEXT = 0xFFEDEDF2;
	private static final int TEXT_DIM = 0xFF8C8C9C;
	private static final int ROW = 0xFF1B1B25;
	private static final int ROW_HOVER = 0xFF232330;

	private static final int W = 420;
	private static final int H = 196;
	private static final int CELL_W = 196;
	private static final int CELL_H = 22;
	private static final int ROWS = 5;

	private final Screen parent;
	private int px, py;

	public BlockExceptionsScreen(Screen parent) {
		super(Component.literal("Исключения"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		px = (this.width - W) / 2;
		py = (this.height - H) / 2;
	}

	private int cellX(int i) {
		return px + 12 + (i / ROWS) * (CELL_W + 8);
	}

	private int cellY(int i) {
		return py + 48 + (i % ROWS) * (CELL_H + 2);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, BG_DIM);
		g.fill(px - 1, py - 1, px + W + 1, py + H + 1, PANEL_BORDER);
		g.fill(px, py, px + W, py + H, PANEL);
		g.fill(px, py, px + W, py + 2, ACCENT);

		g.drawString(this.font, Component.literal("Исключения для частиц блоков").withStyle(s -> s.withBold(true)),
				px + 12, py + 12, TEXT, false);
		g.drawString(this.font, "Отмеченные блоки всегда показывают свои частицы, даже когда общая кнопка выключена",
				px + 12, py + 28, TEXT_DIM, false);

		for (int i = 0; i < BLOCKS.length; i++) {
			int x = cellX(i), y = cellY(i);
			boolean on = MobConfig.isBlockExceptionId(BLOCKS[i][0]);
			boolean hover = inside(mouseX, mouseY, x, y, CELL_W, CELL_H);
			g.fill(x, y, x + CELL_W, y + CELL_H, hover ? ROW_HOVER : ROW);
			g.drawString(this.font, BLOCKS[i][1], x + 8, y + 7, TEXT, false);
			int tx = x + CELL_W - 8 - 60;
			g.fill(tx, y + 3, tx + 60, y + CELL_H - 3, on ? ACCENT : 0xFF24242E);
			g.drawCenteredString(this.font, on ? "ПОКАЗЫВАТЬ" : "как все", tx + 30, y + 7, on ? TEXT : TEXT_DIM);
		}

		int bw = 80;
		int bx = px + (W - bw) / 2;
		int by = py + H - 24;
		g.fill(bx, by, bx + bw, by + 16, inside(mouseX, mouseY, bx, by, bw, 16) ? 0xFF2E2E3A : 0xFF24242E);
		g.drawCenteredString(this.font, "Назад", bx + bw / 2, by + 4, TEXT);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			double mx = event.x();
			double my = event.y();
			for (int i = 0; i < BLOCKS.length; i++) {
				if (inside(mx, my, cellX(i), cellY(i), CELL_W, CELL_H)) {
					String id = BLOCKS[i][0];
					MobConfig.setBlockExceptionId(id, !MobConfig.isBlockExceptionId(id));
					MobConfig.save();
					return true;
				}
			}
			int bw = 80;
			int bx = px + (W - bw) / 2;
			if (inside(mx, my, bx, py + H - 24, bw, 16)) {
				this.onClose();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
