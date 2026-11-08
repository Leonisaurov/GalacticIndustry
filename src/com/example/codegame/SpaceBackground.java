package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Fondo espacial compartido por menú, partida y tienda: degradado, estrellas
 * con parallax, nebulosas y el planeta decorativo. No conoce el estado del
 * juego; MyGame le pasa el tiempo y la cámara.
 */
final class SpaceBackground {

    static final float STAR_CELL = 280f;
    static final int STARS_PER_CELL = 3;

    private static final Color PLANET_LEFT = new Color();
    private static final Color PLANET_RIGHT = new Color();

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final Visuals art;
    /** Tiempo acumulado para el parpadeo y el desplazamiento. */
    float time;

    SpaceBackground(SpriteBatch batch, ShapeRenderer shapes, Visuals art) {
        this.batch = batch;
        this.shapes = shapes;
        this.art = art;
    }

    void gradient(float camX, float camY, float ww, float wh) {
        float hw = ww * 0.5f + STAR_CELL;
        float hh = wh * 0.5f + STAR_CELL;
        shapes.rect(camX - hw, camY - hh, hw * 2f, hh * 2f,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
    }

    /** Estrellas en tres capas de parallax. unit = unidades del mundo por unidad de dibujo. */
    void stars(float camX, float camY, float ww, float wh, float unit, float intensity) {
        float hw = ww * 0.5f;
        float hh = wh * 0.5f;
        for (int layer = 0; layer < 3; layer++) {
            float par = 0.22f + 0.28f * layer;
            float ox = camX * par;
            float oy = camY * par;
            float cell = STAR_CELL * unit;
            int x0 = (int) Math.floor((ox - hw) / cell) - 1;
            int x1 = (int) Math.floor((ox + hw) / cell) + 1;
            int y0 = (int) Math.floor((oy - hh) / cell) - 1;
            int y1 = (int) Math.floor((oy + hh) / cell) + 1;
            for (int gx = x0; gx <= x1; gx++) {
                for (int gy = y0; gy <= y1; gy++) {
                    for (int k = 0; k < STARS_PER_CELL; k++) {
                        float h1 = hash(gx, gy, k * 7 + layer * 131);
                        float h2 = hash(gx, gy, k * 11 + layer * 197 + 5);
                        float h3 = hash(gx, gy, k * 17 + layer * 251 + 9);
                        float px = (gx + hash(gx, gy, k * 3 + layer * 41)) * cell - ox;
                        float py = (gy + hash(gx, gy, k * 5 + layer * 67 + 2)) * cell - oy;
                        float tw = 0.55f + 0.45f * MathUtils.sin(time * 1.7f + h1 * 31f);
                        float size = (1.8f + h2 * 3.2f + layer * 0.9f) * unit;
                        float alpha = (0.30f + layer * 0.20f + 0.25f * tw) * intensity;
                        Color c;
                        if (h3 < 0.08f) {
                            c = Visuals.WARN;
                        } else if (h3 < 0.30f) {
                            c = Visuals.ACCENT;
                        } else {
                            c = Color.WHITE;
                        }
                        art.star(batch, camX + px, camY + py, size, c, alpha);
                        if (layer == 2 && h3 > 0.965f) {
                            float big = size * 3.4f;
                            art.star(batch, camX + px, camY + py, big, c, alpha * 0.45f);
                            art.glow(batch, camX + px, camY + py, big * 1.7f, c, alpha * 0.18f);
                        }
                    }
                }
            }
        }
    }

    /** Nubes de nebulosa muy tenues para dar color y profundidad al fondo. */
    void nebula(float camX, float camY, float ww, float wh, float unit, float intensity) {
        float hw = ww * 0.5f;
        float hh = wh * 0.5f;
        float cell = 1450f * unit;
        for (int layer = 0; layer < 3; layer++) {
            float par = 0.10f + 0.09f * layer;
            float ox = camX * par;
            float oy = camY * par;
            int x0 = (int) Math.floor((ox - hw) / cell) - 1;
            int x1 = (int) Math.floor((ox + hw) / cell) + 1;
            int y0 = (int) Math.floor((oy - hh) / cell) - 1;
            int y1 = (int) Math.floor((oy + hh) / cell) + 1;
            for (int gx = x0; gx <= x1; gx++) {
                for (int gy = y0; gy <= y1; gy++) {
                    float h = hash(gx, gy, 701 + layer * 17);
                    if (h < 0.28f) {
                        continue;
                    }
                    float px = (gx + hash(gx, gy, 733 + layer * 17)) * cell - ox;
                    float py = (gy + hash(gx, gy, 767 + layer * 17)) * cell - oy;
                    float size = (760f + hash(gx, gy, 799 + layer * 17) * 980f) * unit;
                    Color c = h < 0.63f ? Visuals.NEBULA_PURPLE
                            : h < 0.82f ? Visuals.NEBULA_BLUE : Visuals.NEBULA_TEAL;
                    float alpha = (0.080f + 0.100f * hash(gx, gy, 823 + layer * 17)) * intensity;
                    art.cloud(batch, camX + px, camY + py, size, size * 0.82f, c, alpha);
                }
            }
        }
    }

    /** Planeta con bandas y sombreado lateral, dibujado con franjas horizontales. */
    void planet(float cx, float cy, float r) {
        int bands = 44;
        for (int i = 0; i < bands; i++) {
            float t0 = i / (float) bands;
            float t1 = (i + 1) / (float) bands;
            float y0 = -r + 2f * r * t0;
            float y1 = -r + 2f * r * t1;
            float ym = (y0 + y1) * 0.5f;
            float half = (float) Math.sqrt(Math.max(0f, r * r - ym * ym));
            if (half <= 0f) {
                continue;
            }
            float band = 0.5f + 0.30f * MathUtils.sin(ym * 0.115f + 2.1f)
                    + 0.20f * MathUtils.sin(ym * 0.035f + 0.6f);
            float br = 0.20f + 0.13f * band;
            float bg = 0.28f + 0.16f * band;
            float bb = 0.52f + 0.22f * band;
            // Luz desde arriba-izquierda: borde izquierdo claro, derecho en sombra.
            PLANET_LEFT.set(Math.min(1f, br * 1.55f), Math.min(1f, bg * 1.55f),
                    Math.min(1f, bb * 1.45f), 1f);
            PLANET_RIGHT.set(br * 0.28f, bg * 0.28f, bb * 0.28f, 1f);
            shapes.rect(cx - half, cy + y0, half * 2f, (y1 - y0) + 1f,
                    PLANET_LEFT, PLANET_RIGHT, PLANET_RIGHT, PLANET_LEFT);
        }
    }

    /** Número pseudoaleatorio en [0, 1) determinista para una celda y un índice. */
    static float hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0xffffff) / (float) 0x1000000;
    }
}
