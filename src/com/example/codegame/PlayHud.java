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

    private static final Color PANEL_BOTTOM_C = new Color(0.020f, 0.035f, 0.085f, 0.88f);
    private static final Color PANEL_TOP_C = new Color(0.070f, 0.110f, 0.230f, 0.93f);
    private static final Color FUEL_TRACK_C = new Color(0.03f, 0.05f, 0.10f, 0.95f);
    private static final Color FUEL_EDGE_C = new Color(0.22f, 0.38f, 0.58f, 0.55f);
    private static final Color CONTROL_TRACK_C = new Color(0.04f, 0.07f, 0.14f, 0.62f);
    private static final Color CONTROL_EDGE_C = new Color(0.28f, 0.55f, 0.85f, 0.30f);
    private static final Color COIN_CHIP_C = new Color(0.08f, 0.09f, 0.16f, 0.92f);
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
        return 112f * u;
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
        shapes.setColor(0.30f, 0.65f, 1f, 0.45f);
        shapes.rect(0f, h - panelH, w, 2f * u);
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.rect(0f, h - panelH - 4f * u, w, 4f * u);

        drawFuelBar(w, h, panelH);
        drawEnergyBar(w, h);
        drawHullChip(w, h);
        drawSurvivalBars(w, h);
        drawPartAlerts(w, h, panelH);
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
        if (hud.canBuildBase) {
            ui.drawButton(hud.buildBase, Visuals.BTN_PRIMARY, true);
        }
        drawIconButton(hud.pause);
        ui.drawCoinIcon(pad + 18f * u, h - 54f * u, 13f * u);
        if (hud.state == MyGame.PLAYING) {
            drawControls();
        }
        shapes.end();

        drawHudGlow(w, h);

        batch.begin();
        ui.textLeft(String.valueOf(upgrades.coins() + world.coinsEarned),
                pad + 38f * u, h - 44f * u, 1.15f * u, Visuals.GOLD);
        ui.textCentered("OLEADA " + world.wave, w * 0.38f, h - 44f * u, 0.82f * u, Visuals.TEXT);
        float hullFrac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        float hx = hullChipX(w);
        float hy = hullChipY(h);
        ui.textLeft("CASCO", hx + 34f * u, hy + 31f * u, 0.55f * u, Visuals.TEXT_DIM);
        ui.textLeft(Math.round(hullFrac * 100f) + "%", hx + 34f * u, hy + 17f * u,
                0.78f * u, UiKit.partColor(hullFrac));
        ui.textLeftMiddle("PUNTOS " + world.score, pad + 44f * u + fuelBarW(w) + 14f * u,
                h - 86f * u, 0.78f * u, Visuals.TEXT_DIM);
        ui.textLeft("CARGA " + Math.round(world.ship.cargo().usedMass()) + "/"
                        + Math.round(world.ship.cargo().capacity()) + " UM",
                pad + 8f * u, h - 100f * u, 0.60f * u, Visuals.TEXT_DIM);
        ui.textLeft("MODS " + upgrades.shipModuleCount() + "/"
                        + upgrades.loadout().moduleSlots(),
                pad + 8f * u, h - 114f * u, 0.56f * u, Visuals.TEXT_OFF);
        drawEnergyText(w, h);
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        ui.textCenteredMiddle(Math.round(frac * 100f) + "%",
                pad + 44f * u + fuelBarW(w) * 0.5f, h - 86f * u, 0.78f * u, Color.WHITE);
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
                    pad + 8f * u, h - 154f * u, 0.78f * u, Visuals.DANGER);
        } else if (o2Alert == 1) {
            ui.textLeft("O2 BAJO " + o2Pct + "%",
                    pad + 8f * u, h - 154f * u, 0.78f * u, Visuals.WARN);
        }
        if (session.lifeSupport.leak > 0.01f || session.lifeSupport.breach) {
            ui.textLeft(session.lifeSupport.breach ? "BRECHA O2" : "FUGA O2",
                    pad + 8f * u, h - 176f * u, 0.78f * u,
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
                    ax + 14f * u, ay + 15f * u, 0.68f * u, UiKit.partColor(pf));
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
        return w - 2f * pad - 44f * u - 80f * u;
    }

    void drawFuelBar(float w, float h, float panelH) {
        float pad = 16f * u;
        float barX = pad + 44f * u;
        float barY = h - 96f * u;
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
        return h - 110f * u;
    }

    float energyBarW() {
        return 110f * u;
    }

    Color energyColor() {
        float frac = world.ship.capacitor.fraction();
        if (frac > 0.5f) {
            return Visuals.ACCENT;
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
        shapes.setColor(energyColor());
        shapes.triangle(bx - 2f * u, by + 5f * u, bx + 4f * u, by + 1f * u,
                bx - 1f * u, by + 1f * u);
        shapes.triangle(bx + 2f * u, by - 5f * u, bx - 4f * u, by - 1f * u,
                bx + 1f * u, by - 1f * u);
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
        return 96f * u;
    }

    float hullChipH() {
        return 40f * u;
    }

    float hullChipX(float w) {
        return hud.pause.bounds.x - 12f * u - hullChipW();
    }

    float hullChipY(float h) {
        return h - 74f * u;
    }

    void drawHullChip(float w, float h) {
        float x = hullChipX(w);
        float y = hullChipY(h);
        float cw = hullChipW();
        float ch = hullChipH();
        float frac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        ui.panel(x, y, cw, ch, ch * 0.34f, COIN_CHIP_C, Visuals.PANEL_EDGE);
        drawShield(x + 18f * u, y + ch * 0.60f, 9f * u, UiKit.partColor(frac));
        ui.drawIntegrityBar(x + 34f * u, y + 6f * u, cw - 44f * u,
                Math.max(4f * u, ch * 0.15f), frac);
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
        // Justo debajo del panel superior (112 u de alto).
        return h - 130f * u;
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
            slot++;
        }
    }

    void drawIconButton(Button b) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = h * 0.30f;
        boolean pressed = ui.pressed != null && ui.pressed.is(b);
        ui.panel(x, y, w, h, r,
                pressed ? tint2.set(Visuals.BTN_PRIMARY).lerp(Color.WHITE, 0.12f) : Visuals.BTN_SECONDARY,
                Visuals.PANEL_EDGE);
        float bw = w * 0.10f;
        float bh = h * 0.42f;
        shapes.setColor(0.85f, 0.95f, 1f, 0.95f);
        Visuals.roundRect(shapes, x + w * 0.5f - bw * 1.6f, y + h * 0.5f - bh * 0.5f, bw, bh, bw * 0.5f);
        Visuals.roundRect(shapes, x + w * 0.5f + bw * 0.6f, y + h * 0.5f - bh * 0.5f, bw, bh, bw * 0.5f);
    }

    void drawControls() {
        // Joystick: base, anillo, dirección y nudo.
        shapes.setColor(0.10f, 0.18f, 0.35f, 0.28f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR, 40);
        shapes.setColor(0.30f, 0.65f, 1f, 0.30f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR, 40);
        shapes.setColor(0.06f, 0.10f, 0.22f, 0.55f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR - 3f * u, 40);
        shapes.setColor(0.30f, 0.65f, 1f, 0.18f);
        shapes.circle(hud.joyCx, hud.joyCy, hud.joyR * 0.72f, 32);
        // Cuatro marcas cardinales.
        for (int i = 0; i < 4; i++) {
            float a = i * 90f;
            shapes.setColor(0.55f, 0.85f, 1f, 0.45f);
            shapes.circle(hud.joyCx + MathUtils.cosDeg(a) * hud.joyR * 0.84f,
                    hud.joyCy + MathUtils.sinDeg(a) * hud.joyR * 0.84f, 2.2f * u, 5);
        }
        if (hud.joyAiming) {
            shapes.setColor(0.36f, 0.86f, 1f, 0.30f);
            shapes.rectLine(hud.joyCx, hud.joyCy, hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, 4f * u);
        }
        shapes.setColor(0.10f, 0.30f, 0.55f, 0.95f);
        shapes.circle(hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 0.46f, 28);
        shapes.setColor(0.36f, 0.80f, 1f, 0.95f);
        shapes.circle(hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 0.38f, 24);
        shapes.setColor(0.80f, 0.96f, 1f, 0.65f);
        shapes.circle(hud.joyCx + hud.knobX - hud.joyR * 0.10f, hud.joyCy + hud.knobY + hud.joyR * 0.10f, hud.joyR * 0.12f, 10);

        // Barra de aceleración.
        float x = hud.thrust.bounds.x;
        float y = hud.thrust.bounds.y;
        float bw = hud.thrust.bounds.width;
        float bh = hud.thrust.bounds.height;
        float max = world.up.maxSpeed();
        float speedFrac = max > 0f ? MathUtils.clamp(world.ship.vel.len() / max, 0f, 1f) : 0f;
        ui.panel(x, y, bw, bh, bw * 0.46f,
                CONTROL_TRACK_C,
                CONTROL_EDGE_C);
        if (hud.throttle > 0.01f) {
            shapes.setColor(0.14f, 0.52f, 0.82f, 0.30f);
            Visuals.roundRect(shapes, x + 4f * u, y + 4f * u,
                    bw - 8f * u, (bh - 8f * u) * hud.throttle, bw * 0.40f);
            shapes.setColor(0.35f, 0.86f, 1f, 0.38f);
            shapes.rect(x + 7f * u, y + 7f * u, bw - 14f * u, (bh - 14f * u) * hud.throttle);
        }
        // Marcas de nivel.
        for (int i = 1; i < 5; i++) {
            float ly = y + bh * i / 5f;
            shapes.setColor(1f, 1f, 1f, i == 5 ? 0.10f : 0.16f);
            shapes.rect(x + bw * 0.18f, ly, bw * 0.64f, 1.5f * u);
        }
        // Marca blanca de velocidad actual.
        shapes.setColor(1f, 1f, 1f, 0.90f);
        shapes.rect(x - 6f * u, y + bh * speedFrac - 2f * u, bw + 12f * u, 4f * u);
        shapes.setColor(1f, 0.75f, 0.35f, 0.90f);
        shapes.circle(x + bw * 0.5f, y + bh * speedFrac, 4f * u, 10);
        // Nudo del acelerador.
        float knobYLocal = y + bh * hud.throttle;
        shapes.setColor(0.10f, 0.30f, 0.55f, 0.95f);
        shapes.circle(x + bw * 0.5f, knobYLocal, bw * 0.30f, 20);
        shapes.setColor(0.36f, 0.80f, 1f, 0.95f);
        shapes.circle(x + bw * 0.5f, knobYLocal, bw * 0.22f, 16);
    }

    void drawHudGlow(float w, float h) {
        batch.begin();
        Visuals.additive(batch);
        if (hud.state == MyGame.PLAYING) {
            art.glow(batch, hud.joyCx + hud.knobX, hud.joyCy + hud.knobY, hud.joyR * 1.15f, Visuals.ACCENT, 0.10f);
            float x = hud.thrust.bounds.x + hud.thrust.bounds.width * 0.5f;
            float knobYLocal = hud.thrust.bounds.y + hud.thrust.bounds.height * hud.throttle;
            art.glow(batch, x, knobYLocal, hud.thrust.bounds.width * 0.85f, Visuals.ACCENT, 0.12f);
        }
        float barX = 16f * u + 44f * u;
        float barY = h - 96f * u;
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
