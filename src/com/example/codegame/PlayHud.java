package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * HUD de la partida: panel superior, combustible, energía, casco,
 * supervivencia, avisos, joystick y barra de aceleración. Lee el estado del
 * mundo y un {@link HudView} reutilizable; no conoce el flujo de MyGame.
 */
final class PlayHud {

    private static final Color PANEL_BOTTOM_C = new Color(0.030f, 0.042f, 0.058f, 0.94f);
    private static final Color PANEL_TOP_C = new Color(0.070f, 0.095f, 0.122f, 0.96f);
    private static final Color FUEL_TRACK_C = new Color(0.025f, 0.038f, 0.052f, 0.96f);
    private static final Color FUEL_EDGE_C = new Color(0.28f, 0.38f, 0.44f, 0.65f);
    private static final Color CONTROL_TRACK_C = new Color(0.035f, 0.055f, 0.075f, 0.72f);
    private static final Color CONTROL_EDGE_C = new Color(0.32f, 0.44f, 0.52f, 0.45f);
    private static final Color COIN_CHIP_C = new Color(0.07f, 0.09f, 0.11f, 0.94f);
    private static final Color tint = new Color();
    private static final Color tint2 = new Color();

    private final UiKit ui;
    private GameWorld world;
    private final GameSession session;
    private final Upgrades upgrades;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final Visuals art;
    private final HudView hud;
    private float u = 1f;

    /** Estado cacheado de la brújula para no crear objetos por frame. */
    private boolean compassHas;
    private boolean compassLocal;
    private boolean compassBehind;
    private boolean compassHere;
    private float compassRel;
    private float compassDist;
    private String compassLabel = "";
    private static final Color COMPASS_PANEL = new Color(0.030f, 0.042f, 0.058f, 0.90f);

    void setWorld(GameWorld world) {
        if (world != null) {
            this.world = world;
        }
    }

    PlayHud(UiKit ui, GameWorld world, GameSession session, Upgrades upgrades,
            SpriteBatch batch, ShapeRenderer shapes, Visuals art, HudView hud) {
        this.ui = ui;
        this.world = world;
        this.session = session;
        this.upgrades = upgrades;
        this.batch = batch;
        this.shapes = shapes;
        this.art = art;
        this.hud = hud;
    }

    void draw(float w, float h) {
        u = ui.u;
        drawPlayHud(w, h);
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private float topPanelH() {
        return 172f * u;
    }

    void drawPlayHud(float w, float h) {
        float pad = 16f * u;
        float panelH = topPanelH();

        beginShapes();
        shapes.rect(0f, h - panelH, w, panelH,
                PANEL_BOTTOM_C,
                PANEL_BOTTOM_C,
                PANEL_TOP_C,
                PANEL_TOP_C);
        // Franja de consola: acento, canales de ventilacion y tornillos.
        shapes.setColor(0.020f, 0.030f, 0.040f, 0.96f);
        shapes.rect(0f, h - panelH, w, 12f * u);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.70f);
        shapes.rect(0f, h - panelH + 12f * u, w, 2f * u);
        shapes.setColor(0f, 0f, 0f, 0.45f);
        shapes.rect(0f, h - panelH - 4f * u, w, 4f * u);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.50f);
        shapes.rect(0f, h - 2.5f * u, w, 1.5f * u);
        shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                Visuals.STEEL_LIGHT.b, 0.20f);
        shapes.rect(0f, h - 4f * u, w, 1f * u);
        shapes.setColor(1f, 1f, 1f, 0.035f);
        shapes.rect(0f, h - panelH + 14f * u, w, 1.5f * u);
        shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                Visuals.STEEL_LIGHT.b, 0.30f);
        shapes.circle(24f * u, h - panelH + 6f * u, 2f * u, 8);
        shapes.circle(w - 24f * u, h - panelH + 6f * u, 2f * u, 8);
        shapes.setColor(0f, 0f, 0f, 0.35f);
        for (int i = 0; i < 8; i++) {
            shapes.rect(w * 0.50f + i * 9f * u, h - panelH + 5f * u, 5f * u, 2f * u);
        }

        drawFuelBar(w, h, panelH);
        drawEnergyBar(w, h);
        drawHullChip(w, h);
        drawSurvivalBars(w, h);
        drawPartAlerts(w, h, panelH);
        updateCompass();
        drawCompassShapes(w, h);
        drawAdvancedCompassMarker(w, h);
        drawDockGuideShapes(w, h);
        if (hud.canRecover) {
            ui.drawButton(hud.recover, Visuals.BTN_PRIMARY, true);
        }
        if (hud.canDock) {
            ui.drawButton(hud.dock, Visuals.BTN_PRIMARY, true);
        }
        if (hud.canRescue) {
            ui.drawButton(hud.rescue, Visuals.BTN_WARM, true);
        }
        if (hud.canOpenMap) {
            ui.drawButton(hud.map, Visuals.BTN_SECONDARY, true);
        }
        if (hud.canOpenRadar) {
            ui.drawButton(hud.radar, Visuals.BTN_SECONDARY, true);
        }
        if (hud.canBuildBase) {
            ui.drawButton(hud.buildBase, Visuals.BTN_PRIMARY, true);
        }
        if (hud.state == MyGame.PLAYING) {
            drawIconButton(hud.pause);
        }
        ui.drawCoinIcon(pad + 18f * u, h - 54f * u, 13f * u);
        if (hud.state == MyGame.PLAYING) {
            drawControls();
        }
        shapes.end();

        drawHudGlow(w, h);

        batch.begin();
        ui.textLeftMiddle(String.valueOf(upgrades.coins() + world.coinsEarned),
                pad + 38f * u, h - 54f * u, 1.15f * u, Visuals.GOLD);
        boolean narrow = w < 600f * u;
        String statusText = "OLEADA " + world.wave + "   PUNTOS " + world.score;
        if (narrow) {
            float left = pad + 8f * u;
            float right = hullChipX(w) - 12f * u;
            ui.textCenteredFit(statusText, (left + right) * 0.5f, h - 82f * u,
                    0.72f * u, Math.max(40f * u, right - left), Visuals.TEXT);
        } else {
            float left = pad + 56f * u;
            float right = hullChipX(w) - 12f * u;
            ui.textCenteredFit(statusText, (left + right) * 0.5f, h - 60f * u,
                    0.78f * u, Math.max(40f * u, right - left), Visuals.TEXT);
        }
        float hullFrac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        float hx = hullChipX(w);
        float hy = hullChipY(h);
        ui.textLeft("CASCO", hx + 32f * u, hy + 36f * u, 0.46f * u, Visuals.TEXT_DIM);
        ui.textLeft(Math.round(hullFrac * 100f) + "%", hx + 32f * u, hy + 22f * u,
                0.74f * u, UiKit.partColor(hullFrac));
        ui.textLeftFit("CARGA " + Math.round(world.ship.cargo().usedMass()) + "/"
                        + Math.round(world.ship.cargo().capacity()) + " UM"
                        + "   MODS " + upgrades.shipModuleCount() + "/"
                        + upgrades.loadout().moduleSlots(),
                pad + 8f * u, h - 134f * u, 0.60f * u, w - 2f * pad,
                Visuals.TEXT_DIM);
        drawEnergyText(w, h);
        drawCompassText(w, h);
        drawDockGuideText(w, h);
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        ui.textCenteredMiddle(Math.round(frac * 100f) + "%",
                pad + 44f * u + fuelBarW(w) * 0.5f, h - 110f * u, 0.78f * u, Color.WHITE);
        drawSurvivalText(w, h);
        if (world.ship.damageFlash > 0f && world.ship.lastDamaged > 0) {
            int hit = world.ship.lastDamaged;
            float hitFrac = world.ship.part[hit] / world.ship.partMax(hit);
            int pct = Math.round(hitFrac * 100f);
            ui.textLeft("GOLPE EN " + Ship.PART_NAMES[hit] + "  " + pct + "%",
                    pad + 8f * u, alertY(h, panelH, 0) + 15f * u, 0.78f * u, UiKit.partColor(hitFrac));
        }
        int o2Alert = session.pilot.o2AlertLevel();
        int o2Pct = Math.round(session.pilot.oxygenFraction() * 100f);
        if (o2Alert == 2) {
            ui.textLeft("! O2 CRITICO " + o2Pct + "%",
                    pad + 8f * u, h - 196f * u, 0.78f * u, Visuals.DANGER);
        } else if (o2Alert == 1) {
            ui.textLeft("O2 BAJO " + o2Pct + "%",
                    pad + 8f * u, h - 196f * u, 0.78f * u, Visuals.WARN);
        }
        if (session.lifeSupport.leak > 0.01f || session.lifeSupport.breach) {
            ui.textLeft(session.lifeSupport.breach ? "BRECHA O2" : "FUGA O2",
                    pad + 8f * u, h - 218f * u, 0.78f * u,
                    session.lifeSupport.breach ? Visuals.DANGER : Visuals.WARN);
        }
        int slot = 0;
        for (int i = 1; i < Ship.PART_COUNT; i++) {
            float pf = world.ship.part[i] / world.ship.partMax(i);
            if (pf >= 0.72f) {
                continue;
            }
            float ax = alertX(w);
            float ay = alertY(h, panelH, slot);
            int pct = Math.round(pf * 100f);
            ui.textLeft(Ship.PART_NAMES[i] + " " + pct + "%",
                    ax + 30f * u, ay + 15f * u, 0.68f * u, UiKit.partColor(pf));
            slot++;
        }
        if (hud.canRecover) {
            ui.drawButtonText(hud.recover, true);
            ui.textCentered("NAVE LEGADO A "
                            + Math.round(world.legacyDistance(world.ship.pos)) + " u",
                    hud.recover.bounds.x + hud.recover.bounds.width * 0.5f,
                    hud.recover.bounds.y + hud.recover.bounds.height + 22f * u,
                    0.72f * u, Visuals.WARN);
        }
        if (hud.canDock) {
            ui.drawButtonText(hud.dock, true);
            ui.textCenteredFit("NODRIZA A "
                            + Math.round(world.station.distanceTo(world.ship.pos)) + " u",
                    hud.dock.bounds.x + hud.dock.bounds.width * 0.5f,
                    hud.dock.bounds.y + hud.dock.bounds.height + 22f * u,
                    0.72f * u, w - 2f * pad, Visuals.ACCENT);
        }
        if (hud.canOpenMap) {
            ui.drawButtonText(hud.map, true);
        }
        if (hud.canOpenRadar) {
            ui.drawButtonText(hud.radar, true);
        }
        if (hud.canBuildBase) {
            ui.drawButtonText(hud.buildBase, true);
            ui.textCentered("BASE: 300 CR + 25 HIERRO + 10 COBRE + 20 SILICATO",
                    hud.buildBase.bounds.x + hud.buildBase.bounds.width * 0.5f,
                    hud.buildBase.bounds.y + hud.buildBase.bounds.height + 22f * u,
                    0.62f * u, Visuals.ACCENT);
        }
        if (hud.canRescue) {
            ui.drawButtonText(hud.rescue, true);
            ui.textCenteredFit("! SIN COMBUSTIBLE - RESCATE 100 CR + CARGA",
                    hud.rescue.bounds.x + hud.rescue.bounds.width * 0.5f,
                    hud.rescue.bounds.y + hud.rescue.bounds.height + 22f * u,
                    0.68f * u, w - 2f * pad, Visuals.WARN);
        }
        if (hud.rescueActive) {
            ui.textCenteredFit(String.format(java.util.Locale.US,
                            "RESCATE EN %.1f s", hud.rescueSeconds),
                    w * 0.5f, h * 0.55f, 1.10f * u, w - 2f * pad, Visuals.WARN);
        }
        if (hud.state == MyGame.PLAYING) {
            ui.textCenteredMiddle("GIRO", hud.joyCx, hud.joyCy + hud.joyR + 28f * u, 0.68f * u, Visuals.TEXT_DIM);
            ui.textCenteredMiddle("ACEL", hud.thrust.bounds.x + hud.thrust.bounds.width * 0.5f,
                    hud.thrust.bounds.y + hud.thrust.bounds.height + 26f * u,
                    0.68f * u, Visuals.TEXT_DIM);
        }
        batch.end();
    }

    float fuelBarW(float w) {
        float pad = 16f * u;
        return w - 2f * pad - 44f * u;
    }

    void drawFuelBar(float w, float h, float panelH) {
        float pad = 16f * u;
        float barX = pad + 44f * u;
        float barY = h - 120f * u;
        float barH = 20f * u;
        float barW = fuelBarW(w);
        float frac = world.ship.maxFuel > 0f ? MathUtils.clamp(world.ship.fuel / world.ship.maxFuel, 0f, 1f) : 0f;

        ui.drawFuelIcon(pad + 18f * u, barY + barH * 0.5f, 12f * u);
        ui.panel(barX, barY, barW, barH, barH * 0.5f,
                FUEL_TRACK_C,
                FUEL_EDGE_C);
        if (frac > 0.001f) {
            Color fill;
            if (frac > 0.5f) {
                fill = tint.set(0.16f, 0.82f, 0.48f, 1f);
            } else if (frac > 0.25f) {
                fill = tint.set(1f, 0.70f, 0.22f, 1f);
            } else {
                float pulse = 0.75f + 0.25f * MathUtils.sin(hud.time * 9f);
                fill = tint.set(1f, 0.30f, 0.34f, 1f).mul(pulse, pulse, pulse, 1f);
            }
            float inset = 4f * u;
            float fw = Math.max(barH - inset * 2f, (barW - inset * 2f) * frac);
            shapes.setColor(fill);
            Visuals.roundRect(shapes, barX + inset, barY + inset,
                    fw, barH - inset * 2f, (barH - inset * 2f) * 0.5f);
            shapes.setColor(1f, 1f, 1f, 0.20f);
            Visuals.roundRect(shapes, barX + inset + 2f * u, barY + barH * 0.55f,
                    Math.max(0f, fw - 4f * u), barH * 0.28f, barH * 0.14f);
        }
        // Graduacion cada 10 %: lectura de instrumento, no barra decorativa.
        for (int i = 1; i < 10; i++) {
            shapes.setColor(0f, 0f, 0f, 0.30f);
            shapes.rect(barX + barW * i / 10f, barY + 4f * u, 1.2f * u, barH - 8f * u);
        }
        shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                Visuals.STEEL_LIGHT.b, 0.35f);
        shapes.rect(barX + barW - 3f * u, barY + 4f * u, 3f * u, barH - 8f * u);
    }

    /** El capacitor solo se muestra cuando el láser está en uso o acaba de minar. */
    boolean energyVisible() {
        return hud.runActive && (world.ship.laser.active
                || world.ship.tractor.active()
                || world.ship.capacitor.fraction() < 0.999f);
    }

    float energyBarX() {
        return 16f * u + 30f * u;
    }

    float energyBarY(float h) {
        return h - 244f * u;
    }

    float energyBarW() {
        return 110f * u;
    }

    Color energyColor() {
        float frac = world.ship.capacitor.fraction();
        if (frac > 0.5f) {
            return Visuals.INFO;
        }
        return frac > 0.25f ? Visuals.WARN : Visuals.DANGER;
    }

    void drawEnergyBar(float w, float h) {
        if (!energyVisible()) {
            return;
        }
        float x = energyBarX();
        float y = energyBarY(h);
        float bw = energyBarW();
        float bh = 6f * u;
        float frac = MathUtils.clamp(world.ship.capacitor.fraction(), 0f, 1f);
        ui.panel(x, y, bw, bh, bh * 0.5f, FUEL_TRACK_C, FUEL_EDGE_C);
        if (frac > 0.001f) {
            shapes.setColor(energyColor());
            Visuals.roundRect(shapes, x + 1.5f * u, y + 1.5f * u,
                    Math.max(bh - 3f * u, (bw - 3f * u) * frac), bh - 3f * u,
                    (bh - 3f * u) * 0.5f);
        }
        // Rayo del icono.
        float bx = energyBarX() - 13f * u;
        float by = y + bh * 0.5f;
        ui.drawEnergyIcon(bx, by, 6f * u, energyColor());
    }

    void drawEnergyText(float w, float h) {
        if (!energyVisible()) {
            return;
        }
        int pct = Math.round(world.ship.capacitor.fraction() * 100f);
        String label = world.ship.tractor.active()
                ? "ENERGIA " + pct + "%  TRACTOR " + world.ship.tractor.targets.size
                : "ENERGIA " + pct + "%";
        ui.textLeft(label,
                energyBarX() + energyBarW() + 8f * u, energyBarY(h) + 6f * u,
                0.56f * u, energyColor());
    }

    float hullChipW() {
        return 104f * u;
    }

    float hullChipH() {
        return 46f * u;
    }

    float hullChipX(float w) {
        return hud.pause.bounds.x - 12f * u - hullChipW();
    }

    float hullChipY(float h) {
        return h - 89f * u;
    }

    void drawHullChip(float w, float h) {
        float x = hullChipX(w);
        float y = hullChipY(h);
        float cw = hullChipW();
        float ch = hullChipH();
        float frac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        ui.panel(x, y, cw, ch, ch * 0.34f, COIN_CHIP_C, Visuals.PANEL_EDGE);
        drawShield(x + 18f * u, y + ch * 0.60f, 9f * u, UiKit.partColor(frac));
        ui.drawIntegrityBar(x + 32f * u, y + 3f * u, cw - 40f * u,
                Math.max(3.5f * u, ch * 0.09f), frac);
        for (int i = 1; i < 4; i++) {
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.rect(x + 32f * u + (cw - 40f * u) * i / 4f, y + 3f * u,
                    1.2f * u, Math.max(3.5f * u, ch * 0.09f));
        }
    }

    float survivalBarH() {
        return 18f * u;
    }

    float survivalGap() {
        return 10f * u;
    }

    float survivalBarW(float w) {
        return (w - 32f * u - survivalGap()) * 0.5f;
    }

    float survivalX1() {
        return 16f * u;
    }

    float survivalX2(float w) {
        return survivalX1() + survivalBarW(w) + survivalGap();
    }

    float survivalY(float h) {
        // Bajo la fila de carga/mods; el panel superior mide 172 u.
        return h - 162f * u;
    }

    void drawSurvivalBars(float w, float h) {
        float y = survivalY(h);
        float bh = survivalBarH();
        float bw = survivalBarW(w);
        float x1 = survivalX1();
        float x2 = survivalX2(w);
        float o2 = MathUtils.clamp(session.pilot.oxygenFraction(), 0f, 1f);
        float hp = MathUtils.clamp(session.pilot.healthFraction(), 0f, 1f);
        drawSurvivalBar(x1, y, bw, bh, o2, o2Color(o2), true);
        drawSurvivalBar(x2, y, bw, bh, hp, healthColor(hp), false);
    }

    void drawSurvivalBar(float x, float y, float w, float h, float frac,
                                 Color fill, boolean oxygen) {
        boolean leaking = oxygen && (session.lifeSupport.leak > 0.01f
                || session.lifeSupport.breach);
        ui.panel(x, y, w, h, h * 0.5f, COIN_CHIP_C,
                leaking ? Visuals.DANGER : Visuals.PANEL_EDGE);
        if (frac > 0.001f) {
            float inset = 3f * u;
            float fw = Math.max(h - inset * 2f, (w - inset * 2f) * frac);
            shapes.setColor(fill);
            Visuals.roundRect(shapes, x + inset, y + inset,
                    fw, h - inset * 2f, (h - inset * 2f) * 0.5f);
            shapes.setColor(1f, 1f, 1f, 0.18f);
            Visuals.roundRect(shapes, x + inset + 2f * u, y + h * 0.56f,
                    Math.max(0f, fw - 4f * u), h * 0.24f, h * 0.12f);
        }
        for (int i = 1; i < 4; i++) {
            shapes.setColor(0f, 0f, 0f, 0.28f);
            shapes.rect(x + w * i / 4f, y + 3f * u, 1.2f * u, h - 6f * u);
        }
        float iconX = x + h * 0.62f;
        float iconY = y + h * 0.5f;
        if (oxygen) {
            drawO2Icon(iconX, iconY, h * 0.24f, fill);
        } else {
            drawHealthIcon(iconX, iconY, h * 0.24f, fill);
        }
    }

    Color o2Color(float frac) {
        if (session.pilot.o2AlertLevel() == 2) {
            return Visuals.DANGER;
        }
        if (session.pilot.o2AlertLevel() == 1) {
            return Visuals.WARN;
        }
        return Visuals.O2;
    }

    Color healthColor(float frac) {
        return frac > 0.55f ? Visuals.HEALTH : frac > 0.28f ? Visuals.WARN : Visuals.DANGER;
    }

    void drawO2Icon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.circle(cx, cy, r, 10);
        shapes.setColor(0.02f, 0.06f, 0.11f, 1f);
        shapes.circle(cx, cy, r * 0.46f, 8);
    }

    void drawHealthIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.rect(cx - r * 0.22f, cy - r, r * 0.44f, r * 2f);
        shapes.rect(cx - r, cy - r * 0.22f, r * 2f, r * 0.44f);
    }

    void drawSurvivalText(float w, float h) {
        float y = survivalY(h) + survivalBarH() * 0.5f;
        float x1 = survivalX1();
        float x2 = survivalX2(w);
        int o2Pct = Math.round(session.pilot.oxygenFraction() * 100f);
        int hpPct = Math.round(session.pilot.healthFraction() * 100f);
        ui.textLeftMiddle("O2 " + o2Pct + "%", x1 + 26f * u, y, 0.66f * u, Color.WHITE);
        ui.textLeftMiddle("HP " + hpPct + "%", x2 + 26f * u, y, 0.66f * u, Color.WHITE);
        if (session.lifeSupport.breach) {
            ui.textLeftMiddle("BRECHA", x1 + survivalBarW(w) - 72f * u, y,
                    0.58f * u, Visuals.DANGER);
        } else if (session.lifeSupport.leak > 0.01f) {
            ui.textLeftMiddle("FUGA", x1 + survivalBarW(w) - 56f * u, y,
                    0.58f * u, Visuals.WARN);
        }
    }

    void drawShield(float cx, float cy, float r, Color c) {        shapes.setColor(0.03f, 0.06f, 0.12f, 0.95f);
        shieldShape(cx, cy, r * 1.18f);
        shapes.setColor(c);
        shieldShape(cx, cy, r);
        shapes.setColor(1f, 1f, 1f, 0.35f);
        shapes.circle(cx - r * 0.30f, cy + r * 0.30f, r * 0.20f, 8);
    }

    void shieldShape(float cx, float cy, float r) {
        shapes.rect(cx - r * 0.78f, cy - r * 0.05f, r * 1.56f, r * 0.85f);
        shapes.triangle(cx - r * 0.78f, cy + r * 0.20f,
                cx + r * 0.78f, cy + r * 0.20f,
                cx, cy - r * 1.10f);
    }

    float alertW() {
        return 168f * u;
    }

    float alertH() {
        return 22f * u;
    }

    float alertX(float w) {
        return w - 16f * u - alertW();
    }

    float alertY(float h, float panelH, int slot) {
        return h - panelH - 32f * u - slot * 26f * u;
    }

    /** Avisos de los sistemas danados (el casco tiene su propio indicador). */
    void drawPartAlerts(float w, float h, float panelH) {
        int slot = 0;
        for (int i = 1; i < Ship.PART_COUNT; i++) {
            float frac = world.ship.part[i] / world.ship.partMax(i);
            if (frac >= 0.72f) {
                continue;
            }
            float x = alertX(w);
            float y = alertY(h, panelH, slot);
            ui.panel(x, y, alertW(), alertH(), alertH() * 0.45f, COIN_CHIP_C, Visuals.PANEL_EDGE);
            shapes.setColor(UiKit.partColor(frac));
            Visuals.roundRect(shapes, x + 4f * u, y + 4f * u, 4f * u, alertH() - 8f * u, 2f * u);
            ui.drawAlertIcon(x + 18f * u, y + alertH() * 0.5f, 6f * u, UiKit.partColor(frac));
            slot++;
        }
    }

    void drawIconButton(Button b) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = 5f * u;
        boolean pressed = ui.pressed != null && ui.pressed.is(b);
        ui.panel(x, y, w, h, r,
                pressed ? tint2.set(Visuals.BTN_PRIMARY).lerp(Color.WHITE, 0.12f) : Visuals.BTN_SECONDARY,
                Visuals.PANEL_EDGE);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.75f);
        shapes.rect(x + 5f * u, y + 6f * u, 2.5f * u, h - 12f * u);
        float bw = w * 0.09f;
        float bh = h * 0.38f;
        shapes.setColor(0.88f, 0.90f, 0.88f, 0.95f);
        Visuals.roundRect(shapes, x + w * 0.5f - bw * 1.7f, y + h * 0.5f - bh * 0.5f, bw, bh, 1f * u);
        Visuals.roundRect(shapes, x + w * 0.5f + bw * 0.7f, y + h * 0.5f - bh * 0.5f, bw, bh, 1f * u);
    }

    void drawControls() {
        drawJoystick();
        drawThrottle();
    }

    /** Joystick reutilizable por la partida y por los interiores. */
    void drawJoystick() {
        shapes.setColor(0.02f, 0.03f, 0.04f, 0.55f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR + 2f * u, 40);
        shapes.setColor(Visuals.STEEL.r, Visuals.STEEL.g, Visuals.STEEL.b, 0.34f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR, 40);
        shapes.setColor(0.045f, 0.065f, 0.085f, 0.88f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR - 2.5f * u, 40);
        shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                Visuals.STEEL_LIGHT.b, 0.20f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR * 0.70f, 32);
        // Ocho marcas de rumbo.
        for (int i = 0; i < 8; i++) {
            float a = i * 45f;
            float len = i % 2 == 0 ? 6f * u : 3.5f * u;
            shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                    Visuals.STEEL_LIGHT.b, 0.42f);
            shapes.circle(hud.joyCx + MathUtils.cosDeg(a) * hud.joyR * 0.86f,
                    hud.joyCy + MathUtils.sinDeg(a) * hud.joyR * 0.86f,
                    len * 0.35f, 6);
        }
        if (hud.joyAiming) {
            shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.30f);
            shapes.rectLine(hud.joyCx, hud.joyCy, hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, 3f * u);
        }
        Color knob = hud.joyAiming ? Visuals.ACCENT : Visuals.STEEL_LIGHT;
        shapes.setColor(0.02f, 0.03f, 0.04f, 0.85f);
        shapes.circle(hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 0.47f, 28);
        shapes.setColor(knob.r * 0.55f, knob.g * 0.55f, knob.b * 0.55f, 1f);
        shapes.circle(hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 0.38f, 24);
        shapes.setColor(knob);
        shapes.circle(hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 0.29f, 20);
        shapes.setColor(1f, 1f, 1f, 0.45f);
        shapes.circle(hud.joyCx + hud.knobX - hud.joyR * 0.08f,
                hud.joyCy + hud.knobY + hud.joyR * 0.08f, hud.joyR * 0.08f, 8);
    }

    /** Solo joystick y su etiqueta: se usa en los interiores. */
    void drawJoystickOnly(float w, float h) {
        beginShapes();
        drawJoystick();
        shapes.end();
        batch.begin();
        ui.textCenteredMiddle("MOVER", hud.joyCx, hud.joyCy + hud.joyR + 28f * u,
                0.68f * u, Visuals.TEXT_DIM);
        batch.end();
    }

    void drawThrottle() {
        // Barra de aceleración.
        float x = hud.thrust.bounds.x;
        float y = hud.thrust.bounds.y;
        float bw = hud.thrust.bounds.width;
        float bh = hud.thrust.bounds.height;
        float max = world.up.maxSpeed();
        float speedFrac = max > 0f ? MathUtils.clamp(world.ship.vel.len() / max, 0f, 1f) : 0f;
        ui.panel(x, y, bw, bh, bw * 0.30f,
                CONTROL_TRACK_C,
                CONTROL_EDGE_C);
        if (hud.throttle > 0.01f) {
            shapes.setColor(0.42f, 0.29f, 0.12f, 0.42f);
            Visuals.roundRect(shapes, x + 4f * u, y + 4f * u,
                    bw - 8f * u, (bh - 8f * u) * hud.throttle, bw * 0.26f);
            shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.55f);
            shapes.rect(x + 8f * u, y + 8f * u, bw - 16f * u, (bh - 16f * u) * hud.throttle);
        }
        // Marcas de nivel.
        for (int i = 1; i < 5; i++) {
            float ly = y + bh * i / 5f;
            shapes.setColor(1f, 1f, 1f, i == 5 ? 0.10f : 0.16f);
            shapes.rect(x + bw * 0.18f, ly, bw * 0.64f, 1.5f * u);
        }
        // Marca blanca de velocidad actual.
        shapes.setColor(1f, 1f, 1f, 0.90f);
        shapes.rect(x - 6f * u, y + bh * speedFrac - 2f * u, bw + 12f * u, 3f * u);
        shapes.setColor(Visuals.ACCENT);
        shapes.circle(x + bw * 0.5f, y + bh * speedFrac, 3.5f * u, 10);
        // Nudo del acelerador.
        float knobYLocal = y + bh * hud.throttle;
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.circle(x + bw * 0.5f, knobYLocal, bw * 0.34f, 20);
        shapes.setColor(Visuals.STEEL_LIGHT.r * 0.55f, Visuals.STEEL_LIGHT.g * 0.55f,
                Visuals.STEEL_LIGHT.b * 0.55f, 1f);
        shapes.circle(x + bw * 0.5f, knobYLocal, bw * 0.27f, 16);
        shapes.setColor(Visuals.ACCENT);
        shapes.circle(x + bw * 0.5f, knobYLocal, bw * 0.19f, 14);
    }

    /** Datos de la brújula para telemetría y pruebas de aceptación. */
    String compassLabel() { return compassLabel; }
    float compassAngle() { return compassRel; }
    float compassDistance() { return compassDist; }
    boolean compassBehind() { return compassBehind; }
    boolean compassHere() { return compassHere; }
    boolean compassLocal() { return compassLocal; }

    /** Barra horizontal de rumbo a la nodriza más cercana (UPD-006-T08). */
    private void updateCompass() {
        compassHas = false;
        compassBehind = false;
        compassHere = false;
        compassLocal = true;
        compassLabel = "";
        if (world == null || world.ship == null || world.station == null
                || session == null) {
            return;
        }
        Station local = world.station;
        boolean localMother = isMothership(local.def);
        SectorDef remote = localMother ? null : nearestMothershipSector();
        float tx;
        float ty;
        if (localMother) {
            tx = local.pos.x;
            ty = local.pos.y;
            compassLabel = "NODRIZA";
            compassLocal = true;
        } else if (remote != null && !remote.id.equals(
                session.currentSector == null ? "" : session.currentSector.id)) {
            tx = local.pos.x;
            ty = local.pos.y;
            compassLabel = "NAV " + remote.name;
            compassLocal = false;
        } else if (remote != null) {
            tx = local.pos.x;
            ty = local.pos.y;
            compassLabel = remote.name;
            compassLocal = true;
        } else {
            tx = local.pos.x;
            ty = local.pos.y;
            compassLabel = local.def.name;
            compassLocal = true;
        }
        float dx = tx - world.ship.pos.x;
        float dy = ty - world.ship.pos.y;
        compassDist = (float) Math.sqrt(dx * dx + dy * dy);
        float absolute = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        compassRel = Ship.wrapDeg(absolute - world.ship.heading);
        compassBehind = Math.abs(compassRel) > 90f;
        compassHere = compassDist < 60f;
        compassHas = true;
    }

    private static boolean isMothership(StationDef def) {
        return def != null && def.name != null && def.name.startsWith("NODRIZA");
    }

    /** Sector descubierto con nodriza más cercano por distancia de mapa. */
    private SectorDef nearestMothershipSector() {
        if (session == null || session.universe == null) {
            return null;
        }
        SectorDef best = null;
        float bestDist = Float.MAX_VALUE;
        for (GalaxyDef galaxy : session.universe.galaxies()) {
            for (SystemDef system : galaxy.systems) {
                for (SectorDef sector : system.sectors) {
                    if (!session.universe.isDiscovered(sector.id)) {
                        continue;
                    }
                    boolean mother = false;
                    for (int i = 0; i < sector.stationCount(); i++) {
                        if (isMothership(sector.station(i))) {
                            mother = true;
                            break;
                        }
                    }
                    if (!mother) {
                        continue;
                    }
                    float d = session.currentSector == null ? 0f
                            : session.currentSector.distanceTo(sector);
                    if (d < bestDist) {
                        bestDist = d;
                        best = sector;
                    }
                }
            }
        }
        return best;
    }

    private float compassBarW(float w) {
        return Math.min(w * 0.66f, 300f * u);
    }

    private float compassBarX(float w) {
        return (w - compassBarW(w)) * 0.5f;
    }

    private float compassBarY(float h) {
        return 16f * u + 126f * u;
    }

    private void drawCompassShapes(float w, float h) {
        if (!compassHas || hud.state != MyGame.PLAYING) {
            return;
        }
        float x = compassBarX(w);
        float y = compassBarY(h);
        float bw = compassBarW(w);
        float bh = 22f * u;
        ui.panel(x, y, bw, bh, bh * 0.5f, COMPASS_PANEL, Visuals.PANEL_EDGE);
        // Marcas laterales y centro.
        for (int i = 1; i < 8; i++) {
            shapes.setColor(1f, 1f, 1f, i == 4 ? 0.35f : 0.16f);
            shapes.rect(x + bw * i / 8f, y + 4f * u, 1.2f * u, bh - 8f * u);
        }
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.75f);
        shapes.rect(x + bw * 0.5f - 1f * u, y + 2f * u, 2f * u, bh - 4f * u);

        float t = compassHere ? 0f : MathUtils.sinDeg(compassRel);
        if (compassBehind) {
            t = compassRel > 0f ? 1f : -1f;
        }
        float dotX = x + bw * 0.5f + t * bw * 0.45f;
        Color dot = compassHere ? Visuals.GOOD
                : compassLocal ? Visuals.ACCENT : Visuals.WARN;
        shapes.setColor(dot);
        shapes.circle(dotX, y + bh * 0.5f, 4.4f * u, 12);
        shapes.setColor(1f, 1f, 1f, 0.55f);
        shapes.circle(dotX, y + bh * 0.5f, 1.4f * u, 8);
        if (compassBehind) {
            shapes.setColor(Visuals.WARN);
            float ex = compassRel > 0f ? x + bw - 6f * u : x + 2f * u;
            shapes.triangle(ex, y + bh * 0.5f + 5f * u,
                    ex, y + bh * 0.5f - 5f * u,
                    compassRel > 0f ? ex + 4f * u : ex - 4f * u, y + bh * 0.5f);
        }
    }

    private void drawCompassText(float w, float h) {
        if (!compassHas || hud.state != MyGame.PLAYING) {
            return;
        }
        float x = compassBarX(w);
        float y = compassBarY(h);
        float bw = compassBarW(w);
        String label = compassLabel;
        if (compassHere) {
            label = "AQUI  " + label;
        } else if (compassBehind) {
            label = "DETRAS  " + label;
        }
        ui.textLeftFit(label, x + 8f * u, y + 30f * u, 0.58f * u,
                bw * 0.60f, Visuals.TEXT_DIM);
        ui.textRight(Math.round(compassDist) + " u", x + bw - 8f * u,
                y + 30f * u, 0.58f * u, Visuals.TEXT_DIM);
        if (hud.hasNavigation && hud.navTarget != null) {
            drawAdvancedCompassText(w, h);
        }
    }

    /** Marca del objetivo avanzado sobre la barra (pasada de formas). */
    private void drawAdvancedCompassMarker(float w, float h) {
        NavTarget target = hud.navTarget;
        if (!hud.hasNavigation || target == null) {
            return;
        }
        target.sync();
        float dx = target.pos.x - world.ship.pos.x;
        float dy = target.pos.y - world.ship.pos.y;
        float abs = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float rel = Ship.wrapDeg(abs - world.ship.heading);
        boolean behind = Math.abs(rel) > 90f;
        float bw = compassBarW(w);
        float t = behind ? (rel > 0f ? 1f : -1f) : MathUtils.sinDeg(rel);
        shapes.setColor(Visuals.GOLD.r, Visuals.GOLD.g, Visuals.GOLD.b, 0.85f);
        shapes.circle(compassBarX(w) + bw * 0.5f + t * bw * 0.45f,
                compassBarY(h) - 5f * u, 2.4f * u, 8);
    }

    /** Brújula avanzada: solo con módulo NAVEGACION y objetivo conocido. */
    private void drawAdvancedCompassText(float w, float h) {
        NavTarget target = hud.navTarget;
        if (target == null) {
            return;
        }
        target.sync();
        float dx = target.pos.x - world.ship.pos.x;
        float dy = target.pos.y - world.ship.pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float abs = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float rel = Ship.wrapDeg(abs - world.ship.heading);
        boolean behind = Math.abs(rel) > 90f;
        float y = compassBarY(h) + 66f * u;
        ui.textLeftFit("OBJETIVO  " + target.label, compassBarX(w),
                y, 0.62f * u, compassBarW(w) * 0.72f, Visuals.GOLD);
        ui.textRight((behind ? "DETRAS " : "") + Math.round(dist) + " u",
                compassBarX(w) + compassBarW(w), y, 0.62f * u,
                behind ? Visuals.WARN : Visuals.TEXT);
    }

    /** Datos de la maniobra de atraque manual (UPD-006-T06). */
    private void drawDockGuideShapes(float w, float h) {
        if (!hud.dockGuide || hud.state != MyGame.PLAYING) {
            return;
        }
        float bw = Math.min(w * 0.82f, 360f * u);
        float x = (w - bw) * 0.5f;
        float y = compassBarY(h) + 96f * u;
        ui.panel(x, y, bw, 58f * u, 6f * u, COMPASS_PANEL,
                hud.inDockCorridor ? Visuals.GOOD : Visuals.PANEL_EDGE);
        // Indicador lateral: corredor y desviación.
        float cx = x + bw * 0.5f;
        shapes.setColor(0.20f, 0.30f, 0.40f, 0.55f);
        shapes.rect(x + 10f * u, y + 10f * u, bw - 20f * u, 2f * u);
        float half = 90f;
        float lat = MathUtils.clamp(hud.dockLateral, -half, half);
        float lx = cx + lat / half * (bw * 0.42f);
        shapes.setColor(hud.inDockCorridor ? Visuals.GOOD : Visuals.WARN);
        shapes.circle(lx, y + 11f * u, 4f * u, 10);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b, 0.7f);
        shapes.rect(cx - 1f * u, y + 5f * u, 2f * u, 12f * u);
    }

    private void drawDockGuideText(float w, float h) {
        if (!hud.dockGuide || hud.state != MyGame.PLAYING) {
            return;
        }
        float bw = Math.min(w * 0.82f, 360f * u);
        float x = (w - bw) * 0.5f;
        float y = compassBarY(h) + 96f * u;
        ui.textLeftFit("ATRAQUE  D " + Math.round(hud.dockDistance) + " u",
                x + 10f * u, y + 44f * u, 0.62f * u, bw * 0.44f, Visuals.ACCENT);
        ui.textRight("V " + Math.round(hud.dockSpeed) + "  ANG "
                        + Math.round(hud.dockAngle) + "  LAT "
                        + (hud.dockLateral >= 0f ? "+" : "")
                        + Math.round(hud.dockLateral),
                x + bw - 10f * u, y + 44f * u, 0.58f * u,
                hud.inDockCorridor ? Visuals.GOOD : Visuals.TEXT_DIM);
        ui.textCenteredFit(hud.inDockCorridor ? "EN CORREDOR" : "FUERA DE CORREDOR",
                x + bw * 0.5f, y + 27f * u, 0.54f * u, bw - 20f * u,
                hud.inDockCorridor ? Visuals.GOOD : Visuals.WARN);
    }

    void drawHudGlow(float w, float h) {        batch.begin();
        Visuals.additive(batch);
        if (hud.state == MyGame.PLAYING) {
            art.glow(batch, hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 1.15f, Visuals.ACCENT, 0.10f);
            float x = hud.thrust.bounds.x + hud.thrust.bounds.width * 0.5f;
            float knobYLocal = hud.thrust.bounds.y + hud.thrust.bounds.height * hud.throttle;
            art.glow(batch, x, knobYLocal, hud.thrust.bounds.width * 0.85f, Visuals.ACCENT, 0.12f);
        }
        float barX = 16f * u + 44f * u;
        float barY = h - 120f * u;
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        if (frac < 0.25f) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(hud.time * 9f);
            art.glow(batch, barX + fuelBarW(w) * 0.5f, barY + 10f * u, 140f * u,
                    Visuals.DANGER, 0.10f * pulse);
        }
        float hullFrac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        if (hullFrac < 0.35f) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(hud.time * 9f);
            art.glow(batch, hullChipX(w) + hullChipW() * 0.5f, hullChipY(h) + hullChipH() * 0.5f,
                    hullChipW() * 1.3f, Visuals.DANGER, 0.12f * pulse);
        }
        batch.end();
        Visuals.normal(batch);
    }

}
