package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Mapa de sectores y galaxias: muestra la posición actual, los destinos
 * descubiertos y el coste estimado del warp. Los sectores no descubiertos
 * aparecen como incógnitas y no se pueden seleccionar. No usa imágenes ni red.
 */
final class MapScreen {

    private static final Color PANEL_C = new Color(0.030f, 0.042f, 0.058f, 0.95f);
    private static final Color CARD_C = new Color(0.060f, 0.080f, 0.100f, 0.94f);
    private static final Color CARD_SEL_C = new Color(0.110f, 0.115f, 0.100f, 0.96f);
    private static final Color CARD_CURRENT_C = new Color(0.075f, 0.110f, 0.090f, 0.96f);
    private static final Color CARD_EDGE_C = new Color(0.24f, 0.33f, 0.40f, 0.50f);
    private static final Color DECK_C = new Color(0.035f, 0.048f, 0.062f, 0.92f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Array<Button> rows = new Array<>();
    private Button btnTravel, btnBack;
    private int selected;
    private String message = "";
    private float px, py, pw, ph, listTop, rowH, rowGap;

    MapScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes,
              SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
    }

    void enter() {
        message = "";
        selected = -1;
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            SectorDef sector = sectorAt(i);
            if (sector != null && !sector.id.equals(game.session.currentSector.id)
                    && game.session.universe.isDiscovered(sector.id)) {
                selected = i;
                break;
            }
        }
        layout();
    }

    void layout() {
        layout(game.hudView.getWorldWidth(), game.hudView.getWorldHeight());
    }

    void layout(float w, float h) {
        float u = game.u;
        float pad = 16f * u;
        px = pad * 0.5f;
        py = pad * 0.5f;
        pw = w - pad;
        ph = h - pad;

        listTop = h - 108f * u;
        float bottom = py + 220f * u;
        rowGap = 6f * u;
        rowH = (listTop - bottom - 5 * rowGap) / 6f;
        rowH = Math.max(34f * u, Math.min(200f * u, rowH));

        rows.clear();
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            float top = listTop - i * (rowH + rowGap);
            rows.add(new Button("", px + 20f * u, top - rowH,
                    pw - 40f * u, rowH));
        }
        float halfW = (pw - 40f * u - 8f * u) * 0.5f;
        btnTravel = new Button("VIAJAR", px + 20f * u, py + 86f * u, halfW, 48f * u);
        btnBack = new Button("VOLVER", px + 20f * u + halfW + 8f * u,
                py + 86f * u, halfW, 48f * u);
    }

    int selectedIndex() {
        return selected;
    }

    String selectedSectorId() {
        SectorDef sector = sectorAt(selected);
        return sector == null ? "" : sector.id;
    }

    int discoveredCount() {
        return game.session.universe.discoveredCount();
    }

    int hiddenCount() {
        return game.session.universe.sectorCount() - discoveredCount();
    }

    int sectorsCount() {
        return game.session.universe.sectorCount();
    }

    String message() {
        return message;
    }

    private SectorDef sectorAt(int index) {
        int i = 0;
        for (GalaxyDef galaxy : game.session.universe.galaxies()) {
            for (SystemDef system : galaxy.systems) {
                for (SectorDef sector : system.sectors) {
                    if (i == index) {
                        return sector;
                    }
                    i++;
                }
            }
        }
        return null;
    }

    private GalaxyDef galaxyOf(SectorDef sector) {
        return sector == null ? null
                : game.session.universe.galaxy(galaxyIdOf(sector));
    }

    private String galaxyIdOf(SectorDef sector) {
        SystemDef system = sector == null ? null
                : game.session.universe.system(sector.systemId);
        return system == null ? "" : system.galaxyId;
    }

    private SystemDef systemOf(SectorDef sector) {
        return sector == null ? null : game.session.universe.system(sector.systemId);
    }

    boolean handle() {
        if (btnBack == null) {
            layout();
        }
        if (game.tapped(btnBack)) {
            game.closeMap();
            return true;
        }
        for (int i = 0; i < rows.size; i++) {
            if (game.tapped(rows.get(i))) {
                SectorDef sector = sectorAt(i);
                if (sector != null
                        && game.session.universe.isDiscovered(sector.id)) {
                    selected = i;
                    message = "";
                } else {
                    message = "SECTOR NO DESCUBIERTO";
                }
                return true;
            }
        }
        if (game.tapped(btnTravel)) {
            travel();
            return true;
        }
        return false;
    }

    private void travel() {
        SectorDef sector = sectorAt(selected);
        if (sector == null) {
            message = "ELIGE UN DESTINO";
            return;
        }
        if (sector.id.equals(game.session.currentSector.id)) {
            message = "YA ESTAS EN ESTE SECTOR";
            return;
        }
        if (!game.session.universe.isDiscovered(sector.id)) {
            message = "SECTOR NO DESCUBIERTO";
            return;
        }
        TravelService.Quote quote = TravelService.quote(game.session, sector.id);
        if (!quote.ok) {
            message = quote.error;
            return;
        }
        if (game.travelTo(sector.id)) {
            game.closeMap();
        } else {
            message = "NO SE PUDO VIAJAR";
        }
    }

    void draw(float w, float h) {
        if (btnBack == null) {
            layout(w, h);
        }
        float u = game.u;
        float camX = game.time * 9f * u;
        float camY = game.time * 4f * u;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();
        batch.begin();
        Visuals.additive(batch);
        space.stars(w * 0.5f + camX, h * 0.5f + camY, w, h, u, 0.8f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        ui.grid(w, h, 80f * u, 0.018f);
        ui.frame(px, py, pw, ph, 14f * u, PANEL_C, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.divider(px + 22f * u, h - 92f * u, pw - 44f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.panel(px + 14f * u, py + 56f * u, pw - 28f * u, 158f * u,
                8f * u, DECK_C, Visuals.PANEL_EDGE);
        for (int i = 0; i < rows.size; i++) {
            Button row = rows.get(i);
            SectorDef sector = sectorAt(i);
            boolean current = sector != null
                    && sector.id.equals(game.session.currentSector.id);
            boolean discovered = sector != null
                    && game.session.universe.isDiscovered(sector.id);
            Color fill = i == selected ? CARD_SEL_C
                    : current ? CARD_CURRENT_C : CARD_C;
            ui.panel(row.bounds.x - 4f * u, row.bounds.y - 4f * u,
                    row.bounds.width + 8f * u, row.bounds.height + 8f * u,
                    7f * u, fill,
                    i == selected ? Visuals.ACCENT : CARD_EDGE_C);
            float mid = row.bounds.y + row.bounds.height * 0.5f;
            ui.led(row.bounds.x + 16f * u, mid, 3.2f * u,
                    current ? Visuals.GOOD
                            : discovered ? (i == selected ? Visuals.ACCENT : Visuals.INFO)
                            : Visuals.LED_OFF);
            drawSectorIcon(row.bounds.x + 42f * u, mid, 11f * u,
                    current, discovered);
            if (i == selected) {
                shapes.setColor(Visuals.ACCENT);
                shapes.rect(row.bounds.x + 2f * u, row.bounds.y + 10f * u,
                        3f * u, row.bounds.height - 20f * u);
            }
        }
        SectorDef chosen = sectorAt(selected);
        boolean canTravel = chosen != null
                && !chosen.id.equals(game.session.currentSector.id)
                && game.session.universe.isDiscovered(chosen.id);
        ui.drawButton(btnTravel, Visuals.BTN_PRIMARY, canTravel);
        ui.drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        ui.drawWarpIcon(btnTravel.bounds.x + 22f * u,
                btnTravel.bounds.y + btnTravel.bounds.height * 0.5f,
                8f * u, canTravel ? Visuals.ACCENT : Visuals.TEXT_OFF);
        shapes.end();

        batch.begin();
        ui.textLeft("MAPA DE SECTORES", px + 24f * u, h - 32f * u,
                0.94f * u, Visuals.TEXT);
        GalaxyDef currentGalaxy = galaxyOf(game.session.currentSector);
        SystemDef currentSystem = systemOf(game.session.currentSector);
        ui.textLeftFit("ACTUAL  "
                        + (currentGalaxy == null ? "?" : currentGalaxy.name)
                        + " / " + (currentSystem == null ? "?" : currentSystem.name)
                        + " / " + (game.session.currentSector == null ? "?"
                        : game.session.currentSector.name),
                px + 24f * u, h - 58f * u, 0.66f * u, pw - 48f * u,
                Visuals.TEXT_DIM);
        ui.textLeft("DESCUBIERTOS " + discoveredCount() + "/" + sectorsCount(),
                px + 24f * u, h - 80f * u, 0.60f * u, Visuals.TEXT_OFF);
        drawRows();
        drawFooter();
        batch.end();
    }

    private void drawRows() {
        float u = game.u;
        for (int i = 0; i < rows.size; i++) {
            SectorDef sector = sectorAt(i);
            Button row = rows.get(i);
            boolean discovered = sector != null
                    && game.session.universe.isDiscovered(sector.id);
            boolean current = sector != null
                    && sector.id.equals(game.session.currentSector.id);
            float x = row.bounds.x + 76f * u;
            float mid = row.bounds.y + row.bounds.height * 0.5f;
            float titleY = mid + 18f * u;
            float descY = mid - 10f * u;
            String title;
            String desc;
            Color color;
            if (!discovered) {
                title = "???   SECTOR DESCONOCIDO";
                desc = "SIN DATOS DE NAVEGACION";
                color = Visuals.TEXT_OFF;
            } else {
                GalaxyDef galaxy = galaxyOf(sector);
                SystemDef system = systemOf(sector);
                title = (current ? "AQUI   " : "") + sector.name;
                desc = (galaxy == null ? "?" : galaxy.name) + " / "
                        + (system == null ? "?" : system.name);
                color = current ? Visuals.GOOD : Visuals.TEXT;
            }
            ui.textLeftFit(title, x, titleY, 0.86f * u,
                    row.bounds.width * 0.52f, color);
            ui.textLeftFit(desc, x, descY, 0.62f * u,
                    row.bounds.width * 0.52f, Visuals.TEXT_DIM);
            if (discovered && !current && sector != null) {
                TravelService.Quote quote = TravelService.quote(
                        game.session, sector.id);
                if (quote.ok || quote.warpLevel > 0) {
                    ui.textRight(String.format(java.util.Locale.US,
                                    "%.1f u", quote.distance),
                            row.bounds.x + row.bounds.width - 22f * u,
                            titleY, 0.72f * u, Visuals.TEXT);
                    ui.textRight("C" + quote.fuelCost + "  E" + quote.energyCost,
                            row.bounds.x + row.bounds.width - 22f * u,
                            descY, 0.60f * u,
                            quote.ok ? Visuals.ACCENT : Visuals.WARN);
                }
            }
        }
    }

    private void drawFooter() {
        float u = game.u;
        SectorDef sector = sectorAt(selected);
        TravelService.Quote quote = sector == null ? null
                : TravelService.quote(game.session, sector.id);
        String info;
        if (sector == null) {
            info = "ELIGE UN DESTINO";
        } else if (!game.session.universe.isDiscovered(sector.id)) {
            info = "SECTOR NO DESCUBIERTO";
        } else if (sector.id.equals(game.session.currentSector.id)) {
            info = "ESTAS EN " + sector.name;
        } else if (quote != null && quote.ok) {
            info = "DESTINO " + sector.name + "   " + String.format(
                    java.util.Locale.US, "%.1f u   C%d E%d", quote.distance,
                    quote.fuelCost, quote.energyCost);
        } else {
            info = quote == null || quote.error.isEmpty()
                    ? "SIN COSTE" : quote.error;
        }
        ui.textCenteredFit(info, px + pw * 0.5f, py + 178f * u,
                0.68f * u, pw - 40f * u, Visuals.TEXT);
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, px + pw * 0.5f, py + 152f * u,
                    0.64f * u, pw - 40f * u, Visuals.WARN);
        } else {
            ui.textCentered("EL WARP NO NECESITA PUERTAS",
                    px + pw * 0.5f, py + 152f * u, 0.56f * u,
                    Visuals.TEXT_OFF);
        }
        ui.drawButtonText(btnTravel, true);
        ui.drawButtonText(btnBack, true);
    }

    private void drawSectorIcon(float cx, float cy, float r,
                                boolean current, boolean discovered) {
        float u = game.u;
        if (current) {
            ui.drawStationIcon(cx, cy, r, Visuals.GOOD);
        } else if (discovered) {
            ui.drawWarpIcon(cx, cy, r, Visuals.INFO);
        } else {
            shapes.setColor(Visuals.TEXT_OFF.r, Visuals.TEXT_OFF.g,
                    Visuals.TEXT_OFF.b, 0.65f);
            shapes.rect(cx - r, cy - r, r * 2f, 2f * u);
            shapes.rect(cx - r, cy + r - 2f * u, r * 2f, 2f * u);
            shapes.rect(cx - r, cy - r, 2f * u, r * 2f);
            shapes.rect(cx + r - 2f * u, cy - r, 2f * u, r * 2f);
        }
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
