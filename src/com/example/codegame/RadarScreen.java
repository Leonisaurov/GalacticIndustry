package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Radar circular (UPD-006-T43): el jugador en el centro, alcance real de los
 * sensores y contactos conocidos. No revela entidades fuera del alcance ni
 * inventa pilotos sin posición.
 */
final class RadarScreen {

    private static final Color FLOOR = new Color(0.020f, 0.030f, 0.045f, 1f);
    private static final Color RING = new Color(0.24f, 0.52f, 0.62f, 0.55f);
    private static final Color GRID = new Color(0.16f, 0.34f, 0.40f, 0.30f);
    private static final int MAX_CONTACTS = 150;

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Vector2 tmp = new Vector2();
    private final Button[] filters = new Button[4];
    private Button btnClose;
    private final boolean[] enabled = {true, true, true, true};
    private String message = "";

    private float cx, cy, radius;

    RadarScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes,
                SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
    }

    void enter() {
        message = "";
        layout();
    }

    void layout() {
        layout(game.hudView.getWorldWidth(), game.hudView.getWorldHeight());
    }

    void layout(float w, float h) {
        float u = game.u;
        cx = w * 0.5f;
        cy = h * 0.54f;
        radius = Math.min(w * 0.40f, h * 0.30f);
        float bw = Math.min(w * 0.42f, 170f * u);
        float bh = 40f * u;
        float gap = 8f * u;
        float x0 = (w - bw * 2f - gap) * 0.5f;
        float y0 = cy - radius - 96f * u;
        filters[0] = new Button("ASTEROIDES", x0, y0, bw, bh);
        filters[1] = new Button("BIDONES", x0 + bw + gap, y0, bw, bh);
        filters[2] = new Button("ESTACIONES", x0, y0 - bh - gap, bw, bh);
        filters[3] = new Button("OBJETIVO", x0 + bw + gap, y0 - bh - gap, bw, bh);
        btnClose = new Button("VOLVER", (w - bw) * 0.5f, 30f * u, bw, 46f * u);
    }

    boolean handle() {
        if (btnClose == null) {
            layout();
        }
        if (game.tapped(btnClose) || Gdx.input.isKeyJustPressed(
                com.badlogic.gdx.Input.Keys.BACK)) {
            game.closeRadar();
            return true;
        }
        for (int i = 0; i < filters.length; i++) {
            if (game.tapped(filters[i])) {
                enabled[i] = !enabled[i];
                return true;
            }
        }
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < 5; i++) {
                if (!Gdx.input.isTouched(i)) {
                    continue;
                }
                tmp.set(Gdx.input.getX(i),
                        Gdx.graphics.getHeight() - Gdx.input.getY(i));
                if (pick(tmp.x, tmp.y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Selecciona el contacto más cercano al toque dentro del radar. */
    private boolean pick(float sx, float sy) {
        float dx = sx - cx;
        float dy = sy - cy;
        if (dx * dx + dy * dy > radius * radius) {
            return false;
        }
        float range = Sensors.range(game.upgrades);
        float scale = range / radius;
        worldTouch(tmp.set(dx * scale, -dy * scale));
        NavTarget target = Sensors.pick(game.world, game.upgrades, tmp);
        if (target == null) {
            return false;
        }
        game.setNavTarget(target);
        message = "OBJETIVO FIJADO";
        return true;
    }

    private final Vector2 wt = new Vector2();

    private void worldTouch(Vector2 local) {
        wt.set(game.world.ship.pos.x + local.x, game.world.ship.pos.y + local.y);
        tmp.set(wt);
    }

    void draw(float w, float h) {
        if (btnClose == null) {
            layout(w, h);
        }
        float u = game.u;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h, FLOOR, FLOOR,
                new Color(0.035f, 0.055f, 0.080f, 1f),
                new Color(0.035f, 0.055f, 0.080f, 1f));
        shapes.end();
        batch.begin();
        Visuals.additive(batch);
        space.stars(w * 0.5f + game.time * 4f, h * 0.5f, w, h, u, 0.55f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        ui.grid(w, h, 90f * u, 0.012f);
        ui.frame(12f * u, 12f * u, w - 24f * u, h - 24f * u, 12f * u,
                new Color(0.030f, 0.042f, 0.058f, 0.92f),
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        // Anillos y ejes.
        shapes.setColor(RING);
        shapes.circle(cx, cy, radius, 72);
        shapes.setColor(GRID);
        for (int i = 1; i <= 3; i++) {
            shapes.circle(cx, cy, radius * i / 3f, 64);
        }
        shapes.setColor(RING.r, RING.g, RING.b, 0.35f);
        shapes.rect(cx - radius, cy, radius * 2f, 1.2f);
        shapes.rect(cx, cy - radius, 1.2f, radius * 2f);
        shapes.setColor(Visuals.GOOD);
        shapes.circle(cx, cy, 5f * u, 12);
        // Nave orientada.
        float hx = MathUtils.cosDeg(game.world.ship.heading);
        float hy = MathUtils.sinDeg(game.world.ship.heading);
        shapes.setColor(Visuals.ACCENT);
        shapes.triangle(cx + hx * 12f * u, cy + hy * 12f * u,
                cx - hy * 6f * u, cy + hx * 6f * u,
                cx + hy * 6f * u, cy - hx * 6f * u);
        drawContacts();
        for (int i = 0; i < filters.length; i++) {
            ui.drawButton(filters[i], i < 2 ? Visuals.BTN_SECONDARY : Visuals.BTN_PRIMARY,
                    enabled[i]);
        }
        ui.drawButton(btnClose, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("RADAR", 30f * u, h - 30f * u, 0.94f * u, Visuals.TEXT);
        ui.textRight("ALCANCE " + Math.round(Sensors.range(game.upgrades)) + " u",
                w - 30f * u, h - 30f * u, 0.64f * u, Visuals.TEXT_DIM);
        ui.textCenteredFit("CONTACTOS DENTRO DEL ALCANCE REAL",
                w * 0.5f, h - 58f * u, 0.58f * u, w - 60f * u, Visuals.TEXT_OFF);
        ui.textLeft("FILTROS", 30f * u, cy - radius - 46f * u, 0.60f * u,
                Visuals.TEXT_DIM);
        ui.textLeft("N", cx, cy + radius + 18f * u, 0.60f * u, Visuals.TEXT_OFF);
        ui.textRight("E", cx + radius + 12f * u, cy + 4f * u, 0.60f * u,
                Visuals.TEXT_OFF);
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, w * 0.5f, 96f * u, 0.66f * u,
                    w - 40f * u, Visuals.GOLD);
        }
        for (int i = 0; i < filters.length; i++) {
            ui.drawButtonText(filters[i], true);
        }
        ui.drawButtonText(btnClose, true);
        batch.end();
    }

    private void drawContacts() {
        GameWorld world = game.world;
        float range = Sensors.range(game.upgrades);
        float scale = radius / Math.max(1f, range);
        int drawn = 0;
        if (enabled[2]) {
            drawn = contact(world.station.pos.x, world.station.pos.y, scale,
                    Visuals.ACCENT, 5f, drawn, true);
            if (world.playerStation.inSector(world.sector.id)) {
                drawn = contact(world.playerStation.pos.x, world.playerStation.pos.y,
                        scale, Visuals.INFO, 4f, drawn, true);
            }
        }
        if (enabled[0]) {
            for (int i = 0; i < world.asteroids.size && drawn < MAX_CONTACTS; i++) {
                Asteroid a = world.asteroids.get(i);
                if (!Sensors.inRange(a.pos.x, a.pos.y, world.ship.pos, range)) {
                    continue;
                }
                drawn = contact(a.pos.x, a.pos.y, scale, Visuals.STEEL_LIGHT,
                        a.size == Asteroid.LARGE ? 4f : 2.6f, drawn, false);
            }
        }
        if (enabled[1]) {
            for (int i = 0; i < world.fuel.size && drawn < MAX_CONTACTS; i++) {
                FuelPickup f = world.fuel.get(i);
                if (!Sensors.inRange(f.pos.x, f.pos.y, world.ship.pos, range)) {
                    continue;
                }
                drawn = contact(f.pos.x, f.pos.y, scale, Visuals.GOOD, 2.6f,
                        drawn, false);
            }
        }
        if (enabled[3] && game.navTarget() != null) {
            NavTarget t = game.navTarget();
            t.sync();
            if (Sensors.inRange(t.pos.x, t.pos.y, world.ship.pos, range)) {
                contact(t.pos.x, t.pos.y, scale, Visuals.GOLD, 5f, drawn, false);
            }
        }
    }

    private int contact(float x, float y, float scale, Color color, float size,
                        int drawn, boolean always) {
        if (drawn >= MAX_CONTACTS) {
            return drawn;
        }
        float dx = x - game.world.ship.pos.x;
        float dy = y - game.world.ship.pos.y;
        float px = cx + dx * scale;
        float py = cy - dy * scale;
        if (!always && (px - cx) * (px - cx) + (py - cy) * (py - cy)
                > radius * radius) {
            return drawn;
        }
        shapes.setColor(color);
        shapes.circle(px, py, size, 8);
        return drawn + 1;
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
