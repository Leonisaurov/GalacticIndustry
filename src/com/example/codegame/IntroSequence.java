package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Introducción narrativa mínima (UPD-006-T33): 3–5 líneas ASCII, se muestra una
 * vez y se puede omitir. No hay campaña, cinemáticas ni recursos nuevos.
 */
final class IntroSequence {

    private static final Color PANEL = new Color(0.020f, 0.030f, 0.042f, 0.96f);
    private static final String[] LINES = {
            "PROSPECTO ASIGNADO A LA NODRIZA.",
            "LA ESTACION TE CONCEDE 1200 CR",
            "PARA ADQUIRIR TU PRIMERA NAVE.",
            "HABLA CON EL CONCESIONARIO, EQUIPA",
            "Y SALE A EXPLORAR EL SECTOR."
    };

    private final Button btnContinue;
    private boolean shown;

    IntroSequence() {
        btnContinue = new Button("CONTINUAR", 0f, 0f, 120f, 50f);
    }

    boolean seen(Upgrades upgrades) {
        return upgrades != null && "1".equals(upgrades.getText("introSeen"));
    }

    void markSeen(Upgrades upgrades) {
        if (upgrades != null) {
            upgrades.putText("introSeen", "1");
        }
    }

    void enter() {
        shown = true;
    }

    void layout(float w, float h) {
        float u = Math.max(1f, Gdx.graphics.getDensity());
        btnContinue.bounds.set((w - Math.min(w * 0.6f, 260f * u)) * 0.5f,
                h * 0.26f, Math.min(w * 0.6f, 260f * u), 52f * u);
    }

    boolean handle(float w, float h) {
        if (btnContinue.bounds.width <= 0f) {
            layout(w, h);
        }
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < 5; i++) {
                if (!Gdx.input.isTouched(i)) {
                    continue;
                }
                float x = Gdx.input.getX(i);
                float y = Gdx.graphics.getHeight() - Gdx.input.getY(i);
                if (btnContinue.bounds.contains(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    void draw(float w, float h, UiKit ui, SpriteBatch batch,
              ShapeRenderer shapes, Visuals art, float time) {
        if (btnContinue.bounds.width <= 0f) {
            layout(w, h);
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0f, 0f, w, h);
        float u = Math.max(1f, Gdx.graphics.getDensity());
        float pw = Math.min(w * 0.88f, 430f * u);
        float ph = Math.min(h * 0.46f, 420f * u);
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;
        ui.frame(px, py, pw, ph, 10f * u, PANEL, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.led(px + 22f * u, py + ph - 24f * u, 3f * u, Visuals.GOOD);
        float blink = 0.6f + 0.4f * MathUtils.sin(time * 3f);
        shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g, Visuals.ACCENT.b,
                0.60f * blink);
        shapes.rect(px + 20f * u, py + 20f * u, 2f * u, ph - 40f * u);
        ui.drawButton(btnContinue, Visuals.BTN_PRIMARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("TERMINAL DE ASIGNACION", px + 34f * u,
                py + ph - 20f * u, 0.72f * u, Visuals.ACCENT);
        for (int i = 0; i < LINES.length; i++) {
            ui.textLeft(LINES[i], px + 34f * u,
                    py + ph - 70f * u - i * 34f * u,
                    i == 0 ? 0.72f * u : 0.62f * u,
                    i == 0 ? Visuals.TEXT : Visuals.TEXT_DIM);
        }
        ui.drawButtonText(btnContinue, true);
        batch.end();
    }
}
