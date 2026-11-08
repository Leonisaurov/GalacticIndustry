package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Pantalla de la estación acoplada: descarga y venta de mineral, combustible,
 * O2, reparaciones y mejoras. La economía de pausa desaparece en T-504; esta
 * pantalla es la vía principal desde la Etapa 5.
 */
final class StationScreen {

    private static final Color PANEL_C = new Color(0.020f, 0.035f, 0.080f, 0.90f);
    private static final Color CARD_C = new Color(0.07f, 0.11f, 0.21f, 0.92f);
    private static final Color CARD_EDGE_C = new Color(0.26f, 0.48f, 0.75f, 0.40f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Array<Button> rowButtons = new Array<>();
    private final Array<Button> tabButtons = new Array<>();
    private Button btnUndock;
    private int tab;
    private float px;
    private float py;
    private float pw;
    private float ph;
    private float listTop;
    private float rowH;
    private float rowGap;
    private int rows;

    StationScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes,
                  SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
    }

    void enter() {
        tab = 0;
        layout();
    }

    void layout() {
        layout(game.hudView.getWorldWidth(), game.hudView.getWorldHeight());
    }

    void layout(float w, float h) {
        float u = game.u;
        float pad = 16f * u;
        px = pad * 0.5f;
        py = pad * 0.5f;
        pw = w - pad;
        ph = h - pad;

        tabButtons.clear();
        float tabH = 40f * u;
        float tabY = h - 96f * u;
        float tabW = (pw - 60f * u) / 3f;
        tabButtons.add(new Button("SERVICIOS", px + 20f * u, tabY, tabW, tabH));
        tabButtons.add(new Button("MEJORAS", px + 30f * u + tabW, tabY, tabW, tabH));
        tabButtons.add(new Button("MERCADO", px + 40f * u + 2f * tabW, tabY, tabW, tabH));

        rows = tab == 0 ? 5 : Upgrades.COUNT;
        listTop = h - 160f * u;
        float bottom = py + 100f * u;
        rowGap = 8f * u;
        rowH = Math.min(86f * u,
                (listTop - bottom - (rows - 1) * rowGap) / rows);
        rowH = Math.max(40f * u, rowH);

        rowButtons.clear();
        for (int i = 0; i < rows; i++) {
            float top = rowTop(i);
            rowButtons.add(new Button("", px + 24f * u, top - rowH,
                    pw - 48f * u, rowH));
        }
        btnUndock = new Button("DESPEGAR", (w - Math.min(w * 0.6f, 280f * u)) * 0.5f,
                py + 34f * u, Math.min(w * 0.6f, 280f * u), 52f * u);
    }

    private float rowTop(int index) {
        return listTop - index * (rowH + rowGap);
    }

    boolean handle() {
        if (btnUndock == null) {
            layout();
        }
        if (game.tapped(tabButtons.get(0))) {
            if (tab != 0) {
                tab = 0;
                layout();
            }
            return true;
        }
        if (game.tapped(tabButtons.get(1))) {
            if (tab != 1) {
                tab = 1;
                layout();
            }
            return true;
        }
        if (game.tapped(tabButtons.get(2))) {
            game.openMarket();
            return true;
        }
        if (game.tapped(btnUndock)) {
            game.undockFromStation();
            return true;
        }
        for (int i = 0; i < rowButtons.size; i++) {
            if (!game.tapped(rowButtons.get(i))) {
                continue;
            }
            if (tab == 0) {
                service(i);
            } else {
                upgrade(i);
            }
            return true;
        }
        return false;
    }

    private void service(int index) {
        Station station = game.world.station;
        Ship ship = game.world.ship;
        Upgrades up = game.upgrades;
        if (index == 0) {
            int moved = station.storage.depositAll(ship.cargo());
            if (moved > 0) {
                game.saveRun();
                game.logEvent("{\"event\":\"store\",\"units\":" + moved + "}");
            }
        } else if (index == 1) {
            int credits = station.storage.sellAll();
            if (credits > 0) {
                up.addCoins(credits);
                game.saveRun();
                game.logEvent("{\"event\":\"sell\",\"credits\":" + credits
                        + ",\"coins\":" + up.coins() + "}");
            }
        } else if (index == 2) {
            int cost = fuelCost();
            if (cost > 0 && up.spendCoins(cost)) {
                ship.refuel(ship.maxFuel);
                game.saveRun();
                game.logEvent("{\"event\":\"station_fuel\",\"cost\":" + cost + "}");
            }
        } else if (index == 3) {
            int cost = o2Cost();
            if (cost > 0 && up.spendCoins(cost)) {
                game.session.pilot.oxygen = game.session.pilot.o2Max;
                game.saveRun();
                game.logEvent("{\"event\":\"station_o2\",\"cost\":" + cost + "}");
            }
        } else if (index == 4) {
            int cost = repairAllCost();
            if (cost > 0 && up.spendCoins(cost)) {
                for (int i = 0; i < Ship.PART_COUNT; i++) {
                    game.repairPart(i);
                }
                game.saveRun();
                game.logEvent("{\"event\":\"station_repair\",\"cost\":" + cost + "}");
            }
        }
    }

    private void upgrade(int index) {
        if (game.upgrades.buy(index)) {
            game.applyUpgradeToRun(index);
        }
    }

    private int fuelCost() {
        float missing = game.world.ship.maxFuel - game.world.ship.fuel;
        return game.upgrades.refillCost(missing);
    }

    private int o2Cost() {
        float missing = game.session.pilot.o2Max - game.session.pilot.oxygen;
        return game.upgrades.o2RefillCost(missing);
    }

    private int repairAllCost() {
        int total = 0;
        for (int i = 0; i < Ship.PART_COUNT; i++) {
            total += game.repairCost(i);
        }
        return total;
    }

    void draw(float w, float h) {
        if (btnUndock == null) {
            layout(w, h);
        }
        float u = game.u;
        float camX = game.time * 12f * u;
        float camY = game.time * 5f * u;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        space.stars(w * 0.5f + camX, h * 0.5f + camY, w, h, u, 0.9f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        ui.panel(px, py, pw, ph, 20f * u, PANEL_C, Visuals.PANEL_EDGE);
        shapes.setColor(0.28f, 0.58f, 0.90f, 0.35f);
        shapes.rect(px + 20f * u, h - 64f * u, pw - 40f * u, 2f * u);
        ui.drawButton(tabButtons.get(0),
                tab == 0 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        ui.drawButton(tabButtons.get(1),
                tab == 1 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        ui.drawButton(tabButtons.get(2), Visuals.BTN_SECONDARY, true);
        for (int i = 0; i < rowButtons.size; i++) {
            boolean enabled = rowEnabled(i);
            ui.panel(rowButtons.get(i).bounds.x - 6f * u,
                    rowButtons.get(i).bounds.y - 6f * u,
                    rowButtons.get(i).bounds.width + 12f * u,
                    rowButtons.get(i).bounds.height + 12f * u, 14f * u,
                    CARD_C, enabled ? CARD_EDGE_C : Visuals.PANEL_EDGE);
        }
        ui.drawButton(btnUndock, Visuals.BTN_PRIMARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("ESTACION " + game.world.station.def.name,
                px + 28f * u, h - 34f * u, 0.88f * u, Visuals.ACCENT);
        drawCoins(w, h);
        drawTabs();
        drawRows();
        ui.drawButtonText(btnUndock, true);
        batch.end();
    }

    private void drawCoins(float w, float h) {
        int coins = game.upgrades.coins();
        ui.textLeft("CREDITOS " + coins, px + pw - 150f * game.u,
                h - 34f * game.u, 0.76f * game.u, Visuals.GOLD);
    }

    private void drawTabs() {
        ui.drawButtonText(tabButtons.get(0), true);
        ui.drawButtonText(tabButtons.get(1), true);
        ui.drawButtonText(tabButtons.get(2), true);
    }

    private void drawRows() {
        float u = game.u;
        if (tab == 0) {
            drawServiceRow(0, "DESCARGAR BODEGA", cargoUnits() + " UM en la nave");
            drawServiceRow(1, "VENDER ALMACEN",
                    Math.round(game.world.station.storage.totalValue()) + " cr a precio base");
            drawServiceRow(2, "REPOSTAR COMBUSTIBLE",
                    "Coste " + fuelCost() + " cr");
            drawServiceRow(3, "RELLENAR OXIGENO", "Coste " + o2Cost() + " cr");
            drawServiceRow(4, "REPARAR TODO", "Coste " + repairAllCost() + " cr");
        } else {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                Button b = rowButtons.get(i);
                float x = b.bounds.x + 18f * u;
                float top = b.bounds.y + b.bounds.height;
                ui.textLeftFit(Upgrades.NAMES[i], x, top - 20f * u,
                        0.95f * u, b.bounds.width - 30f * u, Visuals.TEXT);
                ui.textLeftFit(Upgrades.DESCS[i], x, b.bounds.y + 20f * u,
                        0.68f * u, b.bounds.width - 180f * u, Visuals.TEXT_DIM);
                String label = game.upgrades.level(i) >= Upgrades.MAX_LEVEL
                        ? "MAX" : "COMPRAR " + game.upgrades.cost(i);
                ui.textLeft(label, b.bounds.x + b.bounds.width - 150f * u,
                        b.bounds.y + b.bounds.height * 0.5f + 5f * u,
                        0.82f * u, rowEnabled(i) ? Visuals.GOLD : Visuals.TEXT_OFF);
            }
        }
    }

    private void drawServiceRow(int index, String title, String desc) {
        Button b = rowButtons.get(index);
        float u = game.u;
        float x = b.bounds.x + 18f * u;
        float top = b.bounds.y + b.bounds.height;
        ui.textLeftFit(title, x, top - 22f * u, 0.95f * u,
                b.bounds.width - 30f * u, rowEnabled(index) ? Visuals.TEXT : Visuals.TEXT_OFF);
        ui.textLeftFit(desc, x, b.bounds.y + 20f * u, 0.70f * u,
                b.bounds.width - 30f * u, Visuals.TEXT_DIM);
    }

    private boolean rowEnabled(int index) {
        if (tab == 1) {
            return game.upgrades.canBuy(index);
        }
        if (index == 0) {
            return cargoUnits() > 0;
        }
        if (index == 1) {
            return !game.world.station.storage.isEmpty();
        }
        if (index == 2) {
            int cost = fuelCost();
            return cost > 0 && game.upgrades.coins() >= cost;
        }
        if (index == 3) {
            int cost = o2Cost();
            return cost > 0 && game.upgrades.coins() >= cost;
        }
        int cost = repairAllCost();
        return cost > 0 && game.upgrades.coins() >= cost;
    }

    private int cargoUnits() {
        return game.world.ship.cargo().totalUnits();
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
