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

    private static final Color PANEL_C = new Color(0.030f, 0.042f, 0.058f, 0.95f);
    private static final Color CARD_C = new Color(0.060f, 0.080f, 0.100f, 0.94f);
    private static final Color CARD_EDGE_C = new Color(0.24f, 0.33f, 0.40f, 0.50f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Array<Button> rowButtons = new Array<>();
    private final Array<Button> tabButtons = new Array<>();
    private Button btnUndock;
    private int tab;
    private String message = "";
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
        message = "";
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
        float tabW = (pw - 70f * u) / 4f;
        tabButtons.add(new Button("SERVICIOS", px + 20f * u, tabY, tabW, tabH));
        tabButtons.add(new Button("MEJORAS", px + 30f * u + tabW, tabY, tabW, tabH));
        tabButtons.add(new Button("MODULOS", px + 40f * u + 2f * tabW, tabY, tabW, tabH));
        tabButtons.add(new Button("MERCADO", px + 50f * u + 3f * tabW, tabY, tabW, tabH));

        rows = tab == 0 ? 5 : tab == 1 ? Upgrades.COUNT : ShipModuleDef.values().length;
        listTop = h - 160f * u;
        float bottom = py + 100f * u;
        rowGap = 8f * u;
        rowH = Math.min(200f * u,
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
        for (int i = 0; i < 3; i++) {
            if (!game.tapped(tabButtons.get(i))) {
                continue;
            }
            if (tab != i) {
                tab = i;
                message = "";
                layout();
            }
            return true;
        }
        if (game.tapped(tabButtons.get(3))) {
            message = "";
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
            if (!rowEnabled(i)) {
                message = rowLockReason(i);
                return true;
            }
            if (tab == 0) {
                service(i);
            } else if (tab == 1) {
                upgrade(i);
            } else {
                toggleModule(i);
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
                message = "DESCARGADAS " + moved + " UM";
                game.saveRun();
                game.logEvent("{\"event\":\"store\",\"units\":" + moved + "}");
            }
        } else if (index == 1) {
            int credits = station.storage.sellAll();
            if (credits > 0) {
                up.addCoins(credits);
                message = "VENDIDO POR " + credits + " CR";
                game.saveRun();
                game.logEvent("{\"event\":\"sell\",\"credits\":" + credits
                        + ",\"coins\":" + up.coins() + "}");
            }
        } else if (index == 2) {
            int cost = fuelCost();
            if (cost > 0 && up.spendCoins(cost)) {
                ship.refuel(ship.maxFuel);
                message = "COMBUSTIBLE COMPLETO";
                game.saveRun();
                game.logEvent("{\"event\":\"station_fuel\",\"cost\":" + cost + "}");
            }
        } else if (index == 3) {
            int cost = o2Cost();
            if (cost > 0 && up.spendCoins(cost)) {
                game.session.pilot.oxygen = game.session.pilot.o2Max;
                message = "OXIGENO COMPLETO";
                game.saveRun();
                game.logEvent("{\"event\":\"station_o2\",\"cost\":" + cost + "}");
            }
        } else if (index == 4) {
            int cost = repairAllCost();
            if (cost > 0 && up.spendCoins(cost)) {
                for (int i = 0; i < Ship.PART_COUNT; i++) {
                    game.repairPart(i);
                }
                message = "NAVE REPARADA";
                game.saveRun();
                game.logEvent("{\"event\":\"station_repair\",\"cost\":" + cost + "}");
            }
        }
    }

    private void upgrade(int index) {
        if (game.upgrades.buy(index)) {
            message = "MEJORA COMPRADA";
            game.applyUpgradeToRun(index);
        }
    }

    /** Instala el módulo en la primera ranura libre o retira el que ya está. */
    private void toggleModule(int index) {
        ShipModuleDef def = ShipModuleDef.values()[index];
        int installed = game.shipModuleSlot(def);
        if (installed >= 0) {
            if (game.removeShipModule(installed)) {
                message = "RETIRADO (SIN REEMBOLSO)";
            } else {
                message = "NO SE PUDO RETIRAR";
            }
            return;
        }
        int free = game.freeShipModuleSlot();
        if (free < 0) {
            message = "SIN RANURA LIBRE";
            return;
        }
        if (game.upgrades.coins() < def.creditCost) {
            message = "SIN CREDITOS";
            return;
        }
        if (game.installShipModule(free, def)) {
            message = "INSTALADO EN RANURA " + (free + 1);
        } else {
            message = "NO SE PUDO INSTALAR";
        }
    }

    private String rowLockReason(int index) {
        if (tab == 1) {
            if (game.upgrades.level(index) >= Upgrades.MAX_LEVEL) {
                return "NIVEL MAXIMO";
            }
            return "SIN CREDITOS";
        }
        if (tab == 2) {
            ShipModuleDef def = ShipModuleDef.values()[index];
            if (game.shipModuleSlot(def) >= 0) {
                return "INSTALADO";
            }
            if (game.freeShipModuleSlot() < 0) {
                return "SIN RANURA LIBRE";
            }
            return "SIN CREDITOS";
        }
        if (index == 0) {
            return "BODEGA VACIA";
        }
        if (index == 1) {
            return "ALMACEN VACIO";
        }
        if (index == 2) {
            return fuelCost() <= 0 ? "DEPOSITO LLENO" : "SIN CREDITOS";
        }
        if (index == 3) {
            return o2Cost() <= 0 ? "OXIGENO LLENO" : "SIN CREDITOS";
        }
        return repairAllCost() <= 0 ? "SIN DANOS" : "SIN CREDITOS";
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
        ui.grid(w, h, 80f * u, 0.018f);
        ui.frame(px, py, pw, ph, 14f * u, PANEL_C, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.divider(px + 24f * u, h - 64f * u, pw - 48f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        for (int i = 0; i < tabButtons.size; i++) {
            ui.drawButton(tabButtons.get(i),
                    tab == i ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        }
        for (int i = 0; i < rowButtons.size; i++) {
            boolean enabled = rowEnabled(i);
            Button b = rowButtons.get(i);
            ui.panel(b.bounds.x - 6f * u, b.bounds.y - 6f * u,
                    b.bounds.width + 12f * u, b.bounds.height + 12f * u, 7f * u,
                    CARD_C, enabled ? CARD_EDGE_C : Visuals.PANEL_EDGE);
            float mid = b.bounds.y + b.bounds.height * 0.5f;
            ui.led(b.bounds.x + 14f * u, mid, 3.2f * u,
                    enabled ? Visuals.ACCENT : Visuals.LED_OFF);
            drawRowIcon(i, b.bounds.x + 40f * u, mid, 11f * u,
                    enabled ? Visuals.ACCENT : Visuals.TEXT_OFF);
            if (tab == 1) {
                ui.drawLevelPips(b.bounds.x + 76f * u, b.bounds.y + 16f * u,
                        game.upgrades.level(i));
            }
            shapes.setColor(0f, 0f, 0f, 0.30f);
            shapes.rect(b.bounds.x + 62f * u, b.bounds.y + 12f * u, 1.2f * u,
                    b.bounds.height - 24f * u);
            // Chip de coste alineado a la derecha.
            shapes.setColor(0.030f, 0.042f, 0.055f, 0.85f);
            Visuals.roundRect(shapes, b.bounds.x + b.bounds.width - 156f * u,
                    mid - 16f * u, 140f * u, 32f * u, 4f * u);
        }
        ui.divider(px + 24f * u, py + 96f * u, pw - 48f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.drawButton(btnUndock, Visuals.BTN_PRIMARY, true);
        shapes.end();

        batch.begin();
        float titleY = h - 34f * u;
        ui.textLeft("ESTACION " + game.world.station.def.name,
                px + 28f * u, titleY, 0.88f * u, Visuals.TEXT);
        drawCoins(w, h);
        drawTabs();
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, w * 0.5f, h - 112f * u,
                    0.70f * u, pw - 40f * u, Visuals.ACCENT);
        } else if (tab == 2) {
            ui.textCenteredFit("MODULOS " + game.upgrades.shipModuleCount() + "/"
                            + game.upgrades.loadout().moduleSlots()
                            + "   -   RETIRAR NO REEMBOLSA",
                    w * 0.5f, h - 112f * u, 0.66f * u, pw - 40f * u,
                    Visuals.TEXT_DIM);
        }
        drawRows();
        ui.drawButtonText(btnUndock, true);
        batch.end();
    }

    private void drawRowIcon(int index, float cx, float cy, float r, Color c) {
        if (tab == 1) {
            ui.drawGearIcon(cx, cy, r, c);
            return;
        }
        if (tab == 2) {
            drawModuleRowIcon(ShipModuleDef.values()[index], cx, cy, r, c);
            return;
        }
        switch (index) {
            case 0:
                ui.drawCargoIcon(cx, cy, r, c);
                break;
            case 1:
                ui.drawOreIcon(cx, cy, r, c);
                break;
            case 2:
                ui.drawFuelIcon(cx, cy, r);
                break;
            case 3:
                shapes.setColor(c);
                shapes.circle(cx, cy, r * 0.72f, 12);
                shapes.setColor(0.03f, 0.04f, 0.05f, 1f);
                shapes.circle(cx, cy, r * 0.34f, 8);
                break;
            default:
                ui.drawGearIcon(cx, cy, r, c);
                break;
        }
    }

    private void drawCoins(float w, float h) {
        int coins = game.upgrades.coins();
        ui.textRight("CREDITOS " + coins, px + pw - 28f * game.u,
                h - 34f * game.u, 0.78f * game.u, Visuals.GOLD);
    }

    private void drawTabs() {
        for (int i = 0; i < tabButtons.size; i++) {
            ui.drawButtonText(tabButtons.get(i), true);
        }
    }

    private void drawRows() {
        float u = game.u;
        if (tab == 0) {
            drawServiceRow(0, "DESCARGAR BODEGA", "Mineral en la nave", cargoUnits() + " UM");
            drawServiceRow(1, "VENDER ALMACEN", "Valor del almacen",
                    Math.round(game.world.station.storage.totalValue()) + " CR");
            drawServiceRow(2, "REPOSTAR COMBUSTIBLE", "Deposito al maximo", fuelCost() + " CR");
            drawServiceRow(3, "RELLENAR OXIGENO", "Soporte vital", o2Cost() + " CR");
            drawServiceRow(4, "REPARAR TODO", "Casco y sistemas", repairAllCost() + " CR");
        } else if (tab == 1) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                Button b = rowButtons.get(i);
                float x = b.bounds.x + 76f * u;
                float mid = b.bounds.y + b.bounds.height * 0.5f;
                ui.textLeftFit(Upgrades.NAMES[i], x, mid + 18f * u,
                        0.95f * u, b.bounds.width - 250f * u,
                        rowEnabled(i) ? Visuals.TEXT : Visuals.TEXT_OFF);
                ui.textLeftFit(Upgrades.DESCS[i], x, mid - 8f * u,
                        0.68f * u, b.bounds.width - 250f * u, Visuals.TEXT_DIM);
                String label = game.upgrades.level(i) >= Upgrades.MAX_LEVEL
                        ? "MAX" : "COMPRAR " + game.upgrades.cost(i);
                ui.textRight(label, b.bounds.x + b.bounds.width - 26f * u,
                        mid + 8f * u, 0.82f * u,
                        rowEnabled(i) ? Visuals.GOLD : Visuals.TEXT_OFF);
            }
        } else {
            drawModuleRows();
        }
    }

    private void drawModuleRows() {
        float u = game.u;
        for (int i = 0; i < ShipModuleDef.values().length; i++) {
            ShipModuleDef def = ShipModuleDef.values()[i];
            Button b = rowButtons.get(i);
            int slot = game.shipModuleSlot(def);
            boolean can = rowEnabled(i);
            float x = b.bounds.x + 76f * u;
            float mid = b.bounds.y + b.bounds.height * 0.5f;
            ui.textLeftFit(def.displayName, x, mid + 18f * u, 0.95f * u,
                    b.bounds.width - 300f * u, can ? Visuals.TEXT : Visuals.TEXT_OFF);
            ui.textLeftFit(def.description, x, mid - 8f * u, 0.68f * u,
                    b.bounds.width - 300f * u, Visuals.TEXT_DIM);
            String status;
            Color color;
            if (slot >= 0) {
                status = "INSTALADO R" + (slot + 1);
                color = Visuals.GOOD;
                ui.textRight(status, b.bounds.x + b.bounds.width - 26f * u,
                        mid + 10f * u, 0.74f * u, color);
                ui.textRight("RETIRAR", b.bounds.x + b.bounds.width - 26f * u,
                        mid - 14f * u, 0.66f * u, Visuals.TEXT_DIM);
                continue;
            } else if (game.freeShipModuleSlot() < 0) {
                status = "SIN RANURA LIBRE";
                color = Visuals.TEXT_OFF;
            } else {
                status = "INSTALAR " + def.creditCost + " CR";
                color = can ? Visuals.GOLD : Visuals.TEXT_OFF;
            }
            ui.textRight(status, b.bounds.x + b.bounds.width - 26f * u,
                    mid + 8f * u, 0.80f * u, color);
        }
    }

    private void drawModuleRowIcon(ShipModuleDef def, float cx, float cy,
                                   float r, Color c) {
        switch (def) {
            case AUTOPILOT:
                ui.drawWarpIcon(cx, cy, r, c);
                break;
            case RADAR:
                ui.drawStationIcon(cx, cy, r * 0.82f, c);
                break;
            case MINING:
                ui.drawEnergyIcon(cx, cy, r, c);
                break;
            case PROPULSION:
                ui.drawFuelIcon(cx, cy, r);
                break;
            case TOOLS:
            default:
                ui.drawGearIcon(cx, cy, r, c);
                break;
        }
    }

    private void drawServiceRow(int index, String title, String desc, String status) {
        Button b = rowButtons.get(index);
        float u = game.u;
        float x = b.bounds.x + 76f * u;
        float mid = b.bounds.y + b.bounds.height * 0.5f;
        ui.textLeftFit(title, x, mid + 18f * u, 0.95f * u,
                b.bounds.width * 0.56f, rowEnabled(index) ? Visuals.TEXT : Visuals.TEXT_OFF);
        ui.textLeftFit(desc, x, mid - 8f * u, 0.70f * u,
                b.bounds.width * 0.56f, Visuals.TEXT_DIM);
        ui.textRight(status, b.bounds.x + b.bounds.width - 26f * u,
                mid + 8f * u, 0.86f * u,
                rowEnabled(index) ? Visuals.GOLD : Visuals.TEXT_OFF);
    }

    private boolean rowEnabled(int index) {
        if (tab == 1) {
            return game.upgrades.canBuy(index);
        }
        if (tab == 2) {
            ShipModuleDef def = ShipModuleDef.values()[index];
            if (game.shipModuleSlot(def) >= 0) {
                return true;
            }
            return game.freeShipModuleSlot() >= 0
                    && game.upgrades.coins() >= def.creditCost;
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
