package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Dibujo de la tienda: pestañas, tarjetas de mejoras, reparaciones y
 * fundición. El estado y las acciones siguen en MyGame; esta clase solo lee el
 * modelo y pinta, lo que saca ~250 líneas del bucle principal.
 */
final class ShopRenderer {

    private static final Color SHOP_PANEL_C = new Color(0.020f, 0.035f, 0.080f, 0.86f);
    private static final Color REFILL_FILL_C = new Color(0.09f, 0.12f, 0.10f, 0.88f);
    private static final Color REFILL_EDGE_C = new Color(0.30f, 0.62f, 0.42f, 0.40f);
    private static final Color REPAIR_FILL_C = new Color(0.12f, 0.10f, 0.06f, 0.88f);
    private static final Color REPAIR_EDGE_C = new Color(0.88f, 0.62f, 0.24f, 0.42f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private float viewW;

    ShopRenderer(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
    }

    void draw(float w, float h) {
        viewW = w;
        drawShop(w, h);
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    void drawShop(float w, float h) {
        float camX = game.time * 10f * game.u;
        float camY = game.time * 4f * game.u;
        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        game.space.nebula(camX, camY, w, h, game.u, 0.8f);
        game.space.stars(camX, camY, w, h, game.u, 0.8f);
        batch.end();
        Visuals.normal(batch);

        float pad = 16f * game.u;
        beginShapes();
        ui.panel(pad * 0.5f, pad * 0.5f, w - pad, h - pad, 20f * game.u,
                SHOP_PANEL_C, Visuals.PANEL_EDGE);
        shapes.setColor(0.28f, 0.58f, 0.90f, 0.35f);
        shapes.rect(pad * 1.5f, h - 64f * game.u, w - 3f * pad, 2f * game.u);

        if (game.runActive) {
            ui.drawButton(game.btnTabUpgrades,
                    game.shopTab == 0 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
            ui.drawButton(game.btnTabRepairs,
                    game.shopTab == 1 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
            ui.drawButton(game.btnTabMelt,
                    game.shopTab == 2 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        }
        if (game.shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                drawShopCard(i);
            }
        } else if (game.shopTab == 1) {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                drawRepairCard(i);
            }
            drawRefillCard();
            drawO2Card();
        } else {
            for (int i = 0; i < game.meltEntries.size; i++) {
                drawMeltCard(i);
            }
        }
        float chipCx = w - pad * 1.5f - 75f * game.u;
        float chipCy = h - 34f * game.u;
        ui.coinChipShape(chipCx, chipCy);
        ui.drawButton(game.btnBack, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("TIENDA", pad * 1.5f, h - 22f * game.u, 1.55f * game.u, Color.WHITE);
        ui.coinChipText(chipCx, chipCy, game.upgrades.coins());
        if (game.runActive) {
            ui.drawButtonText(game.btnTabUpgrades, true);
            ui.drawButtonText(game.btnTabRepairs, true);
            ui.drawButtonText(game.btnTabMelt, true);
        }
        if (game.shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                drawShopCardText(i);
            }
        } else if (game.shopTab == 1) {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                drawRepairCardText(i);
            }
            drawRefillCardText();
            drawO2CardText();
        } else if (game.meltEntries.size == 0) {
            ui.textCentered("BODEGA VACIA", w * 0.5f, h * 0.5f, 1.1f * game.u, Visuals.TEXT_DIM);
        } else {
            for (int i = 0; i < game.meltEntries.size; i++) {
                drawMeltCardText(i);
            }
        }
        ui.drawButtonText(game.btnBack, true);
        batch.end();
    }

    float cardX() {
        return 22f * game.u;
    }

    float cardW(float w) {
        return w - 44f * game.u;
    }

    float rowTitleY(float top) {
        return top - Math.max(10f * game.u, game.shopRowH * 0.30f);
    }

    float rowDescY(float top) {
        return top - Math.max(26f * game.u, game.shopRowH * 0.62f);
    }

    void drawShopCard(int i) {
        float x = cardX();
        float w = cardW(viewW);
        float top = game.shopRowTop(i);
        float y = top - game.shopRowH;
        ui.panel(x, y, w, game.shopRowH, 16f * game.u, Visuals.CARD, Visuals.CARD_EDGE);
        ui.drawLevelPips(x + 18f * game.u, y + 2f * game.u, game.upgrades.level(i));
        ui.drawButton(game.buyButtons.get(i), game.upgrades.canBuy(i) ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                game.upgrades.canBuy(i));
    }

    void drawShopCardText(int i) {
        float x = cardX();
        float top = game.shopRowTop(i);
        float maxW = game.buyButtons.get(i).bounds.x - (x + 18f * game.u) - 10f * game.u;
        ui.textLeftFit(Upgrades.NAMES[i], x + 18f * game.u, rowTitleY(top), 1.02f * game.u, maxW, Visuals.TEXT);
        ui.textLeftFit(Upgrades.DESCS[i], x + 18f * game.u, rowDescY(top), 0.72f * game.u, maxW, Visuals.TEXT_DIM);
        ui.drawButtonText(game.buyButtons.get(i), game.upgrades.canBuy(i));
    }

    void drawRepairCard(int i) {
        float x = cardX();
        float w = cardW(viewW);
        float top = game.shopRowTop(i);
        float y = top - game.shopRowH;
        ui.panel(x, y, w, game.shopRowH, 16f * game.u, REPAIR_FILL_C, REPAIR_EDGE_C);
        float barX = x + 18f * game.u;
        float barY = y + 2f * game.u;
        float barW = game.repairButtons.get(i).bounds.x - barX - 12f * game.u;
        ui.drawIntegrityBar(barX, barY, barW, Math.max(3f * game.u, game.shopRowH * 0.07f),
                game.world.ship.part[i] / game.world.ship.partMax(i));
        ui.drawButton(game.repairButtons.get(i), game.canRepair(i) ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                game.canRepair(i));
    }

    void drawRepairCardText(int i) {
        float x = cardX();
        float top = game.shopRowTop(i);
        float maxW = game.repairButtons.get(i).bounds.x - (x + 18f * game.u) - 10f * game.u;
        float frac = game.world.ship.part[i] / game.world.ship.partMax(i);
        int pct = Math.round(frac * 100f);
        ui.textLeftFit(Ship.PART_NAMES[i] + "  " + pct + "%", x + 18f * game.u,
                rowTitleY(top), 1.02f * game.u, maxW, Visuals.TEXT);
        ui.textLeftFit(partEffect(i), x + 18f * game.u, rowDescY(top), 0.68f * game.u, maxW,
                frac < 0.35f ? Visuals.DANGER : Visuals.TEXT_DIM);
        ui.drawButtonText(game.repairButtons.get(i), game.canRepair(i));
    }

    void drawRefillCard() {
        float x = cardX();
        float w = cardW(viewW);
        float top = game.shopRowTop(Ship.PART_COUNT);
        float y = top - game.shopRowH;
        ui.panel(x, y, w, game.shopRowH, 16f * game.u, REFILL_FILL_C, REFILL_EDGE_C);
        ui.drawButton(game.btnRefill, game.canRefill() ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, game.canRefill());
    }

    void drawRefillCardText() {
        float x = cardX();
        float top = game.shopRowTop(Ship.PART_COUNT);
        float maxW = game.btnRefill.bounds.x - (x + 18f * game.u) - 10f * game.u;
        ui.textLeftFit("RECARGA", x + 18f * game.u, rowTitleY(top), 1.02f * game.u, maxW, Visuals.TEXT);
        ui.textLeftFit("Rellena el deposito al instante", x + 18f * game.u,
                rowDescY(top), 0.72f * game.u, maxW, Visuals.TEXT_DIM);
        ui.drawButtonText(game.btnRefill, game.canRefill());
    }

    void drawO2Card() {
        float x = cardX();
        float w = cardW(viewW);
        float top = game.shopRowTop(Ship.PART_COUNT + 1);
        float y = top - game.shopRowH;
        ui.panel(x, y, w, game.shopRowH, 16f * game.u, REFILL_FILL_C, REFILL_EDGE_C);
        ui.drawButton(game.btnO2, game.canRefillO2() ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                game.canRefillO2());
    }

    void drawO2CardText() {
        float x = cardX();
        float top = game.shopRowTop(Ship.PART_COUNT + 1);
        float maxW = game.btnO2.bounds.x - (x + 18f * game.u) - 10f * game.u;
        ui.textLeftFit("OXIGENO", x + 18f * game.u, rowTitleY(top), 1.02f * game.u, maxW, Visuals.TEXT);
        ui.textLeftFit("Rellena la reserva de O2", x + 18f * game.u,
                rowDescY(top), 0.72f * game.u, maxW, Visuals.TEXT_DIM);
        ui.drawButtonText(game.btnO2, game.canRefillO2());
    }

    void drawMeltCard(int i) {
        float x = cardX();
        float w = cardW(viewW);
        float top = game.shopRowTop(i);
        float y = top - game.shopRowH;
        ui.panel(x, y, w, game.shopRowH, 16f * game.u, REFILL_FILL_C, REFILL_EDGE_C);
        ui.drawButton(game.meltButtons.get(i), Visuals.BTN_PRIMARY, true);
    }

    void drawMeltCardText(int i) {
        Cargo entry = game.meltEntries.get(i);
        float x = cardX();
        float top = game.shopRowTop(i);
        float maxW = game.meltButtons.get(i).bounds.x - (x + 18f * game.u) - 10f * game.u;
        int value = Upgrades.meltValue(entry);
        ui.textLeftFit(entry.commodity.displayName + "  " + entry.amount + " UM",
                x + 18f * game.u, rowTitleY(top), 1.02f * game.u, maxW, Visuals.TEXT);
        ui.textLeftFit(entry.amount + " x " + entry.commodity.basePrice
                        + " cr = " + value + " cr",
                x + 18f * game.u, rowDescY(top), 0.72f * game.u, maxW, Visuals.TEXT_DIM);
        ui.drawButtonText(game.meltButtons.get(i), true);
    }

    /** Texto corto del efecto de cada parte cuando esta danada. */
    static String partEffect(int index) {
        switch (index) {
            case Ship.HULL: return "A 0 se destruye la nave";
            case Ship.ENGINE: return "Menos empuje y velocidad";
            case Ship.TANK: return "Menos capacidad y fugas";
            case Ship.CANNON: return "Menos dano y cadencia";
            case Ship.STEERING: return "Gira mas despacio";
            case Ship.LIFE_SUPPORT: return "Filtro de O2 al 20%";
            case Ship.MINING_LASER: return "Sin laser no se mina";
            default: return "Se pierde el ala y el giro";
        }
    }

}
