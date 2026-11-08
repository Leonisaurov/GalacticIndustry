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
 * Mapa local 2D del sector (UPD-006-T11/T12/T13/T14): proyección real del
 * mundo, zoom, desplazamiento, iconos de entidades, niebla de chunks y
 * selección de objetivos. No revela nada de chunks no generados.
 */
final class LocalMapScreen {

    private static final Color PANEL_C = new Color(0.028f, 0.040f, 0.055f, 0.94f);
    private static final Color FLOOR_C = new Color(0.020f, 0.028f, 0.042f, 1f);
    private static final Color FOG_C = new Color(0.005f, 0.008f, 0.014f, 0.92f);
    private static final Color GRID_C = new Color(0.10f, 0.18f, 0.26f, 0.35f);
    private static final int MAX_ICONS = 150;

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Button btnZoomIn, btnZoomOut, btnPin, btnBack, btnNav;
    private Button[] buttons;
    private final Vector2 tmp = new Vector2();

    private float px, py, pw, ph;
    private float scale = 0.12f;
    private float camX;
    private float camY;
    private float lastTouchX = -1f;
    private float lastTouchY = -1f;
    private int dragPointer = -1;

    private NavTarget selected;
    private String message = "";

    LocalMapScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes,
                   SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
        btnZoomIn = new Button("+", 0f, 0f, 44f, 44f);
        btnZoomOut = new Button("-", 0f, 0f, 44f, 44f);
        btnPin = new Button("FIJAR OBJETIVO", 0f, 0f, 120f, 46f);
        btnBack = new Button("VOLVER", 0f, 0f, 120f, 46f);
        btnNav = new Button("NAVEGACION", 0f, 0f, 120f, 46f);
        buttons = new Button[]{btnZoomIn, btnZoomOut, btnPin, btnBack, btnNav};
    }

    void enter() {
        message = "";
        selected = null;
        camX = game.world.ship.pos.x;
        camY = game.world.ship.pos.y;
        scale = 0.12f;
        layout();
    }

    void layout() {
        layout(game.hudView.getWorldWidth(), game.hudView.getWorldHeight());
    }

    void layout(float w, float h) {
        float u = game.u;
        px = 8f * u;
        py = 8f * u;
        pw = w - 16f * u;
        ph = h - 16f * u;
        float bw = Math.min(w * 0.44f, 180f * u);
        float bh = 44f * u;
        float gap = 6f * u;
        float x0 = (w - bw * 2f - gap) * 0.5f;
        btnBack.bounds.set(x0, py + 12f * u, bw, bh);
        btnNav.bounds.set(x0 + bw + gap, py + 12f * u, bw, bh);
        btnPin.bounds.set(x0, py + 12f * u + bh + gap, bw * 1.2f, bh);
        btnZoomIn.bounds.set(w - 62f * u, h - 130f * u, 46f * u, 46f * u);
        btnZoomOut.bounds.set(w - 62f * u, h - 184f * u, 46f * u, 46f * u);
    }

    int discoveredCount() {
        return game.session.universe.discoveredCount();
    }

    float scale() {
        return scale;
    }

    String message() {
        return message;
    }

    String selectedLabel() {
        return selected == null ? "" : selected.label;
    }

    boolean handle() {
        if (btnBack == null) {
            layout();
        }
        if (game.tapped(btnBack)) {
            game.closeLocalMap();
            return true;
        }
        if (game.tapped(btnNav)) {
            game.openMap();
            return true;
        }
        if (game.tapped(btnZoomIn)) {
            zoomAt(1.35f, game.hudView.getWorldWidth() * 0.5f,
                    game.hudView.getWorldHeight() * 0.5f);
            return true;
        }
        if (game.tapped(btnZoomOut)) {
            zoomAt(1f / 1.35f, game.hudView.getWorldWidth() * 0.5f,
                    game.hudView.getWorldHeight() * 0.5f);
            return true;
        }
        if (game.tapped(btnPin)) {
            pinSelected();
            return true;
        }
        handleDrag();
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < 5; i++) {
                if (Gdx.input.isTouched(i)) {
                    float x = Gdx.input.getX(i);
                    float y = Gdx.graphics.getHeight() - Gdx.input.getY(i);
                    if (!insidePanel(x, y) || overButton(x, y)) {
                        continue;
                    }
                    selectAt(x, y);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean overButton(float x, float y) {
        for (Button b : buttons) {
            if (b.bounds.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private boolean insidePanel(float x, float y) {
        return x >= px && x <= px + pw && y >= py && y <= py + ph;
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
                    if (insidePanel(x, y) && !overButton(x, y)) {
                        dragPointer = i;
                        lastTouchX = x;
                        lastTouchY = y;
                        break;
                    }
                }
            }
            return;
        }
        float x = Gdx.input.getX(dragPointer);
        float y = Gdx.graphics.getHeight() - Gdx.input.getY(dragPointer);
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;
        if (Math.abs(dx) + Math.abs(dy) > 0.5f) {
            camX -= dx / Math.max(0.001f, scale);
            camY -= dy / Math.max(0.001f, scale);
            clampCamera();
        }
        lastTouchX = x;
        lastTouchY = y;
    }

    private void zoomAt(float factor, float sx, float sy) {
        float beforeX = camX + (sx - (px + pw * 0.5f)) / scale;
        float beforeY = camY + (sy - (py + ph * 0.5f)) / scale;
        scale = MathUtils.clamp(scale * factor, 0.04f, 2f);
        camX = beforeX - (sx - (px + pw * 0.5f)) / scale;
        camY = beforeY - (sy - (py + ph * 0.5f)) / scale;
        clampCamera();
    }

    private void clampCamera() {
        float hw = game.world.halfWidth();
        float hh = game.world.halfHeight();
        camX = MathUtils.clamp(camX, -hw, hw);
        camY = MathUtils.clamp(camY, -hh, hh);
    }

    private void selectAt(float sx, float sy) {
        float best = 26f * game.u;
        NavTarget found = null;
        GameWorld world = game.world;
        if (near(sx, sy, world.station.pos, world.station.def.radius, best)) {
            found = NavTarget.ofStation(world.station, true);
            best = screenDistance(sx, sy, world.station.pos) - 12f;
        }
        if (world.playerStation.inSector(world.sector.id)
                && near(sx, sy, world.playerStation.pos, 40f, best)) {
            found = new NavTarget(NavTarget.Type.STATION, "player-station",
                    "PLATAFORMA", world.playerStation.pos.x,
                    world.playerStation.pos.y);
            best = screenDistance(sx, sy, world.playerStation.pos) - 12f;
        }
        float range = Sensors.range(game.upgrades);
        if (found == null) {
            for (int i = 0; i < world.asteroids.size; i++) {
                Asteroid a = world.asteroids.get(i);
                if (!Sensors.inRange(a.pos.x, a.pos.y, world.ship.pos, range)) {
                    continue;
                }
                if (near(sx, sy, a.pos, a.radius, best)) {
                    found = NavTarget.ofAsteroid(a);
                    best = screenDistance(sx, sy, a.pos) - 8f;
                }
            }
        }
        if (found == null) {
            for (int i = 0; i < world.fuel.size; i++) {
                FuelPickup f = world.fuel.get(i);
                if (!Sensors.inRange(f.pos.x, f.pos.y, world.ship.pos, range)) {
                    continue;
                }
                if (near(sx, sy, f.pos, FuelPickup.RADIUS + 6f, best)) {
                    found = NavTarget.ofFuel(f);
                    best = screenDistance(sx, sy, f.pos) - 8f;
                }
            }
        }
        if (found == null) {
            if (world.legacyWreck && near(sx, sy, world.legacyWreckPos, 20f, 26f * game.u)) {
                found = new NavTarget(NavTarget.Type.POINT, "wreck", "RESTOS",
                        world.legacyWreckPos.x, world.legacyWreckPos.y);
            } else {
                message = "SIN OBJETIVO";
                return;
            }
        }
        selected = found;
        message = "SELECCIONADO";
    }

    private void pinSelected() {
        if (selected == null) {
            message = "ELIGE UN OBJETIVO";
            return;
        }
        if (!game.upgrades.loadout().hasNavigation()) {
            message = "REQUIERE MODULO NAVEGACION";
            return;
        }
        game.setNavTarget(selected);
        message = "OBJETIVO FIJADO";
    }

    private boolean near(float sx, float sy, Vector2 worldPos, float worldRadius,
                         float threshold) {
        float d = screenDistance(sx, sy, worldPos);
        return d <= Math.max(threshold, worldRadius * scale + 8f * game.u);
    }

    private float screenX(float worldX) {
        return px + pw * 0.5f + (worldX - camX) * scale;
    }

    private float screenY(float worldY) {
        return py + ph * 0.5f + (worldY - camY) * scale;
    }

    private float screenDistance(float sx, float sy, Vector2 worldPos) {
        float dx = sx - screenX(worldPos.x);
        float dy = sy - screenY(worldPos.y);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    void draw(float w, float h) {
        if (btnBack == null) {
            layout(w, h);
        }
        float u = game.u;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h, FLOOR_C, FLOOR_C,
                new Color(0.030f, 0.045f, 0.065f, 1f),
                new Color(0.030f, 0.045f, 0.065f, 1f));
        shapes.end();
        batch.begin();
        Visuals.additive(batch);
        space.stars(w * 0.5f, h * 0.5f, w, h, u, 0.35f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        ui.frame(px, py, pw, ph, 12f * u, PANEL_C, Visuals.PANEL_EDGE, Visuals.ACCENT);
        drawGridAndSector();
        drawFog();
        drawEntities();
        drawShip();
        drawSelected();
        for (Button b : buttons) {
            ui.drawButton(b, b == btnPin ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        }
        shapes.end();

        batch.begin();
        ui.textLeft("MAPA LOCAL", px + 22f * u, h - 26f * u, 0.94f * u, Visuals.TEXT);
        ui.textRight("ESCALA 1:" + Math.round(1f / Math.max(0.0001f, scale))
                        + "  ZOOM " + Math.round(scale * 100f) + "%",
                px + pw - 22f * u, h - 26f * u, 0.62f * u, Visuals.TEXT_DIM);
        ui.textLeftFit("SECTOR " + (game.session.currentSector == null ? ""
                        : game.session.currentSector.name) + "   CHUNKS "
                        + game.world.generatedChunks().size(),
                px + 22f * u, h - 50f * u, 0.62f * u, pw - 44f * u, Visuals.TEXT_OFF);
        drawSelectedPanel(w, h);
        ui.textCenteredFit(message, w * 0.5f, py + 150f * u, 0.64f * u,
                pw - 30f * u, Visuals.WARN);
        for (Button b : buttons) {
            ui.drawButtonText(b, true);
        }
        batch.end();
    }

    private void drawGridAndSector() {
        float u = game.u;
        float hw = game.world.halfWidth();
        float hh = game.world.halfHeight();
        shapes.setColor(GRID_C);
        float step = 1000f;
        for (float x = -hw; x <= hw; x += step) {
            shapes.rect(screenX(x), screenY(-hh), 1.2f * u,
                    (hh * 2f) * scale);
        }
        for (float y = -hh; y <= hh; y += step) {
            shapes.rect(screenX(-hw), screenY(y), (hw * 2f) * scale, 1.2f * u);
        }
        shapes.setColor(Visuals.STEEL.r, Visuals.STEEL.g, Visuals.STEEL.b, 0.55f);
        float left = screenX(-hw);
        float bottom = screenY(-hh);
        float width = hw * 2f * scale;
        float height = hh * 2f * scale;
        shapes.rect(left, bottom, width, 2f * u);
        shapes.rect(left, bottom + height - 2f * u, width, 2f * u);
        shapes.rect(left, bottom, 2f * u, height);
        shapes.rect(left + width - 2f * u, bottom, 2f * u, height);
    }

    /** Niebla: solo se iluminan los chunks realmente generados. */
    private void drawFog() {
        float chunk = GameWorld.CHUNK;
        float halfW = game.world.halfWidth();
        float halfH = game.world.halfHeight();
        int minCx = (int) Math.floor((camX - (pw * 0.5f) / scale) / chunk);
        int maxCx = (int) Math.ceil((camX + (pw * 0.5f) / scale) / chunk);
        int minCy = (int) Math.floor((camY - (ph * 0.5f) / scale) / chunk);
        int maxCy = (int) Math.ceil((camY + (ph * 0.5f) / scale) / chunk);
        int max = 400;
        int drawn = 0;
        for (int cx = minCx; cx <= maxCx && drawn < max; cx++) {
            for (int cy = minCy; cy <= maxCy && drawn < max; cy++) {
                long key = ((long) cx << 32) | (cy & 0xffffffffL);
                if (game.world.generatedChunks().contains(key)) {
                    continue;
                }
                float wx = cx * chunk;
                float wy = cy * chunk;
                if (Math.abs(wx) > halfW + chunk || Math.abs(wy) > halfH + chunk) {
                    continue;
                }
                shapes.setColor(FOG_C);
                shapes.rect(screenX(wx), screenY(wy), chunk * scale, chunk * scale);
                drawn++;
            }
        }
    }

    private void drawEntities() {
        GameWorld world = game.world;
        float u = game.u;
        // Estación principal.
        float sx = screenX(world.station.pos.x);
        float sy = screenY(world.station.pos.y);
        float sr = Math.max(6f * u, world.station.def.radius * scale);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.25f);
        shapes.circle(sx, sy, sr + 6f * u, 28);
        shapes.setColor(Visuals.ACCENT);
        shapes.circle(sx, sy, 5f * u, 12);
        float ring = world.station.def.safeRadius * scale;
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.25f);
        shapes.circle(sx, sy, ring, 48);
        if (world.playerStation.inSector(world.sector.id)) {
            float plotX = screenX(world.playerStation.pos.x);
            float plotY = screenY(world.playerStation.pos.y);
            shapes.setColor(Visuals.INFO);
            shapes.rect(plotX - 5f * u, plotY - 5f * u, 10f * u, 10f * u);
        }
        float range = Sensors.range(game.upgrades);
        int icons = 0;
        for (int i = 0; i < world.asteroids.size && icons < MAX_ICONS; i++) {
            Asteroid a = world.asteroids.get(i);
            if (!Sensors.inRange(a.pos.x, a.pos.y, world.ship.pos, range)) {
                continue;
            }
            float ax = screenX(a.pos.x);
            float ay = screenY(a.pos.y);
            if (ax < px || ax > px + pw || ay < py || ay > py + ph) {
                continue;
            }
            shapes.setColor(Visuals.STEEL_LIGHT);
            shapes.circle(ax, ay, Math.max(2f * u, a.radius * scale), 8);
            icons++;
        }
        for (int i = 0; i < world.fuel.size && icons < MAX_ICONS; i++) {
            FuelPickup f = world.fuel.get(i);
            if (!Sensors.inRange(f.pos.x, f.pos.y, world.ship.pos, range)) {
                continue;
            }
            float fx = screenX(f.pos.x);
            float fy = screenY(f.pos.y);
            shapes.setColor(Visuals.GOOD);
            shapes.triangle(fx, fy + 4f * u, fx - 4f * u, fy - 3f * u,
                    fx + 4f * u, fy - 3f * u);
            icons++;
        }
        if (world.legacyWreck) {
            float wx = screenX(world.legacyWreckPos.x);
            float wy = screenY(world.legacyWreckPos.y);
            shapes.setColor(Visuals.WARN);
            shapes.rect(wx - 6f * u, wy - 1f * u, 12f * u, 2f * u);
            shapes.rect(wx - 1f * u, wy - 6f * u, 2f * u, 12f * u);
        }
        if (game.navTarget() != null) {
            NavTarget t = game.navTarget();
            t.sync();
            float tx = screenX(t.pos.x);
            float ty = screenY(t.pos.y);
            float pulse = 1f + 0.12f * MathUtils.sin(game.time * 6f);
            shapes.setColor(Visuals.GOLD.r, Visuals.GOLD.g, Visuals.GOLD.b, 0.85f);
            shapes.circle(tx, ty, 10f * u * pulse, 18);
            shapes.setColor(FLOOR_C);
            shapes.circle(tx, ty, 6f * u * pulse, 14);
        }
    }

    private void drawShip() {
        float u = game.u;
        float sx = screenX(game.world.ship.pos.x);
        float sy = screenY(game.world.ship.pos.y);
        float angle = game.world.ship.heading;
        float c = MathUtils.cosDeg(angle);
        float s = MathUtils.sinDeg(angle);
        float size = 11f * u;
        shapes.setColor(Visuals.ACCENT);
        shapes.triangle(sx + c * size, sy + s * size,
                sx - s * size * 0.6f - c * size * 0.5f,
                sy + c * size * 0.6f - s * size * 0.5f,
                sx + s * size * 0.6f - c * size * 0.5f,
                sy - c * size * 0.6f - s * size * 0.5f);
    }

    private void drawSelected() {
        if (selected == null) {
            return;
        }
        selected.sync();
        float u = game.u;
        float x = screenX(selected.pos.x);
        float y = screenY(selected.pos.y);
        shapes.setColor(Visuals.GOLD);
        shapes.circle(x, y, 13f * u, 20);
    }

    private void drawSelectedPanel(float w, float h) {
        float u = game.u;
        if (selected == null) {
            ui.textLeftFit("TOCA UN ICONO PARA VER SU FICHA",
                    px + 22f * u, py + 118f * u, 0.60f * u, pw - 44f * u,
                    Visuals.TEXT_OFF);
            return;
        }
        selected.sync();
        float d = selected.pos.dst(game.world.ship.pos);
        float dx = selected.pos.x - game.world.ship.pos.x;
        float dy = selected.pos.y - game.world.ship.pos.y;
        float abs = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float rel = Ship.wrapDeg(abs - game.world.ship.heading);
        ui.textLeftFit(selected.label, px + 22f * u, py + 118f * u,
                0.68f * u, pw - 44f * u, Visuals.GOLD);
        ui.textLeftFit("DIST " + Math.round(d) + " u   RUMBO "
                        + (rel >= 0f ? "+" : "") + Math.round(rel) + " deg",
                px + 22f * u, py + 96f * u, 0.58f * u, pw - 44f * u, Visuals.TEXT);
        ui.textLeftFit(game.upgrades.loadout().hasNavigation()
                        ? "FIJAR PARA LA BRUJULA AVANZADA"
                        : "INSTALA NAVEGACION PARA FIJARLO",
                px + 22f * u, py + 74f * u, 0.52f * u, pw - 44f * u,
                Visuals.TEXT_DIM);
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
