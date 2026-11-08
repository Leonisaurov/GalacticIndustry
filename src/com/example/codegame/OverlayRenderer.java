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
 * La composición sigue el mismo lenguaje de consola que el resto de pantallas:
 * cabecera con acento, paneles de datos y pie de acciones.
 */
final class OverlayRenderer {

    private static final Color PLANET_GLOW = new Color(0.30f, 0.50f, 0.70f, 1f);
    private static final Color ENGINE_GLOW = new Color(1f, 0.55f, 0.18f, 1f);
    private static final Color HINT_TEXT = new Color(0.46f, 0.52f, 0.58f, 1f);
    private static final Color OVER_EDGE_C = new Color(0.55f, 0.24f, 0.22f, 0.70f);
    private static final Color ROW_C = new Color(0.055f, 0.075f, 0.095f, 0.80f);

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

    void drawMenu(float w, float h) {
        float u = game.u;
        float camX = game.time * 16f * u;
        float camY = game.time * 7f * u;
        float planetX = w * 0.93f;
        float planetY = h * 0.92f;
        float planetR = w * 0.16f;

        float bob = MathUtils.sin(game.time * 1.1f) * 5f * u;
        float shipX = w * 0.5f;
        float shipY = h * 0.635f + bob;
        float shipHeading = 22f + MathUtils.sin(game.time * 0.55f) * 5f;
        float shipSize = 52f * u;
        float engX = shipX - MathUtils.cosDeg(shipHeading) * shipSize * 1.05f;
        float engY = shipY - MathUtils.sinDeg(shipHeading) * shipSize * 1.05f;
        game.btnPlay.label = game.runActive ? "CONTINUAR" : "JUGAR";

        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        game.space.nebula(camX, camY, w, h, u, 1.15f);
        game.space.stars(camX, camY, w, h, u, 1.05f);
        art.glow(batch, planetX, planetY, planetR * 2.8f, PLANET_GLOW, 0.20f);
        art.glow(batch, w * 0.5f, h * 0.835f, w * 1.0f, Visuals.ACCENT, 0.055f);
        art.glow(batch, engX, engY, shipSize * 2.6f, ENGINE_GLOW, 0.48f);
        art.glow(batch, engX, engY, shipSize * 1.1f, Color.WHITE, 0.50f);
        batch.end();
        Visuals.normal(batch);

        float pad = 14f * u;
        float cw = Math.min(w - 2f * pad - 24f * u, 620f * u);
        float cx = (w - cw) * 0.5f;
        float cy = h * 0.235f;
        float ch = h * 0.300f;

        beginShapes();
        game.space.planet(planetX, planetY, planetR);
        Ship.drawBody(shapes, shipX, shipY, shipHeading, shipSize, true, 1f, game.time);
        ui.frame(pad, pad, w - 2f * pad, h - 2f * pad, 14f * u,
                Visuals.GLASS, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.frame(cx, cy, cw, ch, 16f * u,
                Visuals.PANEL, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.drawButton(game.btnPlay, Visuals.BTN_PRIMARY, true);
        ui.coinChipShape(w * 0.5f, h * 0.43f);
        // Indicadores laterales del marco: datos de la nave, no decoracion.
        ui.led(pad + 34f * u, h - 62f * u, 3f * u, Visuals.GOOD);
        ui.led(w - pad - 34f * u, h - 62f * u, 3f * u, Visuals.ACCENT);
        shapes.end();

        batch.begin();
        ui.textLeft("CONSOLA DE MANDO", pad + 48f * u, h - 58f * u,
                0.80f * u, Visuals.TEXT);
        ui.textRight(game.runActive ? "SESION ACTIVA" : "SISTEMA EN ESPERA",
                w - pad - 48f * u, h - 58f * u, 0.68f * u,
                game.runActive ? Visuals.GOOD : Visuals.TEXT_DIM);

        ui.drawTitle("ASTEROIDES", w * 0.5f, h * 0.905f, 2.25f * u);
        ui.textCentered("SIMULADOR DE MINERIA, EXPLORACION Y SUPERVIVENCIA",
                w * 0.5f, h * 0.862f, 0.76f * u, Visuals.ACCENT);

        float rowW = cw - 72f * u;
        float rowX = cx + 36f * u;
        float rowY1 = cy + ch - 78f * u;
        ui.textCentered("OPERACION", w * 0.5f, cy + ch - 26f * u,
                0.72f * u, Visuals.TEXT_DIM);
        batch.end();

        beginShapes();
        ui.panel(rowX, rowY1, rowW, 34f * u, 6f * u,
                ROW_C, Visuals.CARD_EDGE);
        ui.panel(rowX, rowY1 - 40f * u, rowW, 34f * u, 6f * u,
                ROW_C, Visuals.CARD_EDGE);
        ui.divider(cx + 36f * u, cy + 66f * u, cw - 72f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        shapes.end();

        batch.begin();
        ui.textLeft("MISION", rowX + 14f * u, rowY1 + 24f * u, 0.62f * u, Visuals.TEXT_DIM);
        ui.textRight("EXTRACCION / SUPERVIVENCIA", rowX + rowW - 14f * u,
                rowY1 + 24f * u, 0.64f * u, Visuals.TEXT);
        ui.textLeft("CARGA", rowX + 14f * u, rowY1 - 16f * u, 0.62f * u, Visuals.TEXT_DIM);
        ui.textRight(game.runActive ? "PARTIDA GUARDADA" : "SIN PARTIDA",
                rowX + rowW - 14f * u, rowY1 - 16f * u, 0.64f * u,
                game.runActive ? Visuals.GOLD : Visuals.TEXT_OFF);
        ui.coinChipText(w * 0.5f, h * 0.43f, game.upgrades.coins());
        ui.textCentered("SECTOR 01   -   NAVE DE EXTRACCION   -   MODO SUPERVIVENCIA",
                w * 0.5f, h * 0.405f, 0.56f * u, Visuals.TEXT_OFF);
        ui.drawButtonText(game.btnPlay, true);

        ui.textCentered("JOYSTICK GIRA  -  BARRA ACELERA", w * 0.5f, cy + 44f * u,
                0.64f * u, Visuals.TEXT_DIM);
        ui.textCentered("TOCA EL VACIO PARA DISPARAR  -  TOCA UNA ROCA PARA EL PILOTO",
                w * 0.5f, 34f * u, 0.58f * u, HINT_TEXT);
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
        float u = game.u;
        float pad = 16f * u;
        float pw = Math.min(w - 4f * pad, 520f * u);
        float ph = 440f * u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        boolean soundOn = !game.audioMuted();
        boolean musicOn = !game.musicMuted();

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.64f);
        shapes.rect(0f, 0f, w, h);
        ui.frame(px, py, pw, ph, 18f * u, Visuals.PANEL, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.divider(px + 30f * u, py + 190f * u, pw - 60f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.drawButton(game.btnResume, Visuals.BTN_PRIMARY, true);
        ui.drawButton(game.btnMenu, Visuals.BTN_SECONDARY, true);
        ui.drawButton(game.btnMute,
                soundOn ? Visuals.BTN_SECONDARY : Visuals.BTN_OFF, true);
        ui.drawButton(game.btnMusic,
                musicOn ? Visuals.BTN_SECONDARY : Visuals.BTN_OFF, true);
        ui.led(px + 34f * u, py + ph - 34f * u, 3f * u, Visuals.WARN);
        // Icons de accion dentro de cada boton, alineados con su borde izquierdo.
        float iconX = game.btnResume.bounds.x + 24f * u;
        ui.drawWarpIcon(iconX, game.btnResume.bounds.y + 28f * u, 8f * u, Visuals.ACCENT);
        drawMenuGlyph(iconX, game.btnMenu.bounds.y + 26f * u, 8f * u, Visuals.STEEL_LIGHT);
        drawSpeakerGlyph(iconX, game.btnMute.bounds.y + 22f * u, 8f * u,
                soundOn ? Visuals.GOOD : Visuals.TEXT_OFF);
        drawNoteGlyph(iconX, game.btnMusic.bounds.y + 22f * u, 8f * u,
                musicOn ? Visuals.GOOD : Visuals.TEXT_OFF);
        shapes.end();

        batch.begin();
        ui.textLeft("SISTEMA / PAUSA", px + 54f * u, py + ph - 22f * u,
                0.74f * u, Visuals.TEXT_DIM);
        ui.textCenteredMiddle("PAUSA", w * 0.5f, py + ph - 58f * u,
                1.70f * u, Color.WHITE);
        ui.textCenteredMiddle("OLEADA " + game.world.wave + "   -   PUNTOS " + game.world.score,
                w * 0.5f, py + ph - 98f * u, 0.82f * u, Visuals.ACCENT);
        ui.drawButtonText(game.btnResume, true);
        ui.drawButtonText(game.btnMenu, true);
        ui.drawButtonText(game.btnMute, true);
        ui.drawButtonText(game.btnMusic, true);
        ui.textCenteredMiddle("PARTIDA GUARDADA",
                w * 0.5f, py + 44f * u, 0.60f * u, Visuals.GOLD);
        batch.end();
    }

    void drawOver(float w, float h) {
        float u = game.u;
        float pad = 16f * u;
        float pw = Math.min(w - 4f * pad, 520f * u);
        float ph = 400f * u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.66f);
        shapes.rect(0f, 0f, w, h);
        ui.frame(px, py, pw, ph, 18f * u, Visuals.PANEL, OVER_EDGE_C, Visuals.DANGER);
        ui.divider(px + 30f * u, py + 212f * u, pw - 60f * u,
                Visuals.PANEL_EDGE, Visuals.DANGER);
        ui.drawButton(game.btnRetry, Visuals.BTN_PRIMARY, true);
        ui.drawButton(game.btnOverMenu, Visuals.BTN_SECONDARY, true);
        ui.led(px + 34f * u, py + ph - 34f * u, 3f * u, Visuals.DANGER);
        drawMenuGlyph(game.btnOverMenu.bounds.x + 24f * u,
                game.btnOverMenu.bounds.y + 26f * u, 8f * u, Visuals.STEEL_LIGHT);
        shapes.end();

        batch.begin();
        ui.textLeft("SISTEMA / FIN DE OPERACION", px + 54f * u, py + ph - 22f * u,
                0.72f * u, Visuals.DANGER);
        ui.textCenteredMiddle("PERSONAJE PERDIDO", w * 0.5f, py + ph - 58f * u,
                1.55f * u, Color.WHITE);
        // Panel de resultados.
        float rowX = px + 36f * u;
        float rowW = pw - 72f * u;
        batch.end();
        beginShapes();
        ui.panel(rowX, py + 232f * u, rowW, 54f * u, 7f * u,
                ROW_C, Visuals.CARD_EDGE);
        shapes.end();
        batch.begin();
        ui.textLeft("MONEDAS GANADAS", rowX + 14f * u, py + 278f * u,
                0.60f * u, Visuals.TEXT_DIM);
        ui.textRight("+" + game.overCoins, rowX + rowW - 14f * u,
                py + 278f * u, 0.90f * u, Visuals.GOLD);
        ui.textLeft("PUNTOS " + game.world.score + "   -   OLEADA " + game.world.wave,
                rowX + 14f * u, py + 248f * u, 0.68f * u, Visuals.TEXT_DIM);
        ui.drawButtonText(game.btnRetry, true);
        ui.drawButtonText(game.btnOverMenu, true);
        ui.textCenteredMiddle("EMPIEZA OTRA PARTIDA CUANDO QUIERAS",
                w * 0.5f, py + 44f * u, 0.58f * u, Visuals.TEXT_OFF);
        batch.end();
    }

    private void drawMenuGlyph(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.rect(cx - r, cy - r * 0.72f, r * 2f, r * 0.28f);
        shapes.rect(cx - r, cy - r * 0.14f, r * 2f, r * 0.28f);
        shapes.rect(cx - r, cy + r * 0.44f, r * 2f, r * 0.28f);
    }

    private void drawSpeakerGlyph(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.rect(cx - r * 0.95f, cy - r * 0.28f, r * 0.55f, r * 0.56f);
        shapes.triangle(cx - r * 0.40f, cy - r * 0.72f,
                cx - r * 0.40f, cy + r * 0.72f, cx + r * 0.35f, cy);
        if (c != Visuals.TEXT_OFF) {
            shapes.rect(cx + r * 0.62f, cy - r * 0.34f, r * 0.16f, r * 0.68f);
            shapes.rect(cx + r * 0.98f, cy - r * 0.58f, r * 0.16f, r * 1.16f);
        }
    }

    private void drawNoteGlyph(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.rect(cx - r * 0.05f, cy - r * 0.18f, r * 0.16f, r * 1.10f);
        shapes.triangle(cx + r * 0.10f, cy + r * 0.92f,
                cx + r * 0.10f, cy + r * 0.42f, cx + r * 0.80f, cy + r * 0.62f);
        shapes.circle(cx - r * 0.18f, cy - r * 0.24f, r * 0.28f, 10);
    }
}
