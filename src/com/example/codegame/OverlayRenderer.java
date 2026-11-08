package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Pantallas superpuestas y de menú: menú principal, fundido de muerte, pausa y
 * fin de partida. Comparte UiKit y SpaceBackground, y lee el estado por MyGame.
 */
final class OverlayRenderer {

    private static final Color PLANET_GLOW = new Color(0.28f, 0.55f, 1f, 1f);
    private static final Color ENGINE_GLOW = new Color(1f, 0.45f, 0.12f, 1f);
    private static final Color HINT_TEXT = new Color(0.40f, 0.48f, 0.62f, 1f);
    private static final Color OVER_EDGE_C = new Color(0.80f, 0.30f, 0.35f, 0.55f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final Visuals art;

    OverlayRenderer(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes, Visuals art) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.art = art;
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    /** Pantalla mínima de estación acoplada; los servicios completos llegan en T-503. */
    void drawDocked(float w, float h) {
        float pad = 16f * game.u;
        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        game.space.stars(w * 0.5f, h * 0.5f, w, h, game.u, 0.9f);
        batch.end();
        Visuals.normal(batch);

        float pw = Math.min(w - 4f * pad, 540f * game.u);
        float ph = 430f * game.u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0f, 0f, w, h);
        ui.panel(px, py, pw, ph, 26f * game.u, Visuals.PANEL, Visuals.PANEL_EDGE);
        shapes.setColor(0.30f, 0.65f, 1f, 0.55f);
        shapes.rect(px + pw * 0.25f, py + ph - 4f * game.u, pw * 0.5f, 3f * game.u);
        // Barra de almacén.
        float storeFrac = game.world.station.storage.capacity() > 0f
                ? game.world.station.storage.usedMass()
                / game.world.station.storage.capacity() : 0f;
        ui.panel(px + 30f * game.u, py + 150f * game.u, pw - 60f * game.u,
                16f * game.u, 8f * game.u, Visuals.PANEL, Visuals.PANEL_EDGE);
        if (storeFrac > 0.001f) {
            shapes.setColor(Visuals.ACCENT);
            Visuals.roundRect(shapes, px + 32f * game.u, py + 152f * game.u,
                    (pw - 64f * game.u) * storeFrac, 12f * game.u, 6f * game.u);
        }
        ui.drawButton(game.btnUndock, Visuals.BTN_PRIMARY, true);
        shapes.end();

        batch.begin();
        ui.textCenteredMiddle("NODO " + game.world.station.def.name,
                w * 0.5f, py + ph - 70f * game.u, 1.6f * game.u, Color.WHITE);
        ui.textCenteredMiddle("SERVICIOS COMPLETOS",
                w * 0.5f, py + ph - 120f * game.u, 0.85f * game.u, Visuals.ACCENT);
        ui.textCenteredMiddle("Reparacion  -  Combustible  -  O2  -  Mejoras",
                w * 0.5f, py + ph - 165f * game.u, 0.75f * game.u, Visuals.TEXT_DIM);
        ui.textCenteredMiddle("MERCADO Y ALMACEN EN T-503",
                w * 0.5f, py + ph - 200f * game.u, 0.72f * game.u, Visuals.TEXT_DIM);
        ui.textLeft("ALMACEN " + Math.round(game.world.station.storage.usedMass())
                        + "/" + Math.round(game.world.station.storage.capacity()) + " UM",
                px + 32f * game.u, py + 190f * game.u, 0.70f * game.u, Visuals.TEXT_DIM);
        ui.textCenteredMiddle("DESPEGAR devuelve la nave al exterior",
                w * 0.5f, py + 52f * game.u, 0.70f * game.u, Visuals.TEXT_DIM);
        ui.drawButtonText(game.btnUndock, true);
        batch.end();
    }

    void drawMenu(float w, float h) {
        float camX = game.time * 16f * game.u;
        float camY = game.time * 7f * game.u;
        float planetX = w * 0.93f;
        float planetY = h * 0.92f;
        float planetR = w * 0.16f;

        float bob = MathUtils.sin(game.time * 1.1f) * 5f * game.u;
        float shipX = w * 0.5f;
        float shipY = h * 0.635f + bob;
        float shipHeading = 22f + MathUtils.sin(game.time * 0.55f) * 5f;
        float shipSize = 52f * game.u;
        float engX = shipX - MathUtils.cosDeg(shipHeading) * shipSize * 1.05f;
        float engY = shipY - MathUtils.sinDeg(shipHeading) * shipSize * 1.05f;
        game.btnPlay.label = game.runActive ? "CONTINUAR" : "JUGAR";

        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        game.space.nebula(camX, camY, w, h, game.u, 1.15f);
        game.space.stars(camX, camY, w, h, game.u, 1.05f);
        art.glow(batch, planetX, planetY, planetR * 2.8f,
                PLANET_GLOW, 0.22f);
        art.glow(batch, w * 0.5f, h * 0.835f, w * 1.0f, Visuals.ACCENT, 0.07f);
        art.glow(batch, engX, engY, shipSize * 2.6f, ENGINE_GLOW, 0.48f);
        art.glow(batch, engX, engY, shipSize * 1.1f, Color.WHITE, 0.50f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        game.space.planet(planetX, planetY, planetR);
        Ship.drawBody(shapes, shipX, shipY, shipHeading, shipSize, true, 1f, game.time);

        ui.drawButton(game.btnPlay, Visuals.BTN_PRIMARY, true);
        ui.coinChipShape(w * 0.5f, h * 0.725f);
        shapes.end();

        batch.begin();
        ui.drawTitle("ASTEROIDES", w * 0.5f, h * 0.845f, 2.35f * game.u);
        ui.textCentered("PILOTA. MINA. SOBREVIVE.", w * 0.5f, h * 0.795f, 0.95f * game.u, Visuals.ACCENT);
        ui.coinChipText(w * 0.5f, h * 0.725f, game.upgrades.coins());
        ui.drawButtonText(game.btnPlay, true);
        if (game.runActive) {
            ui.textCentered("PARTIDA GUARDADA", w * 0.5f, h * 0.56f, 0.78f * game.u, Visuals.GOLD);
        }
        ui.textCentered("JOYSTICK GIRA - BARRA ACELERA", w * 0.5f, h * 0.215f, 0.68f * game.u, Visuals.TEXT_DIM);
        ui.textCentered("TOCA EL VACIO PARA DISPARAR - TOCA UNA ROCA PARA EL PILOTO",
                w * 0.5f, 22f * game.u, 0.62f * game.u, HINT_TEXT);
        batch.end();
    }

    /** Destello blanco, viñeta roja y fundido mientras la nave explota. */
    void drawDeathFlourish(float w, float h) {
        float flash = MathUtils.clamp(1f - game.deathTimer / 0.30f, 0f, 1f);
        float dim = MathUtils.clamp((game.deathTimer - 1.20f) / 1.20f, 0f, 1f) * 0.60f;
        beginShapes();
        if (flash > 0f) {
            shapes.setColor(1f, 0.96f, 0.86f, flash * 0.62f);
            shapes.rect(0f, 0f, w, h);
        }
        if (dim > 0f) {
            shapes.setColor(0f, 0f, 0f, dim);
            shapes.rect(0f, 0f, w, h);
        }
        shapes.end();

        batch.begin();
        Visuals.normal(batch);
        if (game.deathTimer < 0.75f) {
            float p = 1f - game.deathTimer / 0.75f;
            art.vignetteTint(batch, w, h, 1f, 0.20f, 0.16f, 0.30f * p * p);
        }
        batch.end();
    }

    void drawPause(float w, float h) {
        float pad = 16f * game.u;
        float pw = Math.min(w - 4f * pad, 520f * game.u);
        float ph = 470f * game.u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.64f);
        shapes.rect(0f, 0f, w, h);
        ui.panel(px, py, pw, ph, 26f * game.u, Visuals.PANEL, Visuals.PANEL_EDGE);
        shapes.setColor(0.30f, 0.65f, 1f, 0.55f);
        shapes.rect(px + pw * 0.25f, py + ph - 4f * game.u, pw * 0.5f, 3f * game.u);
        ui.drawButton(game.btnResume, Visuals.BTN_PRIMARY, true);
        ui.drawButton(game.btnMenu, Visuals.BTN_SECONDARY, true);
        ui.drawButton(game.btnMute, Visuals.BTN_SECONDARY, true);
        ui.drawButton(game.btnMusic, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textCenteredMiddle("PAUSA", w * 0.5f, h * 0.5f + 172f * game.u, 1.9f * game.u, Color.WHITE);
        ui.textCenteredMiddle("OLEADA " + game.world.wave + "   -   PUNTOS " + game.world.score,
                w * 0.5f, h * 0.5f + 105f * game.u, 0.92f * game.u, Visuals.ACCENT);
        ui.drawButtonText(game.btnResume, true);
        ui.drawButtonText(game.btnMenu, true);
        ui.drawButtonText(game.btnMute, true);
        ui.drawButtonText(game.btnMusic, true);
        batch.end();
    }

    void drawOver(float w, float h) {
        float pad = 16f * game.u;
        float pw = Math.min(w - 4f * pad, 520f * game.u);
        float ph = 470f * game.u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.66f);
        shapes.rect(0f, 0f, w, h);
        ui.panel(px, py, pw, ph, 26f * game.u, Visuals.PANEL, OVER_EDGE_C);
        shapes.setColor(1f, 0.35f, 0.42f, 0.60f);
        shapes.rect(px + pw * 0.25f, py + ph - 4f * game.u, pw * 0.5f, 3f * game.u);
        ui.drawButton(game.btnRetry, Visuals.BTN_PRIMARY, true);
        ui.drawButton(game.btnOverMenu, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textCenteredMiddle("PERSONAJE PERDIDO", w * 0.5f, h * 0.5f + 172f * game.u, 1.75f * game.u, Color.WHITE);
        ui.textCenteredMiddle("MONEDAS GANADAS  +" + game.overCoins,
                w * 0.5f, h * 0.5f + 112f * game.u, 0.95f * game.u, Visuals.GOLD);
        ui.textCenteredMiddle("PUNTOS " + game.world.score + "   -   OLEADA " + game.world.wave,
                w * 0.5f, h * 0.5f + 78f * game.u, 0.90f * game.u, Visuals.TEXT_DIM);
        ui.textCenteredMiddle(game.recoveryTest ? "MODO PRUEBAS: RECUPERAR" : "La nave queda en el espacio",
                w * 0.5f, h * 0.5f + 48f * game.u, 0.72f * game.u,
                game.recoveryTest ? Visuals.WARN : Visuals.TEXT_DIM);
        ui.drawButtonText(game.btnRetry, true);
        ui.drawButtonText(game.btnOverMenu, true);
        batch.end();
    }

}
