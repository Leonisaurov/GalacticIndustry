package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** Botón rectangular con texto centrado. Se dibuja en dos pasadas: formas y texto. */
class Button {

    static final Color FILL = new Color(0.2f, 0.32f, 0.6f, 0.85f);

    final Rectangle bounds;
    final String label;

    Button(String label, float x, float y, float w, float h) {
        this.label = label;
        this.bounds = new Rectangle(x, y, w, h);
    }

    boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    void drawShape(ShapeRenderer shapes, Color color) {
        shapes.setColor(color);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /** Escala del texto de los botones (la fuente por defecto es pequeña). */
    static final float TEXT_SCALE = 1.4f;

    void drawText(SpriteBatch batch, BitmapFont font, GlyphLayout layout) {
        font.getData().setScale(TEXT_SCALE);
        layout.setText(font, label);
        float x = bounds.x + (bounds.width - layout.width) / 2f;
        float y = bounds.y + (bounds.height + layout.height) / 2f;
        font.setColor(0f, 0f, 0f, 0.5f);
        font.draw(batch, label, x + 2f, y - 2f);
        font.setColor(Color.WHITE);
        font.draw(batch, label, x, y);
        font.getData().setScale(1f);
    }
}
