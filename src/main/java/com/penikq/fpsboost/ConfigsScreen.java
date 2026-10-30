package com.penikq.fpsboost;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Свои конфиги: сохранить текущие настройки под именем, загрузить или удалить сохранённый. */
public class ConfigsScreen extends Screen {

	private static final int BG_DIM = 0xA0000000;
	private static final int PANEL = 0xF0121218;
	private static final int PANEL_BORDER = 0xFF2A2A36;
	private static final int ACCENT = 0xFFE5384F;
	private static final int ACCENT_DARK = 0xFF8E2030;
	private static final int TEXT = 0xFFEDEDF2;
	private static final int TEXT_DIM = 0xFF8C8C9C;
	private static final int ROW = 0xFF1B1B25;
	private static final int BTN = 0xFF24242E;
	private static final int BTN_HOVER = 0xFF2E2E3A;

	private static final int W = 300;
	private static final int H = 220;
	private static final int ROW_H = 22;
	private static final int VISIBLE_ROWS = 5;

	private final Screen parent;
	private EditBox nameBox;
	private List<String> names = new ArrayList<>();
	private int scrollRows = 0;
	private String status = "";
	private int statusColor = TEXT_DIM;
	private String confirmDelete = null;
	private int px, py;

	public ConfigsScreen(Screen parent) {
		super(Component.literal("Конфиги"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		px = (this.width - W) / 2;
		py = (this.height - H) / 2;
		nameBox = new EditBox(this.font, px + 12, py + 34, 170, 16, Component.literal("Название"));
		nameBox.setHint(Component.literal("Название конфига..."));
		nameBox.setMaxLength(24);
		this.addRenderableWidget(nameBox);
		refresh();
	}

	private void refresh() {
		names = MobConfig.listProfiles();
		scrollRows = Mth.clamp(scrollRows, 0, Math.max(0, names.size() - VISIBLE_ROWS));
	}

	private int listY() {
		return py + 62;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void drawButton(GuiGraphics g, int x, int y, int w, int h, String text, boolean hover, boolean accent) {
		int color = accent ? (hover ? ACCENT : ACCENT_DARK) : (hover ? BTN_HOVER : BTN);
		g.fill(x, y, x + w, y + h, color);
		g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, TEXT);
	}

	// геометрия кнопок строки
	private int deleteX() {
		return px + W - 12 - 52;
	}

	private int loadX() {
		return deleteX() - 4 - 62;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, BG_DIM);
		g.fill(px - 1, py - 1, px + W + 1, py + H + 1, PANEL_BORDER);
		g.fill(px, py, px + W, py + H, PANEL);
		g.fill(px, py, px + W, py + 2, ACCENT);

		g.drawString(this.font, Component.literal("Конфиги").withStyle(s -> s.withBold(true)),
				px + 12, py + 12, TEXT, false);
		g.drawString(this.font, "Сохраняют все настройки мода", px + 12 + 60, py + 12, TEXT_DIM, false);

		nameBox.render(g, mouseX, mouseY, delta);
		int sbx = px + 186;
		drawButton(g, sbx, py + 34, W - 12 - 186, 16, "Сохранить", inside(mouseX, mouseY, sbx, py + 34, W - 12 - 186, 16), true);

		int ly = listY();
		if (names.isEmpty()) {
			g.drawCenteredString(this.font, "Сохранённых конфигов нет", px + W / 2, ly + 36, TEXT_DIM);
		}
		for (int i = 0; i < VISIBLE_ROWS; i++) {
			int idx = scrollRows + i;
			if (idx >= names.size()) {
				break;
			}
			String name = names.get(idx);
			int y = ly + i * ROW_H;
			g.fill(px + 12, y, px + W - 12, y + ROW_H - 2, ROW);
			g.drawString(this.font, name, px + 20, y + 7, TEXT, false);

			boolean hl = inside(mouseX, mouseY, loadX(), y + 2, 62, 16);
			drawButton(g, loadX(), y + 2, 62, 16, "Загрузить", hl, false);
			boolean confirming = name.equals(confirmDelete);
			boolean hd = inside(mouseX, mouseY, deleteX(), y + 2, 52, 16);
			drawButton(g, deleteX(), y + 2, 52, 16, confirming ? "Точно?" : "Удалить", hd, confirming);
		}

		if (names.size() > VISIBLE_ROWS) {
			g.drawString(this.font, "Колесо мыши: ещё конфиги (" + names.size() + ")", px + 12,
					py + H - 44, TEXT_DIM, false);
		}
		if (!status.isEmpty()) {
			g.drawString(this.font, status, px + 12, py + H - 30, statusColor, false);
		}
		int bw = 80;
		int bx = px + (W - bw) / 2;
		drawButton(g, bx, py + H - 22, bw, 16, "Назад", inside(mouseX, mouseY, bx, py + H - 22, bw, 16), false);
	}

	private void say(String text, boolean ok) {
		status = text;
		statusColor = ok ? 0xFF7DDC8C : 0xFFFF6B6B;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		if (event.button() == 0) {
			// сохранить
			int sbx = px + 186;
			if (inside(mx, my, sbx, py + 34, W - 12 - 186, 16)) {
				confirmDelete = null;
				String name = MobConfig.sanitizeName(nameBox.getValue());
				if (name == null) {
					say("Введи название конфига", false);
				} else if (MobConfig.saveProfile(name)) {
					say("Сохранено: " + name, true);
					nameBox.setValue("");
					refresh();
				} else {
					say("Не удалось сохранить", false);
				}
				return true;
			}

			// строки списка
			for (int i = 0; i < VISIBLE_ROWS; i++) {
				int idx = scrollRows + i;
				if (idx >= names.size()) {
					break;
				}
				String name = names.get(idx);
				int y = listY() + i * ROW_H;
				if (inside(mx, my, loadX(), y + 2, 62, 16)) {
					confirmDelete = null;
					if (MobConfig.loadProfile(name)) {
						say("Загружено: " + name, true);
					} else {
						say("Не удалось загрузить", false);
					}
					return true;
				}
				if (inside(mx, my, deleteX(), y + 2, 52, 16)) {
					if (name.equals(confirmDelete)) {
						confirmDelete = null;
						MobConfig.deleteProfile(name);
						say("Удалено: " + name, true);
						refresh();
					} else {
						confirmDelete = name;
					}
					return true;
				}
			}

			// назад
			int bw = 80;
			int bx = px + (W - bw) / 2;
			if (inside(mx, my, bx, py + H - 22, bw, 16)) {
				this.onClose();
				return true;
			}
			confirmDelete = null;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		scrollRows = Mth.clamp(scrollRows - (int) Math.signum(scrollY), 0, Math.max(0, names.size() - VISIBLE_ROWS));
		return true;
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
