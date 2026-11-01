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
 * Меню мода (клавиша H): 4 вкладки. Вкладка 1 пока показывает "Скоро...",
 * вкладка 2 "Умная отрисовка", вкладка 3 "Оптимизация мобов" (ползунки дальности),
 * вкладка 4 "Частицы и мир" (убрать любую частицу, небо, облака, погоду).
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

	private static final String[] TABS = {"Вкладка 1", "Умная отрисовка", "Оптимизация мобов", "Частицы и мир"};

	private static final int PANEL_W = 460;
	private static final int PANEL_H = 264;
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

	// Вкладка 4 "Частицы и мир"
	private EditBox pSearch;
	private final List<ParticleCatalog.Entry> pItems = new ArrayList<>();
	private double pScroll = 0;
	private boolean pDraggingScrollbar = false;
	private double pGrabOffset = 0;

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

		pSearch = new EditBox(this.font, px + 12, pRowY(), PANEL_W - 24 - 144 - 6, 16, Component.literal("Поиск частицы"));
		pSearch.setHint(Component.literal("Поиск частицы..."));
		pSearch.setMaxLength(32);
		pSearch.setResponder(s -> {
			pScroll = 0;
			rebuildParticles();
		});
		this.addRenderableWidget(pSearch);
		pSearch.visible = tab == 3;
		rebuildParticles();
	}

	private void switchTab(int idx) {
		tab = idx;
		search.setFocused(false);
		search.visible = tab == 2;
		pSearch.setFocused(false);
		pSearch.visible = tab == 3;
		dragging = null;
		draggingSimplify = false;
		draggingScrollbar = false;
		pDraggingScrollbar = false;
	}

	private void rebuildParticles() {
		pItems.clear();
		String q = pSearch == null ? "" : pSearch.getValue().trim().toLowerCase(Locale.ROOT);
		for (ParticleCatalog.Entry e : ParticleCatalog.get()) {
			if (q.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(q)
					|| e.id().toLowerCase(Locale.ROOT).contains(q)) {
				pItems.add(e);
			}
		}
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
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

		if (tab == 3) {
			renderParticlesTab(g, mouseX, mouseY, delta);
		} else if (tab == 2) {
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

	// ---------- вкладка 4: Частицы и мир ----------

	private static final int PROW_H = 20;
	private static final int CHIP_H = 26;
	private static final String[] WORLD_LABELS = {"Небо", "Светила и звёзды", "Облака", "Дождь и снег"};

	private int chipsY() {
		return py + TAB_H + 8;
	}

	private int chipW() {
		return (listW() - 18) / 4;
	}

	private int chipX(int i) {
		return listX() + i * (chipW() + 6);
	}

	private int pRowY() {
		return chipsY() + CHIP_H + 8;
	}

	private int blockRowY() {
		return pRowY() + 22;
	}

	private int blockToggleX() {
		return listX() + listW() - 8 - 40;
	}

	private int blockExceptionsX() {
		return blockToggleX() - 4 - 120;
	}

	private int pListY() {
		return blockRowY() + 28;
	}

	private int pListH() {
		return py + PANEL_H - pListY() - 8;
	}

	private double pMaxScroll() {
		return Math.max(0, pItems.size() * PROW_H - pListH());
	}

	private int pThumbH() {
		int content = Math.max(1, pItems.size() * PROW_H);
		return Math.max(20, (int) (pListH() * (pListH() / (double) content)));
	}

	private int pThumbY() {
		double max = pMaxScroll();
		if (max <= 0) {
			return pListY();
		}
		return pListY() + (int) ((pListH() - pThumbH()) * (pScroll / max));
	}

	private void pScrollToThumbTop(double top) {
		double track = pListH() - pThumbH();
		if (track <= 0) {
			return;
		}
		pScroll = Mth.clamp((top - pListY()) / track, 0.0, 1.0) * pMaxScroll();
	}

	private boolean worldFlag(int i) {
		return switch (i) {
			case 0 -> MobConfig.hideSky;
			case 1 -> MobConfig.hideSunMoonStars;
			case 2 -> MobConfig.hideClouds;
			default -> MobConfig.hideWeather;
		};
	}

	private void toggleWorldFlag(int i) {
		switch (i) {
			case 0 -> MobConfig.hideSky = !MobConfig.hideSky;
			case 1 -> MobConfig.hideSunMoonStars = !MobConfig.hideSunMoonStars;
			case 2 -> MobConfig.hideClouds = !MobConfig.hideClouds;
			default -> MobConfig.hideWeather = !MobConfig.hideWeather;
		}
		changed = true;
	}

	private void renderParticlesTab(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// переключатели мира
		for (int i = 0; i < WORLD_LABELS.length; i++) {
			int x = chipX(i), y = chipsY(), w = chipW();
			boolean off = worldFlag(i);
			boolean hover = inside(mouseX, mouseY, x, y, w, CHIP_H);
			g.fill(x, y, x + w, y + CHIP_H, hover ? ROW_HOVER : ROW);
			g.fill(x, y, x + 3, y + CHIP_H, off ? ACCENT : PANEL_BORDER);
			g.drawString(this.font, WORLD_LABELS[i], x + 8, y + 5, TEXT, false);
			g.drawString(this.font, off ? "убрано" : "есть", x + 8, y + 15, off ? ACCENT : TEXT_DIM, false);
		}

		// поиск и кнопки
		pSearch.render(g, mouseX, mouseY, delta);
		int by = pRowY();
		int b2 = px + PANEL_W - 12 - 70;
		int b1 = b2 - 4 - 70;
		g.fill(b1, by, b1 + 70, by + 16, inside(mouseX, mouseY, b1, by, 70, 16) ? ACCENT : ACCENT_DARK);
		g.drawCenteredString(this.font, "Откл. все", b1 + 35, by + 4, TEXT);
		g.fill(b2, by, b2 + 70, by + 16, inside(mouseX, mouseY, b2, by, 70, 16) ? 0xFF2E2E3A : 0xFF24242E);
		g.drawCenteredString(this.font, "Вкл. все", b2 + 35, by + 4, TEXT);

		// строка "Частицы блоков": одна кнопка на все частицы от блоков + исключения
		int brx = listX(), bry = blockRowY(), brw = listW();
		g.fill(brx, bry, brx + brw, bry + 22, ROW);
		g.fill(brx, bry, brx + 3, bry + 22, MobConfig.hideBlockParticles ? ACCENT : PANEL_BORDER);
		g.drawString(this.font, Component.literal("Частицы блоков").withStyle(s -> s.withBold(true)),
				brx + 8, bry + 7, TEXT, false);
		g.drawString(this.font, "(ломание, удары, бег)", brx + 108, bry + 7, TEXT_DIM, false);
		int exX = blockExceptionsX(), tgX = blockToggleX();
		boolean exHover = inside(mouseX, mouseY, exX, bry + 3, 120, 16);
		g.fill(exX, bry + 3, exX + 120, bry + 19, exHover ? 0xFF2E2E3A : 0xFF24242E);
		g.drawCenteredString(this.font, "Исключения (" + MobConfig.blockExceptionCount() + ")",
				exX + 60, bry + 7, TEXT);
		boolean blocksOff = MobConfig.hideBlockParticles;
		boolean tgHover = inside(mouseX, mouseY, tgX, bry + 3, 40, 16);
		g.fill(tgX, bry + 3, tgX + 40, bry + 19,
				blocksOff ? (tgHover ? 0xFFF04A60 : ACCENT) : (tgHover ? 0xFF2E5236 : 0xFF24402C));
		g.drawCenteredString(this.font, blocksOff ? "ВЫКЛ" : "ВКЛ", tgX + 20, bry + 7,
				blocksOff ? TEXT : 0xFF7DDC8C);

		// список частиц
		int lx = listX(), ly = pListY(), lw = listW(), lh = pListH();
		int rowW = lw - SCROLLBAR_W - 4;
		g.enableScissor(lx, ly, lx + lw, ly + lh);
		int y = ly - (int) pScroll;
		for (ParticleCatalog.Entry e : pItems) {
			if (y + PROW_H >= ly && y <= ly + lh) {
				boolean hidden = MobConfig.isParticleHidden(e.type());
				boolean hover = mouseY >= ly && mouseY < ly + lh && inside(mouseX, mouseY, lx, y, rowW, PROW_H - 2);
				g.fill(lx, y, lx + rowW, y + PROW_H - 2, hover ? ROW_HOVER : ROW);
				g.drawString(this.font, e.name(), lx + 8, y + 5, hidden ? TEXT_DIM : TEXT, false);
				g.drawString(this.font, e.id(), lx + 190, y + 5, 0xFF5C5C6C, false);
				int tx = lx + rowW - 8 - 40;
				g.fill(tx, y + 1, tx + 40, y + PROW_H - 3, hidden ? ACCENT_DARK : 0xFF24402C);
				g.drawCenteredString(this.font, hidden ? "ВЫКЛ" : "ВКЛ", tx + 20, y + 5,
						hidden ? TEXT : 0xFF7DDC8C);
			}
			y += PROW_H;
		}
		g.disableScissor();

		if (pItems.isEmpty()) {
			g.drawCenteredString(this.font, "Ничего не найдено", px + PANEL_W / 2, ly + 20, TEXT_DIM);
		}

		if (pMaxScroll() > 0) {
			int sbx = lx + lw - SCROLLBAR_W;
			int ty = pThumbY();
			int th = pThumbH();
			boolean overThumb = inside(mouseX, mouseY, sbx - 4, ty, SCROLLBAR_W + 6, th);
			g.fill(sbx, ly, sbx + SCROLLBAR_W, ly + lh, TRACK);
			g.fill(sbx, ty, sbx + SCROLLBAR_W, ty + th, (pDraggingScrollbar || overThumb) ? 0xFFF04A60 : ACCENT);
		}
	}

	private ParticleCatalog.Entry particleAt(double mx, double my) {
		int rowW = listW() - SCROLLBAR_W - 4;
		if (!inside(mx, my, listX(), pListY(), rowW, pListH())) {
			return null;
		}
		int idx = (int) ((my - pListY() + pScroll) / PROW_H);
		return idx >= 0 && idx < pItems.size() ? pItems.get(idx) : null;
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

			if (tab == 3) {
				// переключатели мира
				for (int i = 0; i < WORLD_LABELS.length; i++) {
					if (inside(mx, my, chipX(i), chipsY(), chipW(), CHIP_H)) {
						toggleWorldFlag(i);
						return true;
					}
				}

				// "Откл. все" и "Вкл. все"
				int by = pRowY();
				int b2 = px + PANEL_W - 12 - 70;
				int b1 = b2 - 4 - 70;
				if (inside(mx, my, b1, by, 70, 16)) {
					List<net.minecraft.core.particles.ParticleType<?>> types = new ArrayList<>();
					for (ParticleCatalog.Entry e : ParticleCatalog.get()) {
						types.add(e.type());
					}
					MobConfig.hideAllParticles(types);
					MobConfig.hideBlockParticles = true;
					changed = true;
					return true;
				}
				if (inside(mx, my, b2, by, 70, 16)) {
					MobConfig.showAllParticles();
					MobConfig.hideBlockParticles = false;
					changed = true;
					return true;
				}

				// строка "Частицы блоков"
				int bry = blockRowY();
				if (inside(mx, my, blockToggleX(), bry + 3, 40, 16)) {
					MobConfig.hideBlockParticles = !MobConfig.hideBlockParticles;
					changed = true;
					return true;
				}
				if (inside(mx, my, blockExceptionsX(), bry + 3, 120, 16)) {
					if (changed) {
						MobConfig.save();
						changed = false;
					}
					this.minecraft.setScreen(new BlockExceptionsScreen(this));
					return true;
				}

				// полоса прокрутки списка частиц
				if (pMaxScroll() > 0) {
					int sbx = listX() + listW() - SCROLLBAR_W;
					if (inside(mx, my, sbx - 4, pListY(), SCROLLBAR_W + 6, pListH())) {
						int ty = pThumbY();
						int th = pThumbH();
						if (my >= ty && my < ty + th) {
							pGrabOffset = my - ty;
						} else {
							pGrabOffset = th / 2.0;
							pScrollToThumbTop(my - pGrabOffset);
						}
						pDraggingScrollbar = true;
						return true;
					}
				}

				// клик по частице: убрать или вернуть
				ParticleCatalog.Entry hitParticle = particleAt(mx, my);
				if (hitParticle != null) {
					MobConfig.setParticleHidden(hitParticle.type(), !MobConfig.isParticleHidden(hitParticle.type()));
					changed = true;
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
		if (pDraggingScrollbar) {
			pScrollToThumbTop(event.y() - pGrabOffset);
			return true;
		}
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
		if (pDraggingScrollbar) {
			pDraggingScrollbar = false;
			return true;
		}
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
		if (tab == 3) {
			pScroll = Mth.clamp(pScroll - scrollY * 18, 0, pMaxScroll());
			return true;
		}
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
		if (event.key() == bound && !search.isFocused() && !pSearch.isFocused()) {
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
