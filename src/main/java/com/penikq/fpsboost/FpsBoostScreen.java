package com.penikq.fpsboost;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

	private static final String[] TABS = {"Вкладка 1", "Умная отрисовка", "Оптимизация мобов"};

	private static final int PANEL_W = 380;
	private static final int PANEL_H = 250;
	private static final int TAB_H = 24;
	private static final int ROW_H = 22;
	private static final int HEADING_H = 24;
	private static final int SCROLLBAR_W = 6;

	private int tab = 2;
	private EditBox search;
	private final List<MobCatalog.Mob> allMobs = MobCatalog.get();

	/** Элементы списка: либо заголовок раздела, либо моб. */
	private final List<Object> items = new ArrayList<>();
	private double scroll = 0;
	private MobCatalog.Mob dragging = null;
	private boolean draggingSimplify = false;
	private boolean draggingScrollbar = false;
	private double scrollGrabOffset = 0;
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

		search = new EditBox(this.font, px + 12, py + TAB_H + 28, PANEL_W - 24 - 134 - 6, 16, Component.literal("Поиск"));
		search.setHint(Component.literal("Поиск моба..."));
		search.setMaxLength(32);
		search.setResponder(s -> {
			scroll = 0;
			rebuild();
		});
		this.addRenderableWidget(search);
		search.visible = tab == 2;
		rebuild();
	}

	private void switchTab(int idx) {
		tab = idx;
		search.setFocused(false);
		search.visible = tab == 2;
		dragging = null;
		draggingSimplify = false;
		draggingScrollbar = false;
	}

	// ---------- полоса прокрутки ----------

	private int scrollbarX() {
		return listX() + listW() - SCROLLBAR_W;
	}

	private int thumbH() {
		int content = Math.max(1, contentHeight());
		return Math.max(20, (int) (listH() * (listH() / (double) content)));
	}

	private int thumbY() {
		double max = maxScroll();
		if (max <= 0) {
			return listY();
		}
		return listY() + (int) ((listH() - thumbH()) * (scroll / max));
	}

	/** Ставит прокрутку так, чтобы верх ползунка оказался на экранной координате top. */
	private void scrollToThumbTop(double top) {
		double track = listH() - thumbH();
		if (track <= 0) {
			return;
		}
		double t = Mth.clamp((top - listY()) / track, 0.0, 1.0);
		scroll = t * maxScroll();
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
		} else if (tab == 1) {
			renderSmartTab(g, mouseX, mouseY);
		} else {
			g.drawCenteredString(this.font, "Скоро...", px + PANEL_W / 2, py + PANEL_H / 2 - 4, TEXT_DIM);
		}
	}

	// ---------- вкладка 2: Умная отрисовка ----------

	private static final int CARD1_H = 98;
	private static final int CARD2_H = 76;

	private int cardX() {
		return px + 12;
	}

	private int cardW() {
		return PANEL_W - 24;
	}

	private int card1Y() {
		return py + TAB_H + 12;
	}

	private int card2Y() {
		return card1Y() + CARD1_H + 8;
	}

	private int toggleX() {
		return cardX() + cardW() - 10 - 40;
	}

	private int simplifySliderX() {
		return cardX() + 10;
	}

	private int simplifySliderW() {
		return cardW() - 20 - 60;
	}

	private int simplifySliderY() {
		return card1Y() + CARD1_H - 22;
	}

	private void renderToggle(GuiGraphics g, int x, int y, boolean on, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + 40 && mouseY >= y && mouseY < y + 16;
		g.fill(x, y, x + 40, y + 16, on ? (hover ? 0xFFF04A60 : ACCENT) : (hover ? 0xFF2E2E3A : 0xFF24242E));
		g.drawCenteredString(this.font, on ? "ВКЛ" : "ВЫКЛ", x + 20, y + 4, on ? TEXT : TEXT_DIM);
	}

	private void renderCardText(GuiGraphics g, String text, int x, int y, int width) {
		int yy = y;
		for (var line : this.font.split(Component.literal(text), width)) {
			g.drawString(this.font, line, x, yy, TEXT_DIM, false);
			yy += 10;
		}
	}

	private void renderSmartTab(GuiGraphics g, int mouseX, int mouseY) {
		int x = cardX(), w = cardW();

		// Карточка 1: упрощение далёких мобов
		int y1 = card1Y();
		g.fill(x, y1, x + w, y1 + CARD1_H, ROW);
		g.fill(x, y1, x + 3, y1 + CARD1_H, MobConfig.simplifyFar ? ACCENT : PANEL_BORDER);
		g.drawString(this.font, Component.literal("Упрощённые далёкие мобы").withStyle(s -> s.withBold(true)),
				x + 10, y1 + 9, TEXT, false);
		renderToggle(g, toggleX(), y1 + 6, MobConfig.simplifyFar, mouseX, mouseY);
		renderCardText(g, "У мобов вдали не рисуются броня, предметы в руках и ник. Сам моб виден, но отрисовать его дешевле.",
				x + 10, y1 + 28, w - 20);

		int sx = simplifySliderX(), sw = simplifySliderW(), sy = simplifySliderY();
		g.drawString(this.font, "Считать далёким от:", x + 10, sy - 12, TEXT_DIM, false);
		g.fill(sx, sy + 4, sx + sw, sy + 8, TRACK);
		double t = (MobConfig.simplifyDistance - MobConfig.SIMPLIFY_MIN)
				/ (double) (MobConfig.SIMPLIFY_MAX - MobConfig.SIMPLIFY_MIN);
		int filled = (int) (sw * t);
		g.fill(sx, sy + 4, sx + filled, sy + 8, MobConfig.simplifyFar ? ACCENT : ACCENT_DARK);
		g.fill(sx + filled - 2, sy + 1, sx + filled + 2, sy + 11, TEXT);
		g.drawString(this.font, MobConfig.simplifyDistance + " бл.", sx + sw + 10, sy + 2, TEXT, false);

		// Карточка 2: скрытие за стенами
		int y2 = card2Y();
		g.fill(x, y2, x + w, y2 + CARD2_H, ROW);
		g.fill(x, y2, x + 3, y2 + CARD2_H, MobConfig.occlusionCull ? ACCENT : PANEL_BORDER);
		g.drawString(this.font, Component.literal("Не рисовать мобов за стенами").withStyle(s -> s.withBold(true)),
				x + 10, y2 + 9, TEXT, false);
		renderToggle(g, toggleX(), y2 + 6, MobConfig.occlusionCull, mouseX, mouseY);
		renderCardText(g, "Мобы в пещерах и за постройками не отрисовываются, пока их не видно. "
						+ "Пропадают с задержкой 0,5 сек, чтобы не выскакивать при повороте. Игроки и боссы всегда видны.",
				x + 10, y2 + 28, w - 20);
	}

	private void applySimplifySlider(double mx) {
		double t = Mth.clamp((mx - simplifySliderX()) / simplifySliderW(), 0.0, 1.0);
		MobConfig.simplifyDistance = MobConfig.SIMPLIFY_MIN
				+ (int) Math.round(t * (MobConfig.SIMPLIFY_MAX - MobConfig.SIMPLIFY_MIN));
		changed = true;
	}

	private void renderMobsTab(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// подсказка + кнопка сброса
		g.drawString(this.font, "Дальность: 0 = не видно, " + MobConfig.MAX + " = без лимита",
				px + 12, py + TAB_H + 9, TEXT_DIM, false);

		// кнопка "Конфиги" справа в строке подсказки
		int cbx = px + PANEL_W - 12 - 62;
		int cby = py + TAB_H + 5;
		boolean ch = mouseX >= cbx && mouseX < cbx + 62 && mouseY >= cby && mouseY < cby + 16;
		g.fill(cbx, cby, cbx + 62, cby + 16, ch ? 0xFF2E2E3A : 0xFF24242E);
		g.drawCenteredString(this.font, "Конфиги", cbx + 31, cby + 4, TEXT);

		// кнопки "Отключить всё" и "Сбросить"
		int rbx = px + PANEL_W - 12 - 62;
		int rby = py + TAB_H + 28;
		int dbx = rbx - 4 - 62;
		boolean rh = mouseX >= rbx && mouseX < rbx + 62 && mouseY >= rby && mouseY < rby + 16;
		boolean dh = mouseX >= dbx && mouseX < dbx + 62 && mouseY >= rby && mouseY < rby + 16;
		g.fill(dbx, rby, dbx + 62, rby + 16, dh ? ACCENT : ACCENT_DARK);
		g.drawCenteredString(this.font, "Откл. всё", dbx + 31, rby + 4, TEXT);
		g.fill(rbx, rby, rbx + 62, rby + 16, rh ? 0xFF2E2E3A : 0xFF24242E);
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
		if (maxScroll() > 0) {
			int sbx = scrollbarX();
			int ty = thumbY();
			int th = thumbH();
			boolean overThumb = mouseX >= sbx - 4 && mouseX < sbx + SCROLLBAR_W + 2
					&& mouseY >= ty && mouseY < ty + th;
			g.fill(sbx, ly, sbx + SCROLLBAR_W, ly + lh, TRACK);
			g.fill(sbx, ty, sbx + SCROLLBAR_W, ty + th,
					(draggingScrollbar || overThumb) ? 0xFFF04A60 : ACCENT);
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
				switchTab(idx);
				return true;
			}

			if (tab == 1) {
				// переключатели
				int tx = toggleX();
				if (mx >= tx && mx < tx + 40) {
					if (my >= card1Y() + 6 && my < card1Y() + 22) {
						MobConfig.simplifyFar = !MobConfig.simplifyFar;
						changed = true;
						return true;
					}
					if (my >= card2Y() + 6 && my < card2Y() + 22) {
						MobConfig.occlusionCull = !MobConfig.occlusionCull;
						changed = true;
						return true;
					}
				}
				// ползунок дистанции
				int sx = simplifySliderX(), sw = simplifySliderW(), sy = simplifySliderY();
				if (mx >= sx - 6 && mx <= sx + sw + 6 && my >= sy - 2 && my <= sy + 14) {
					draggingSimplify = true;
					applySimplifySlider(mx);
					return true;
				}
			}

			if (tab == 2) {
				// кнопка "Конфиги"
				int cbx = px + PANEL_W - 12 - 62;
				int cby = py + TAB_H + 5;
				if (mx >= cbx && mx < cbx + 62 && my >= cby && my < cby + 16) {
					if (changed) {
						MobConfig.save();
						changed = false;
					}
					this.minecraft.setScreen(new ConfigsScreen(this));
					return true;
				}

				// "Откл. всё" и "Сбросить"
				int rbx = px + PANEL_W - 12 - 62;
				int rby = py + TAB_H + 28;
				int dbx = rbx - 4 - 62;
				if (my >= rby && my < rby + 16) {
					if (mx >= rbx && mx < rbx + 62) {
						MobConfig.resetAll();
						changed = true;
						return true;
					}
					if (mx >= dbx && mx < dbx + 62) {
						List<net.minecraft.world.entity.EntityType<?>> types = new ArrayList<>();
						for (MobCatalog.Mob mob : allMobs) {
							types.add(mob.type());
						}
						MobConfig.disableAll(types);
						changed = true;
						return true;
					}
				}

				// полоса прокрутки: тянем ползунок или кликаем по дорожке
				if (maxScroll() > 0) {
					int sbx = scrollbarX();
					if (mx >= sbx - 4 && mx < sbx + SCROLLBAR_W + 2 && my >= listY() && my < listY() + listH()) {
						int ty = thumbY();
						int th = thumbH();
						if (my >= ty && my < ty + th) {
							scrollGrabOffset = my - ty;
						} else {
							scrollGrabOffset = th / 2.0;
							scrollToThumbTop(my - scrollGrabOffset);
						}
						draggingScrollbar = true;
						return true;
					}
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
		if (draggingScrollbar) {
			scrollToThumbTop(event.y() - scrollGrabOffset);
			return true;
		}
		if (draggingSimplify) {
			applySimplifySlider(event.x());
			return true;
		}
		if (dragging != null) {
			applySlider(dragging, event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		if (draggingSimplify) {
			draggingSimplify = false;
			return true;
		}
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
		// Клавиша открытия (та, что выбрана в настройках управления) закрывает меню,
		// но только когда не печатаем в поиске
		int bound = KeyBindingHelper.getBoundKeyOf(FpsBoostClient.openMenuKey()).getValue();
		if (event.key() == bound && !search.isFocused()) {
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
