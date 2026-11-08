package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Primitivas de interfaz: paneles, botones, iconos y texto. No guarda estado
 * de juego; MyGame le pasa las referencias de dibujo y la densidad {@link #u}
 * antes de usarla. Así el dibujo de la UI deja de vivir dentro de MyGame.
 */
final class UiKit {

    /** Comprueba si un botón está pulsado; lo aporta la capa de entrada. */
    interface Pressed {
        boolean is(Button button);
    }

    private static final Color HULL_TRACK_C = new Color(0.03f, 0.045f, 0.06f, 0.96f);
    private static final Color COIN_CHIP_C = new Color(0.07f, 0.09f, 0.11f, 0.94f);
    private static final Color tint = new Color();
    private static final Color tint2 = new Color();
    private static final Color tint3 = new Color();

    final SpriteBatch batch;
    final ShapeRenderer shapes;
    final BitmapFont font;
    final GlyphLayout layout;
    /** Densidad de pantalla en unidades de dibujo. */
    float u = 1f;
    float buttonTextScale = 3f;
    Pressed pressed;

    UiKit(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, GlyphLayout layout) {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.layout = layout;
    }

    /** Margen exterior comun a todas las pantallas. */
    static float margin(float u) {
        return 20f * u;
    }

    /** Altura de la cabecera de una pantalla de consola. */
    static float headerH(float u) {
        return 96f * u;
    }

    /** Altura del pie de acciones de una pantalla de consola. */
    static float footerH(float u) {
        return 110f * u;
    }

    static float rowGap(float u) {
        return 8f * u;
    }

    void panel(float x, float y, float w, float h, float r, Color fill, Color border) {
        shapes.setColor(0f, 0f, 0f, 0.40f);
        Visuals.roundRect(shapes, x + 2f * u, y - 4f * u, w, h, r);
        shapes.setColor(border);
        Visuals.roundRect(shapes, x - 1.5f * u, y - 1.5f * u, w + 3f * u, h + 3f * u, r + 1.5f * u);
        shapes.setColor(fill);
        Visuals.roundRect(shapes, x, y, w, h, r);
        // Bisel superior y sombra de apoyo: da volumen sin brillos neon.
        shapes.setColor(1f, 1f, 1f, 0.055f);
        Visuals.roundRect(shapes, x + 2f * u, y + h - 2.5f * u, w - 4f * u, 1.5f * u, 0.75f * u);
        shapes.setColor(0f, 0f, 0f, 0.22f);
        Visuals.roundRect(shapes, x + 2f * u, y + 1f * u, w - 4f * u, 2f * u, 1f * u);
    }

    /**
     * Marco de consola para pantallas completas: panel, franja de cabecera con
     * barra de acento, esquinas tecnicas y remaches. El texto se dibuja aparte.
     */
    void frame(float x, float y, float w, float h, float r,
               Color fill, Color edge, Color accent) {
        panel(x, y, w, h, r, fill, edge);
        float head = 38f * u;
        if (h > head * 2f) {
            shapes.setColor(1f, 1f, 1f, 0.035f);
            shapes.rect(x + 2f * u, y + h - head, w - 4f * u, head - 2f * u);
            shapes.setColor(accent.r, accent.g, accent.b, 0.80f);
            shapes.rect(x + 2f * u, y + h - head, 7f * u, head - 2f * u);
            shapes.setColor(0f, 0f, 0f, 0.30f);
            shapes.rect(x + 2f * u, y + h - head - 2f * u, w - 4f * u, 2f * u);
        }
        corner(x + 7f * u, y + 7f * u, 1f, 1f, accent);
        corner(x + w - 7f * u, y + 7f * u, -1f, 1f, accent);
        corner(x + 7f * u, y + h - 7f * u, 1f, -1f, accent);
        corner(x + w - 7f * u, y + h - 7f * u, -1f, -1f, accent);
        float rv = 2.2f * u;
        shapes.setColor(Visuals.STEEL_LIGHT.r, Visuals.STEEL_LIGHT.g,
                Visuals.STEEL_LIGHT.b, 0.35f);
        shapes.circle(x + 15f * u, y + 15f * u, rv, 8);
        shapes.circle(x + w - 15f * u, y + 15f * u, rv, 8);
        shapes.circle(x + 15f * u, y + h - 15f * u, rv, 8);
        shapes.circle(x + w - 15f * u, y + h - 15f * u, rv, 8);
    }

    private void corner(float x, float y, float sx, float sy, Color c) {
        float len = 16f * u;
        float thick = 1.6f * u;
        shapes.setColor(c.r, c.g, c.b, 0.55f);
        shapes.rect(sx > 0f ? x : x - len, y, len, thick);
        shapes.rect(x, sy > 0f ? y : y - len, thick, len);
    }

    void divider(float x, float y, float w, Color edge, Color accent) {
        shapes.setColor(edge.r, edge.g, edge.b, edge.a * 0.8f);
        shapes.rect(x, y, w, 1.2f * u);
        shapes.setColor(accent.r, accent.g, accent.b, 0.75f);
        shapes.rect(x, y, Math.min(w * 0.16f, 52f * u), 1.6f * u);
    }

    /** Rejilla tecnica muy tenue para fondos de consola. */
    void grid(float w, float h, float step, float alpha) {
        shapes.setColor(Visuals.GRID.r, Visuals.GRID.g, Visuals.GRID.b, alpha);
        for (float x = 0f; x <= w; x += step) {
            shapes.rect(x, 0f, 1f * u, h);
        }
        for (float y = 0f; y <= h; y += step) {
            shapes.rect(0f, y, w, 1f * u);
        }
    }

    void led(float cx, float cy, float r, Color c) {
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.circle(cx, cy, r * 1.55f, 10);
        shapes.setColor(c.r, c.g, c.b, 0.30f);
        shapes.circle(cx, cy, r * 1.25f, 10);
        shapes.setColor(c);
        shapes.circle(cx, cy, r, 8);
    }

    void drawButton(Button b, Color fill, boolean enabled) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = Math.min(5f * u, h * 0.18f);
        boolean isPressed = enabled && pressed != null && pressed.is(b);

        shapes.setColor(0f, 0f, 0f, 0.42f);
        Visuals.roundRect(shapes, x + 2f * u, y - 5f * u, w, h, r);

        Color body = fill;
        if (!enabled) {
            body = Visuals.BTN_OFF;
        } else if (isPressed) {
            body = tint2.set(fill).lerp(Color.WHITE, 0.12f);
        }
        float br = Math.min(1f, body.r * 1.35f + 0.16f);
        float bg = Math.min(1f, body.g * 1.35f + 0.16f);
        float bb = Math.min(1f, body.b * 1.35f + 0.16f);
        shapes.setColor(br, bg, bb, enabled ? 0.70f : 0.40f);
        Visuals.roundRect(shapes, x - 1.5f * u, y - 1.5f * u, w + 3f * u, h + 3f * u, r + 1.5f * u);

        shapes.setColor(body);
        Visuals.roundRect(shapes, x, y, w, h, r);
        // Bisel superior, sombra inferior y pestaña de acento a la izquierda.
        shapes.setColor(1f, 1f, 1f, enabled ? 0.10f : 0.05f);
        Visuals.roundRect(shapes, x + 2f * u, y + h * 0.62f, w - 4f * u, h * 0.30f, r * 0.7f);
        shapes.setColor(0f, 0f, 0f, 0.20f);
        Visuals.roundRect(shapes, x + 2f * u, y + 2f * u, w - 4f * u, h * 0.16f, r * 0.5f);
        shapes.setColor(tint3.set(body).lerp(Color.WHITE, 0.42f));
        shapes.rect(x + 3f * u, y + 5f * u, 2.5f * u, h - 10f * u);
    }

    void drawButtonText(Button b, boolean enabled) {
        b.drawText(batch, font, layout, buttonTextScale, enabled ? Visuals.TEXT : Visuals.TEXT_OFF);
    }

    void coinChipShape(float cx, float cy) {
        float cw = 150f * u;
        float ch = 42f * u;
        panel(cx - cw * 0.5f, cy - ch * 0.5f, cw, ch, 6f * u,
                COIN_CHIP_C, Visuals.PANEL_EDGE);
        drawCoinIcon(cx - cw * 0.5f + 25f * u, cy, 12f * u);
    }

    void coinChipText(float cx, float cy, int value) {
        textCenteredMiddle(String.valueOf(value), cx + 12f * u, cy, 1.05f * u, Visuals.GOLD);
    }

    void drawCoinIcon(float cx, float cy, float r) {
        shapes.setColor(0.55f, 0.40f, 0.08f, 0.95f);
        shapes.circle(cx, cy, r, 20);
        shapes.setColor(Visuals.GOLD);
        shapes.circle(cx, cy, r * 0.86f, 20);
        shapes.setColor(1f, 0.92f, 0.60f, 0.95f);
        shapes.circle(cx - r * 0.20f, cy + r * 0.22f, r * 0.42f, 12);
        shapes.setColor(0.80f, 0.55f, 0.10f, 0.95f);
        shapes.triangle(cx, cy + r * 0.48f,
                cx + r * 0.34f, cy,
                cx, cy - r * 0.48f);
        shapes.triangle(cx, cy + r * 0.48f,
                cx - r * 0.34f, cy,
                cx, cy - r * 0.48f);
    }

    void drawFuelIcon(float cx, float cy, float r) {
        shapes.setColor(0.10f, 0.55f, 0.30f, 0.95f);
        Visuals.roundRect(shapes, cx - r * 0.55f, cy - r * 0.75f, r * 1.1f, r * 1.5f, r * 0.32f);
        shapes.setColor(0.45f, 1f, 0.65f, 0.95f);
        Visuals.roundRect(shapes, cx - r * 0.55f, cy - r * 0.75f, r * 0.30f, r * 1.5f, r * 0.15f);
        shapes.setColor(0.60f, 1f, 0.75f, 0.95f);
        shapes.rect(cx - r * 0.20f, cy + r * 0.72f, r * 0.40f, r * 0.22f);
        shapes.setColor(Color.WHITE);
        shapes.rect(cx - r * 0.12f, cy - r * 0.28f, r * 0.24f, r * 0.56f);
        shapes.rect(cx - r * 0.28f, cy - r * 0.12f, r * 0.56f, r * 0.24f);
    }

    void drawLevelPips(float x, float y, int level) {
        float pipW = 12f * u;
        float pipH = 5f * u;
        float gap = 4f * u;
        for (int k = 0; k < Upgrades.MAX_LEVEL; k++) {
            if (k < level) {
                shapes.setColor(Visuals.ACCENT);
            } else {
                shapes.setColor(0.20f, 0.24f, 0.34f, 0.9f);
            }
            Visuals.roundRect(shapes, x + k * (pipW + gap), y, pipW, pipH, pipH * 0.5f);
        }
    }

    /** Barra de integridad de una parte (verde, ámbar o rojo). */
    void drawIntegrityBar(float x, float y, float w, float h, float frac) {
        shapes.setColor(HULL_TRACK_C);
        Visuals.roundRect(shapes, x, y, w, h, h * 0.5f);
        if (frac > 0.001f) {
            shapes.setColor(partColor(frac));
            Visuals.roundRect(shapes, x, y, Math.max(h, w * MathUtils.clamp(frac, 0f, 1f)), h, h * 0.5f);
        }
    }

    static Color partColor(float frac) {
        if (frac > 0.55f) {
            return Visuals.GOOD;
        }
        return frac > 0.28f ? Visuals.WARN : Visuals.DANGER;
    }

    void drawTitle(String text, float cx, float topY, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float o = Math.max(2f, scale * 0.75f);
        font.setColor(0.36f, 0.86f, 1f, 0.16f);
        font.draw(batch, text, x + o, topY);
        font.draw(batch, text, x - o, topY);
        font.draw(batch, text, x, topY + o);
        font.draw(batch, text, x, topY - o);
        font.setColor(0f, 0f, 0f, 0.55f);
        font.draw(batch, text, x + o * 1.5f, topY - o * 1.5f);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    void textCentered(String text, float cx, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    /** Texto centrado que reduce la escala si supera maxWidth. */
    void textCenteredFit(String text, float cx, float topY, float scale,
                         float maxWidth, Color c) {
        float s = scale;
        font.getData().setScale(s);
        layout.setText(font, text);
        if (layout.width > maxWidth && layout.width > 0f) {
            s *= maxWidth / layout.width;
            font.getData().setScale(s);
        }
        float x = cx - layout.width * 0.5f;
        float sh = Math.max(1.5f, s * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    void textCenteredMiddle(String text, float cx, float cy, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float y = cy + layout.height * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, y - sh);
        font.setColor(c);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    void textLeft(String text, float x, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    /** Como {@link #textLeft} pero reduce la escala si el texto no cabe en maxWidth. */
    void textLeftFit(String text, float x, float topY, float scale, float maxWidth, Color c) {
        float s = scale;
        font.getData().setScale(s);
        layout.setText(font, text);
        if (layout.width > maxWidth && layout.width > 0f) {
            s *= maxWidth / layout.width;
            font.getData().setScale(s);
        }
        float sh = Math.max(1.5f, s * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    void textLeftMiddle(String text, float x, float cy, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float y = cy + layout.height * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, y - sh);
        font.setColor(c);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    void textRight(String text, float rightX, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = rightX - layout.width;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    void textRightMiddle(String text, float rightX, float cy, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = rightX - layout.width;
        float y = cy + layout.height * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, y - sh);
        font.setColor(c);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    /** Icono de mineral: roca facetada. */
    void drawOreIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f);
        shapes.triangle(cx - r, cy - r * 0.55f, cx, cy - r * 1.10f, cx + r, cy - r * 0.55f);
        shapes.setColor(c);
        shapes.triangle(cx - r, cy - r * 0.55f, cx + r, cy - r * 0.55f, cx + r * 0.55f, cy + r * 0.85f);
        shapes.setColor(c.r * 0.75f, c.g * 0.75f, c.b * 0.75f, 1f);
        shapes.triangle(cx - r, cy - r * 0.55f, cx - r * 0.55f, cy + r * 0.85f, cx + r * 0.55f, cy + r * 0.85f);
        shapes.setColor(1f, 1f, 1f, 0.28f);
        shapes.triangle(cx - r * 0.2f, cy + r * 0.2f, cx + r * 0.1f, cy + r * 0.55f, cx + r * 0.35f, cy + r * 0.15f);
    }

    /** Icono de bodega: contenedor con travesanos. */
    void drawCargoIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f);
        Visuals.roundRect(shapes, cx - r, cy - r * 0.85f, r * 2f, r * 1.7f, r * 0.18f);
        shapes.setColor(c);
        Visuals.roundRect(shapes, cx - r * 0.82f, cy - r * 0.66f, r * 1.64f, r * 1.32f, r * 0.14f);
        shapes.setColor(0.05f, 0.07f, 0.09f, 0.85f);
        shapes.rect(cx - r * 0.55f, cy - r * 0.10f, r * 1.10f, r * 0.14f);
        shapes.rect(cx - r * 0.55f, cy + r * 0.30f, r * 1.10f, r * 0.14f);
        shapes.rect(cx - r * 0.55f, cy - r * 0.50f, r * 1.10f, r * 0.14f);
    }

    /** Icono de estacion: anillo de atraque. */
    void drawStationIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f);
        shapes.circle(cx, cy, r, 20);
        shapes.setColor(0.05f, 0.07f, 0.09f, 1f);
        shapes.circle(cx, cy, r * 0.62f, 20);
        shapes.setColor(c);
        shapes.rect(cx - r * 0.12f, cy - r * 1.25f, r * 0.24f, r * 2.5f);
        shapes.rect(cx - r * 1.25f, cy - r * 0.12f, r * 2.5f, r * 0.24f);
        shapes.circle(cx, cy, r * 0.22f, 10);
    }

    /** Icono de warp: galones de navegacion. */
    void drawWarpIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        for (int i = 0; i < 3; i++) {
            float off = (i - 1) * r * 0.62f;
            shapes.triangle(cx - r * 0.35f + off, cy + r * 0.75f,
                    cx + r * 0.55f + off, cy,
                    cx - r * 0.35f + off, cy - r * 0.75f);
        }
    }

    /** Icono de alerta: triangulo con exclamacion. */
    void drawAlertIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f);
        shapes.triangle(cx, cy + r, cx - r, cy - r * 0.85f, cx + r, cy - r * 0.85f);
        shapes.setColor(c);
        shapes.triangle(cx, cy + r * 0.72f, cx - r * 0.78f, cy - r * 0.68f,
                cx + r * 0.78f, cy - r * 0.68f);
        shapes.setColor(0.05f, 0.06f, 0.07f, 0.95f);
        shapes.rect(cx - r * 0.07f, cy - r * 0.25f, r * 0.14f, r * 0.55f);
        shapes.circle(cx, cy - r * 0.45f, r * 0.09f, 6);
    }

    /** Icono de sistema: engranaje simplificado. */
    void drawGearIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f);
        shapes.circle(cx, cy, r, 16);
        shapes.setColor(c);
        for (int i = 0; i < 6; i++) {
            // El origen de rotacion es relativo al rectangulo (su centro), no
            // una coordenada absoluta de pantalla.
            shapes.rect(cx - r * 0.22f, cy - r * 1.30f, r * 0.22f, r * 1.30f,
                    r * 0.44f, r * 2.60f, 1f, 1f, i * 30f);
        }
        shapes.circle(cx, cy, r * 0.72f, 14);
        shapes.setColor(0.05f, 0.07f, 0.09f, 1f);
        shapes.circle(cx, cy, r * 0.34f, 10);
    }

    /** Icono de energia: rayo. */
    void drawEnergyIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.triangle(cx + r * 0.15f, cy + r, cx - r * 0.75f, cy - r * 0.10f,
                cx - r * 0.05f, cy - r * 0.10f);
        shapes.triangle(cx - r * 0.15f, cy - r, cx + r * 0.75f, cy + r * 0.10f,
                cx + r * 0.05f, cy + r * 0.10f);
    }
}
