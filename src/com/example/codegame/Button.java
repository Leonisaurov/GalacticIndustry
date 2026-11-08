package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** Botón rectangular con texto centrado. Se dibuja en dos pasadas: formas y texto. */
class Button {

    final Rectangle bounds;
    String label;

    Button(String label, float x, float y, float w, float h) {
        this.label = label;
        this.bounds = new Rectangle(x, y, w, h);
    }

    boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    /** Texto con sombra, reduciendo la escala si no cabe en el botón. */
    void drawText(SpriteBatch batch, BitmapFont font, GlyphLayout layout, float scale, Color color) {
        float s = scale;
        float maxWidth = bounds.width * 0.86f;
        font.getData().setScale(s);
        layout.setText(font, label);
        if (layout.width > maxWidth && layout.width > 0f) {
            s *= maxWidth / layout.width;
            font.getData().setScale(s);
            layout.setText(font, label);
        }
        float x = bounds.x + (bounds.width - layout.width) * 0.5f;
        float y = bounds.y + (bounds.height + layout.height) * 0.5f;
        float sh = Math.max(2f, s * 1.1f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, label, x + sh, y - sh);
        font.setColor(color);
        font.draw(batch, label, x, y);
        font.getData().setScale(1f);
    }
}
