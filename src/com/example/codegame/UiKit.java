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

    private static final Color HULL_TRACK_C = new Color(0.04f, 0.07f, 0.12f, 0.95f);
    private static final Color COIN_CHIP_C = new Color(0.08f, 0.09f, 0.16f, 0.92f);
    private static final Color tint = new Color();
    private static final Color tint2 = new Color();

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

    void panel(float x, float y, float w, float h, float r, Color fill, Color border) {
        shapes.setColor(0f, 0f, 0f, 0.35f);
        Visuals.roundRect(shapes, x + 2f * u, y - 4f * u, w, h, r);
        shapes.setColor(border);
        Visuals.roundRect(shapes, x - 1.5f * u, y - 1.5f * u, w + 3f * u, h + 3f * u, r + 1.5f * u);
        shapes.setColor(fill);
        Visuals.roundRect(shapes, x, y, w, h, r);
    }

    void drawButton(Button b, Color fill, boolean enabled) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = Math.min(h * 0.30f, 20f * u);
        boolean isPressed = enabled && pressed != null && pressed.is(b);

        shapes.setColor(0f, 0f, 0f, 0.38f);
        Visuals.roundRect(shapes, x + 2f * u, y - 5f * u, w, h, r);

        Color body = fill;
        if (!enabled) {
            body = Visuals.BTN_OFF;
        } else if (isPressed) {
            body = tint2.set(fill).lerp(Color.WHITE, 0.16f);
        }
        float br = Math.min(1f, body.r * 1.5f + 0.18f);
        float bg = Math.min(1f, body.g * 1.5f + 0.18f);
        float bb = Math.min(1f, body.b * 1.5f + 0.18f);
        shapes.setColor(br, bg, bb, enabled ? 0.75f : 0.45f);
        Visuals.roundRect(shapes, x - 2f * u, y - 2f * u, w + 4f * u, h + 4f * u, r + 2f * u);

        shapes.setColor(body);
        Visuals.roundRect(shapes, x, y, w, h, r);
        // Brillo superior y base en sombra.
        shapes.setColor(1f, 1f, 1f, enabled ? 0.12f : 0.06f);
        Visuals.roundRect(shapes, x + 3f * u, y + h * 0.56f, w - 6f * u, h * 0.38f, r * 0.75f);
        shapes.setColor(0f, 0f, 0f, 0.18f);
        Visuals.roundRect(shapes, x + 3f * u, y + 2f * u, w - 6f * u, h * 0.20f, r * 0.55f);
    }

    void drawButtonText(Button b, boolean enabled) {
        b.drawText(batch, font, layout, buttonTextScale, enabled ? Visuals.TEXT : Visuals.TEXT_OFF);
    }

    void coinChipShape(float cx, float cy) {
        float cw = 150f * u;
        float ch = 42f * u;
        panel(cx - cw * 0.5f, cy - ch * 0.5f, cw, ch, ch * 0.5f,
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
}
