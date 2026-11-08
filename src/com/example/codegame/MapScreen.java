package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Mapa de navegación (UPD-006-T16/T17): representación espacial de galaxias,
 * sistemas y sectores con sus posiciones reales, descubrimiento, ruta y
 * presupuesto de viaje en modo warp o convencional. No revela lo no descubierto
 * ni inventa puntos de interés.
 */
final class MapScreen {

    private static final Color PANEL_C = new Color(0.030f, 0.042f, 0.058f, 0.95f);
    private static final Color DECK_C = new Color(0.022f, 0.034f, 0.048f, 0.94f);
    private static final Color EDGE_C = new Color(0.20f, 0.34f, 0.44f, 0.42f);
    private static final Color ROUTE_C = new Color(0.35f, 0.90f, 1f, 0.75f);
    private static final Color GALAXY_C = new Color(0.42f, 0.56f, 0.90f, 0.45f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private Button btnTravel, btnBack, btnMode, btnLocal, btnZoomIn, btnZoomOut;
    private int selected = -1;
    private String message = "";
    private TravelPlan.Mode mode = TravelPlan.Mode.WARP;
    private float px, py, pw, ph, mapX, mapY, mapW, mapH;
    private float zoom = 1f;
    private float panX, panY;
    private float scale = 1f;
    private float centerMapX, centerMapY;
    private final Vector2 tmp = new Vector2();
    private int dragPointer = -1;
    private float lastDragX, lastDragY;

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
        zoom = 1f;
        panX = 0f;
        panY = 0f;
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
        mapX = px + 16f * u;
        mapY = py + 236f * u;
        mapW = pw - 32f * u;
        mapH = h - 104f * u - mapY;
        float halfW = (pw - 40f * u - 8f * u) * 0.5f;
        btnTravel = new Button("VIAJAR", px + 20f * u, py + 86f * u, halfW, 48f * u);
        btnBack = new Button("VOLVER", px + 20f * u + halfW + 8f * u,
                py + 86f * u, halfW, 48f * u);
        float localW = Math.min(120f * u, pw * 0.28f);
        btnLocal = new Button("LOCAL", px + pw - localW - 22f * u,
                h - 74f * u, localW, 40f * u);
        btnMode = new Button("WARP", px + 24f * u, py + 190f * u,
                Math.min(160f * u, pw * 0.40f), 38f * u);
        btnZoomIn = new Button("+", px + pw - 76f * u, py + 190f * u,
                42f * u, 38f * u);
        btnZoomOut = new Button("-", px + pw - 76f * u + 46f * u,
                py + 190f * u, 42f * u, 38f * u);
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

    TravelPlan.Mode mode() {
        return mode;
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
        if (game.tapped(btnLocal)) {
            game.openLocalMap();
            return true;
        }
        if (game.tapped(btnMode)) {
            mode = mode == TravelPlan.Mode.WARP
                    ? TravelPlan.Mode.CONVENTIONAL : TravelPlan.Mode.WARP;
            btnMode.label = mode == TravelPlan.Mode.WARP ? "WARP" : "CONVENCIONAL";
            message = "";
            return true;
        }
        if (game.tapped(btnZoomIn)) {
            zoom = Math.min(4f, zoom * 1.3f);
            return true;
        }
        if (game.tapped(btnZoomOut)) {
            zoom = Math.max(0.6f, zoom / 1.3f);
            return true;
        }
        handleDrag();
        if (Gdx.input.justTouched()) {
            float sx = Gdx.input.getX();
            float sy = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (sx >= mapX && sx <= mapX + mapW && sy >= mapY && sy <= mapY + mapH) {
                selectNode(sx, sy);
                return true;
            }
        }
        if (game.tapped(btnTravel)) {
            travel();
            return true;
        }
        return false;
    }

    private void handleDrag() {
        if (dragPointer >= 0 && !Gdx.input.isTouched(dragPointer)) {
            dragPointer = -1;
        }
        if (dragPointer < 0) {
            for (int i = 0; i < 5; i++) {
                if (Gdx.input.isTouched(i)) {
                    float x = Gdx.input.getX(i);
                    float y = Gdx.graphics.getHeight() - Gdx.input.getY(i);
                    if (x >= mapX && x <= mapX + mapW
                            && y >= mapY && y <= mapY + mapH) {
                        dragPointer = i;
                        lastDragX = x;
                        lastDragY = y;
                        break;
                    }
                }
            }
            return;
        }
        float x = Gdx.input.getX(dragPointer);
        float y = Gdx.graphics.getHeight() - Gdx.input.getY(dragPointer);
        panX += x - lastDragX;
        panY += y - lastDragY;
        lastDragX = x;
        lastDragY = y;
    }

    private void computeProjection() {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            SectorDef sector = sectorAt(i);
            if (sector == null || !game.session.universe.isDiscovered(sector.id)) {
                continue;
            }
            minX = Math.min(minX, sector.mapX);
            maxX = Math.max(maxX, sector.mapX);
            minY = Math.min(minY, sector.mapY);
            maxY = Math.max(maxY, sector.mapY);
        }
        if (minX > maxX) {
            minX = 0f;
            maxX = 1f;
            minY = 0f;
            maxY = 1f;
        }
        float spanX = Math.max(1f, maxX - minX);
        float spanY = Math.max(1f, maxY - minY);
        scale = Math.min(mapW / spanX, mapH / spanY) * 0.62f * zoom;
        centerMapX = (minX + maxX) * 0.5f;
        centerMapY = (minY + maxY) * 0.5f;
    }

    private float nodeX(float wx) {
        return mapX + mapW * 0.5f + panX + (wx - centerMapX) * scale;
    }

    private float nodeY(float wy) {
        return mapY + mapH * 0.5f + panY + (wy - centerMapY) * scale;
    }

    private void selectNode(float sx, float sy) {
        int best = -1;
        float bestDist = 22f * game.u;
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            SectorDef sector = sectorAt(i);
            if (sector == null || !game.session.universe.isDiscovered(sector.id)) {
                continue;
            }
            float dx = sx - nodeX(sector.mapX);
            float dy = sy - nodeY(sector.mapY);
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        if (best >= 0) {
            selected = best;
            message = "";
        }
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
        TravelPlan plan = TravelPlan.quote(game.session, sector.id, mode);
        if (!plan.ok()) {
            message = plan.error;
            return;
        }
        if (game.travelTo(sector.id, mode)) {
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
        computeProjection();

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
        ui.panel(mapX, mapY, mapW, mapH, 8f * u, DECK_C, Visuals.PANEL_EDGE);
        drawGraph();
        boolean canTravel = sectorAt(selected) != null
                && !sectorAt(selected).id.equals(game.session.currentSector.id)
                && game.session.universe.isDiscovered(sectorAt(selected).id);
        ui.drawButton(btnTravel, Visuals.BTN_PRIMARY, canTravel);
        ui.drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnLocal, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnMode, Visuals.BTN_WARM, true);
        ui.drawButton(btnZoomIn, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnZoomOut, Visuals.BTN_SECONDARY, true);
        ui.drawWarpIcon(btnTravel.bounds.x + 22f * u,
                btnTravel.bounds.y + btnTravel.bounds.height * 0.5f,
                8f * u, canTravel ? Visuals.ACCENT : Visuals.TEXT_OFF);
        shapes.end();

        batch.begin();
        ui.textLeft("MAPA DE NAVEGACION", px + 24f * u, h - 32f * u,
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
        drawGraphLabels();
        drawFooter();
        batch.end();
    }

    private void drawGraph() {
        // Aristas sector-sector dentro del mismo sistema.
        for (GalaxyDef galaxy : game.session.universe.galaxies()) {
            for (SystemDef system : galaxy.systems) {
                for (int i = 0; i < system.sectors.size(); i++) {
                    for (int j = i + 1; j < system.sectors.size(); j++) {
                        SectorDef a = system.sectors.get(i);
                        SectorDef b = system.sectors.get(j);
                        if (!game.session.universe.isDiscovered(a.id)
                                || !game.session.universe.isDiscovered(b.id)) {
                            continue;
                        }
                        shapes.setColor(EDGE_C);
                        shapes.rectLine(nodeX(a.mapX), nodeY(a.mapY),
                                nodeX(b.mapX), nodeY(b.mapY), 1.2f * game.u);
                    }
                }
            }
        }
        // Nodos de galaxia y sistema.
        for (GalaxyDef galaxy : game.session.universe.galaxies()) {
            if (!galaxyKnown(galaxy)) {
                continue;
            }
            float gx = nodeX(galaxy.mapX);
            float gy = nodeY(galaxy.mapY);
            shapes.setColor(GALAXY_C.r, GALAXY_C.g, GALAXY_C.b, 0.10f);
            shapes.circle(gx, gy, 34f * game.u, 24);
            shapes.setColor(GALAXY_C);
            shapes.circle(gx, gy, 5f * game.u, 12);
            for (SystemDef system : galaxy.systems) {
                if (!systemKnown(system)) {
                    continue;
                }
                float sx = nodeX(system.mapX);
                float sy = nodeY(system.mapY);
                shapes.setColor(Visuals.INFO.r, Visuals.INFO.g, Visuals.INFO.b, 0.75f);
                shapes.circle(sx, sy, 8f * game.u, 14);
                shapes.setColor(EDGE_C);
                shapes.rectLine(gx, gy, sx, sy, 1f * game.u);
            }
        }
        // Ruta desde el sector actual al elegido.
        SectorDef routeTo = sectorAt(selected);
        if (routeTo != null && game.session.currentSector != null
                && !routeTo.id.equals(game.session.currentSector.id)
                && game.session.universe.isDiscovered(routeTo.id)) {
            SectorDef from = game.session.currentSector;
            shapes.setColor(ROUTE_C);
            shapes.rectLine(nodeX(from.mapX), nodeY(from.mapY),
                    nodeX(routeTo.mapX), nodeY(routeTo.mapY), 1.8f * game.u);
        }
        // Sectores descubiertos.
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            SectorDef sector = sectorAt(i);
            if (sector == null || !game.session.universe.isDiscovered(sector.id)) {
                continue;
            }
            float x = nodeX(sector.mapX);
            float y = nodeY(sector.mapY);
            boolean current = sector.id.equals(game.session.currentSector.id);
            boolean chosen = i == selected;
            if (chosen) {
                shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.28f);
                shapes.circle(x, y, 16f * game.u, 18);
            }
            shapes.setColor(current ? Visuals.GOOD : chosen ? Visuals.ACCENT : Visuals.INFO);
            shapes.circle(x, y, current ? 6f * game.u : 4.5f * game.u, 12);
            if (current) {
                shapes.setColor(Visuals.GOOD.r, Visuals.GOOD.g, Visuals.GOOD.b, 0.35f);
                shapes.circle(x, y, 12f * game.u, 18);
            }
        }
    }

    private void drawGraphLabels() {
        float u = game.u;
        for (GalaxyDef galaxy : game.session.universe.galaxies()) {
            if (!galaxyKnown(galaxy)) {
                continue;
            }
            ui.textLeftFit(galaxy.name, nodeX(galaxy.mapX) - 26f * u,
                    nodeY(galaxy.mapY) - 10f * u, 0.54f * u, 120f * u,
                    Visuals.TEXT_DIM);
        }
        for (int i = 0; i < game.session.universe.sectorCount(); i++) {
            SectorDef sector = sectorAt(i);
            if (sector == null || !game.session.universe.isDiscovered(sector.id)) {
                continue;
            }
            float x = nodeX(sector.mapX);
            float y = nodeY(sector.mapY);
            ui.textCenteredFit((i == selected ? "> " : "") + sector.name,
                    x, y - 20f * u, 0.50f * u, 90f * u,
                    i == selected ? Visuals.ACCENT : Visuals.TEXT_DIM);
        }
    }

    private boolean galaxyKnown(GalaxyDef galaxy) {
        for (SystemDef system : galaxy.systems) {
            if (systemKnown(system)) {
                return true;
            }
        }
        return false;
    }

    private boolean systemKnown(SystemDef system) {
        for (SectorDef sector : system.sectors) {
            if (game.session.universe.isDiscovered(sector.id)) {
                return true;
            }
        }
        return false;
    }

    private void drawFooter() {
        float u = game.u;
        SectorDef sector = sectorAt(selected);
        TravelPlan plan = sector == null ? null
                : TravelPlan.quote(game.session, sector.id, mode);
        String info;
        if (sector == null) {
            info = "ELIGE UN DESTINO";
        } else if (!game.session.universe.isDiscovered(sector.id)) {
            info = "SECTOR NO DESCUBIERTO";
        } else if (sector.id.equals(game.session.currentSector.id)) {
            info = "ESTAS EN " + sector.name;
        } else if (plan != null && plan.ok()) {
            info = sector.name + "   " + String.format(java.util.Locale.US,
                    "%.1f u   C%d E%d   %s", plan.distance, plan.fuelCost,
                    plan.energyCost, plan.timeLabel());
        } else {
            info = plan == null || plan.error.isEmpty() ? "SIN COSTE" : plan.error;
        }
        ui.textCenteredFit(info, px + pw * 0.5f, py + 178f * u,
                0.66f * u, pw - 40f * u, Visuals.TEXT);
        ui.textLeft("MODO", btnMode.bounds.x + btnMode.bounds.width + 12f * u,
                py + 214f * u, 0.56f * u, Visuals.TEXT_OFF);
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, px + pw * 0.5f, py + 152f * u,
                    0.60f * u, pw - 40f * u, Visuals.WARN);
        } else {
            TravelPlan warp = sector == null ? null
                    : TravelPlan.quote(game.session, sector.id, TravelPlan.Mode.WARP);
            TravelPlan conv = sector == null ? null
                    : TravelPlan.quote(game.session, sector.id,
                    TravelPlan.Mode.CONVENTIONAL);
            String both = "WARP "
                    + (warp == null ? "-" : warp.ok() ? "OK" : warp.error)
                    + "   CONV "
                    + (conv == null ? "-" : conv.ok() ? "OK" : conv.error);
            ui.textCenteredFit(both, px + pw * 0.5f, py + 152f * u,
                    0.56f * u, pw - 40f * u, Visuals.TEXT_OFF);
        }
        ui.textCenteredFit("ARRASTRA PARA MOVER   +/- ZOOM", px + pw * 0.5f,
                py + 132f * u, 0.48f * u, pw - 40f * u, Visuals.TEXT_OFF);
        ui.drawButtonText(btnTravel, true);
        ui.drawButtonText(btnBack, true);
        ui.drawButtonText(btnLocal, true);
        ui.drawButtonText(btnMode, true);
        ui.drawButtonText(btnZoomIn, true);
        ui.drawButtonText(btnZoomOut, true);
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
