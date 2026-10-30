package com.penikq.fpsboost;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Меню мода (клавиша H): 3 вкладки. Вкладки 1 и 2 пока показывают "Скоро...",
 * вкладка 3 "Оптимизация мобов" содержит список мобов с ползунками дальности.
 */
public class FpsBoostScreen extends Screen {

	// Цвета (тёмная тема с красным акцентом)
	private static final int BG_DIM = 0xA0000000;
	private static final int PANEL = 0xF0121218;
	private static final int PANEL_BORDER = 0xFF2A2A36;
	private static final int HEADER = 0xFF191922;
	private static final int ACCENT = 0xFFE5384F;
	private static final int ACCENT_DARK = 0xFF8E2030;
	private static final int TEXT = 0xFFEDEDF2;
	private static final int TEXT_DIM = 0xFF8C8C9C;
	private static final int ROW = 0xFF1B1B25;
	private static final int ROW_HOVER = 0xFF232330;
	private static final int TRACK = 0xFF0C0C11;

	private static final String[] TABS = {"Вкладка 1", "Вкладка 2", "Оптимизация мобов"};

	private static final int PANEL_W = 380;
	private static final int PANEL_H = 250;
	private static final int TAB_H = 24;
	private static final int ROW_H = 22;
	private static final int HEADING_H = 24;
	private static final int SCROLLBAR_W = 4;

	private int tab = 2;
	private EditBox search;
	private final List<MobCatalog.Mob> allMobs = MobCatalog.build();

	/** Элементы списка: либо заголовок раздела, либо моб. */
	private final List<Object> items = new ArrayList<>();
	private double scroll = 0;
	private MobCatalog.Mob dragging = null;
	private boolean changed = false;

	// Геометрия, пересчитывается в init()
	private int px, py;

	public FpsBoostScreen() {
		super(Component.literal("FPS Boost"));
	}

	@Override
	protected void init() {
		px = (this.width - PANEL_W) / 2;
		py = (this.height - PANEL_H) / 2;

		search = new EditBox(this.font, px + 12, py + TAB_H + 28, PANEL_W - 24 - 70, 16, Component.literal("Поиск"));
		search.setHint(Component.literal("Поиск моба..."));
		search.setMaxLength(32);
		search.setResponder(s -> {
			scroll = 0;
			rebuild();
		});
		this.addRenderableWidget(search);
		rebuild();
	}

	private void rebuild() {
		items.clear();
		String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		MobCatalog.Section last = null;
		for (MobCatalog.Mob mob : allMobs) {
			if (!q.isEmpty() && !mob.name().toLowerCase(Locale.ROOT).contains(q)) {
				continue;
			}
			if (mob.section() != last) {
				last = mob.section();
				items.add(last);
			}
			items.add(mob);
		}
	}

	// ---------- геометрия списка ----------

	private int listX() {
		return px + 12;
	}

	private int listY() {
		return py + TAB_H + 52;
	}

	private int listW() {
		return PANEL_W - 24;
	}

	private int listH() {
		return PANEL_H - TAB_H - 52 - 10;
	}

	private int contentHeight() {
		int h = 0;
		for (Object o : items) {
			h += (o instanceof MobCatalog.Section) ? HEADING_H : ROW_H;
		}
		return h;
	}

	private double maxScroll() {
		return Math.max(0, contentHeight() - listH());
	}

	// ---------- отрисовка ----------

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, BG_DIM);

		// панель
		g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, PANEL_BORDER);
		g.fill(px, py, px + PANEL_W, py + PANEL_H, PANEL);

		// шапка с вкладками
		g.fill(px, py, px + PANEL_W, py + TAB_H, HEADER);
		int tabW = PANEL_W / TABS.length;
		for (int i = 0; i < TABS.length; i++) {
			int tx = px + i * tabW;
			boolean active = i == tab;
			boolean hover = mouseX >= tx && mouseX < tx + tabW && mouseY >= py && mouseY < py + TAB_H;
			if (hover && !active) {
				g.fill(tx, py, tx + tabW, py + TAB_H, 0xFF20202B);
			}
			g.drawCenteredString(this.font, TABS[i], tx + tabW / 2, py + 8, active ? TEXT : TEXT_DIM);
			if (active) {
				g.fill(tx, py + TAB_H - 2, tx + tabW, py + TAB_H, ACCENT);
			}
		}

		if (tab == 2) {
			renderMobsTab(g, mouseX, mouseY, delta);
		} else {
			g.drawCenteredString(this.font, "Скоро...", px + PANEL_W / 2, py + PANEL_H / 2 - 4, TEXT_DIM);
		}
	}

	private void renderMobsTab(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// подсказка + кнопка сброса
		g.drawString(this.font, "Дальность отрисовки: 0 = не видно, " + MobConfig.MAX + " = без лимита",
				px + 12, py + TAB_H + 8, TEXT_DIM, false);

		int rbx = px + PANEL_W - 12 - 62;
		int rby = py + TAB_H + 28;
		boolean rh = mouseX >= rbx && mouseX < rbx + 62 && mouseY >= rby && mouseY < rby + 16;
		g.fill(rbx, rby, rbx + 62, rby + 16, rh ? ACCENT : ACCENT_DARK);
		g.drawCenteredString(this.font, "Сбросить", rbx + 31, rby + 4, TEXT);

		search.render(g, mouseX, mouseY, delta);

		// список (ножницы)
		int lx = listX(), ly = listY(), lw = listW(), lh = listH();
		g.enableScissor(lx, ly, lx + lw, ly + lh);

		int y = ly - (int) scroll;
		int rowW = lw - SCROLLBAR_W - 4;
		for (Object o : items) {
			if (o instanceof MobCatalog.Section section) {
				if (y + HEADING_H >= ly && y <= ly + lh) {
					// жирный заголовок раздела
					g.fill(lx, y + 4, lx + 3, y + HEADING_H - 4, section.color);
					g.drawString(this.font, Component.literal(section.title).withStyle(s -> s.withBold(true)),
							lx + 9, y + 8, section.color, false);
				}
				y += HEADING_H;
			} else if (o instanceof MobCatalog.Mob mob) {
				if (y + ROW_H >= ly && y <= ly + lh) {
					renderMobRow(g, mob, lx, y, rowW, mouseX, mouseY);
				}
				y += ROW_H;
			}
		}
		g.disableScissor();

		if (items.isEmpty()) {
			g.drawCenteredString(this.font, "Ничего не найдено", px + PANEL_W / 2, ly + 20, TEXT_DIM);
		}

		// полоса прокрутки
		double max = maxScroll();
		if (max > 0) {
			int sbx = lx + lw - SCROLLBAR_W;
			g.fill(sbx, ly, sbx + SCROLLBAR_W, ly + lh, TRACK);
			int thumbH = Math.max(20, (int) (lh * (lh / (double) contentHeight())));
			int thumbY = ly + (int) ((lh - thumbH) * (scroll / max));
			g.fill(sbx, thumbY, sbx + SCROLLBAR_W, thumbY + thumbH, ACCENT);
		}
	}

	private static final int NAME_W = 120;
	private static final int VALUE_W = 34;

	private void renderMobRow(GuiGraphics g, MobCatalog.Mob mob, int x, int y, int w, int mouseX, int mouseY) {
		boolean inList = mouseY >= listY() && mouseY < listY() + listH();
		boolean hover = inList && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_H - 2;
		g.fill(x, y, x + w, y + ROW_H - 2, hover || dragging == mob ? ROW_HOVER : ROW);

		g.drawString(this.font, mob.name(), x + 8, y + 7, TEXT, false);

		int value = MobConfig.get(mob.type());
		int tx = sliderX(x, w);
		int tw = sliderW(w);
		int ty = y + (ROW_H - 2) / 2 - 2;
		g.fill(tx, ty, tx + tw, ty + 4, TRACK);
		int filled = (int) (tw * (value / (double) MobConfig.MAX));
		g.fill(tx, ty, tx + filled, ty + 4, value >= MobConfig.MAX ? ACCENT_DARK : ACCENT);
		g.fill(tx + filled - 2, ty - 3, tx + filled + 2, ty + 7, TEXT);

		String label = value >= MobConfig.MAX ? "∞" : (value == 0 ? "выкл" : String.valueOf(value));
		int color = value == 0 ? ACCENT : (value >= MobConfig.MAX ? TEXT_DIM : TEXT);
		g.drawString(this.font, label, x + w - VALUE_W + 4, y + 7, color, false);
	}

	private int sliderX(int rowX, int rowW) {
		return rowX + NAME_W + 16;
	}

	private int sliderW(int rowW) {
		return rowW - NAME_W - 16 - VALUE_W - 12;
	}

	// ---------- ввод ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();

		if (event.button() == 0) {
			// вкладки
			if (my >= py && my < py + TAB_H && mx >= px && mx < px + PANEL_W) {
				int tabW = PANEL_W / TABS.length;
				int idx = Mth.clamp((int) ((mx - px) / tabW), 0, TABS.length - 1);
				tab = idx;
				search.setFocused(false);
				return true;
			}

			if (tab == 2) {
				// сброс
				int rbx = px + PANEL_W - 12 - 62;
				int rby = py + TAB_H + 28;
				if (mx >= rbx && mx < rbx + 62 && my >= rby && my < rby + 16) {
					MobConfig.resetAll();
					changed = true;
					return true;
				}

				// ползунки
				MobCatalog.Mob hit = mobAt(mx, my);
				if (hit != null) {
					int rowW = listW() - SCROLLBAR_W - 4;
					int tx = sliderX(listX(), rowW);
					int tw = sliderW(rowW);
					if (mx >= tx - 6 && mx <= tx + tw + 6) {
						dragging = hit;
						applySlider(hit, mx);
						return true;
					}
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private MobCatalog.Mob mobAt(double mx, double my) {
		if (mx < listX() || mx >= listX() + listW() || my < listY() || my >= listY() + listH()) {
			return null;
		}
		int y = listY() - (int) scroll;
		for (Object o : items) {
			int h = (o instanceof MobCatalog.Section) ? HEADING_H : ROW_H;
			if (o instanceof MobCatalog.Mob mob && my >= y && my < y + h - 2) {
				return mob;
			}
			y += h;
		}
		return null;
	}

	private void applySlider(MobCatalog.Mob mob, double mx) {
		int rowW = listW() - SCROLLBAR_W - 4;
		int tx = sliderX(listX(), rowW);
		int tw = sliderW(rowW);
		double t = Mth.clamp((mx - tx) / tw, 0.0, 1.0);
		MobConfig.set(mob.type(), (int) Math.round(t * MobConfig.MAX));
		changed = true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			applySlider(dragging, event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (tab == 2) {
			scroll = Mth.clamp(scroll - scrollY * 18, 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// H закрывает меню, но только когда не печатаем в поиске
		if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_H && !search.isFocused()) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (changed) {
			MobConfig.save();
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
