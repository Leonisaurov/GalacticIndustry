package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Pantalla de gestión de la estación personal: ranuras y módulos, construcción,
 * ampliación, descarga de la bodega y reparación. Todo servicio depende de los
 * módulos construidos; la plataforma vacía no ofrece nada.
 */
final class StationManagementScreen {

    private static final Color PANEL_C = new Color(0.020f, 0.035f, 0.080f, 0.94f);
    private static final Color CARD_C = new Color(0.07f, 0.11f, 0.21f, 0.94f);
    private static final Color CARD_SEL_C = new Color(0.10f, 0.22f, 0.38f, 0.96f);
    private static final Color CARD_EDGE_C = new Color(0.26f, 0.48f, 0.75f, 0.40f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Array<Button> slotButtons = new Array<>();
    private final Array<Button> catalogButtons = new Array<>();
    private final ModuleDef[] catalog = {
            ModuleDef.STORAGE, ModuleDef.REPAIR, ModuleDef.REFINERY, ModuleDef.MARKET
    };
    private Button btnBuild, btnUpgrade, btnDeposit, btnRepair, btnUndock;
    private Button btnRefine, btnMarket;
    private Button btnEnter;
    private int selectedSlot;
    private int selectedCatalog;
    private boolean fromInterior;
    private String message = "";
    private float px, py, pw, ph, listTop, rowH, rowGap;

    StationManagementScreen(MyGame game, UiKit ui, SpriteBatch batch,
                            ShapeRenderer shapes, SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
    }

    void enter() {
        enter(false);
    }

    void enter(boolean fromInterior) {
        this.fromInterior = fromInterior;
        selectedSlot = 0;
        selectedCatalog = 0;
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

        listTop = h - 116f * u;
        float bottom = py + 280f * u;
        rowGap = 6f * u;
        rowH = Math.max(34f * u, Math.min(62f * u,
                (listTop - bottom - 5 * rowGap) / 6f));
        slotButtons.clear();
        for (int i = 0; i < PlayerStation.SLOTS; i++) {
            float top = listTop - i * (rowH + rowGap);
            slotButtons.add(new Button("", px + 20f * u, top - rowH,
                    pw - 40f * u, rowH));
        }
        float catW = (pw - 40f * u) / 4f;
        catalogButtons.clear();
        for (int i = 0; i < 4; i++) {
            catalogButtons.add(new Button("", px + 20f * u + i * catW,
                    py + 226f * u, catW - 4f * u, 40f * u));
        }
        float halfW = (pw - 48f * u) * 0.5f;
        btnBuild = new Button("CONSTRUIR", px + 20f * u, py + 176f * u,
                halfW, 42f * u);
        btnUpgrade = new Button("MEJORAR", px + 20f * u + halfW + 8f * u,
                py + 176f * u, halfW, 42f * u);
        btnRefine = new Button("REFINAR", px + 20f * u, py + 128f * u,
                halfW, 42f * u);
        btnMarket = new Button("MERCADO", px + 20f * u + halfW + 8f * u,
                py + 128f * u, halfW, 42f * u);
        btnDeposit = new Button("DESCARGAR", px + 20f * u, py + 80f * u,
                halfW, 42f * u);
        btnRepair = new Button("REPARAR", px + 20f * u + halfW + 8f * u,
                py + 80f * u, halfW, 42f * u);
        btnUndock = new Button(fromInterior ? "VOLVER" : "DESPEGAR",
                px + 20f * u, py + 28f * u, pw - 40f * u, 44f * u);
        btnEnter = new Button("ENTRAR", px + pw - 118f * u, h - 62f * u,
                98f * u, 44f * u);
    }

    int selectedSlot() {
        return selectedSlot;
    }

    String message() {
        return message;
    }

    boolean handle() {
        if (btnUndock == null) {
            layout();
        }
        if (game.tapped(btnEnter)) {
            game.enterInterior();
            return true;
        }
        if (game.tapped(btnUndock)) {
            game.managementBack();
            return true;
        }
        for (int i = 0; i < slotButtons.size; i++) {
            if (game.tapped(slotButtons.get(i))) {
                selectedSlot = i;
                message = "";
                return true;
            }
        }
        for (int i = 0; i < catalogButtons.size; i++) {
            if (game.tapped(catalogButtons.get(i))) {
                selectedCatalog = i;
                message = "";
                return true;
            }
        }
        if (game.tapped(btnBuild)) {
            build();
            return true;
        }
        if (game.tapped(btnUpgrade)) {
            upgrade();
            return true;
        }
        if (game.tapped(btnDeposit)) {
            deposit();
            return true;
        }
        if (game.tapped(btnRepair)) {
            repair();
            return true;
        }
        if (game.tapped(btnRefine)) {
            refine();
            return true;
        }
        if (game.tapped(btnMarket)) {
            marketMessage();
            return true;
        }
        return false;
    }

    private void build() {
        if (game.session.playerStation.moduleAt(selectedSlot) != null) {
            message = "RANURA OCUPADA";
            return;
        }
        PlayerStation.BuildResult result = game.session.playerStation.build(
                selectedSlot, catalog[selectedCatalog], game.upgrades,
                game.world.ship.cargo());
        message = result.ok ? "CONSTRUIDO" : result.error;
        if (result.ok) {
            game.playSfx(AudioService.Sfx.MODULE);
            game.saveRun();
        }
    }

    private void upgrade() {
        PlayerStation.BuildResult result = game.session.playerStation.upgrade(
                selectedSlot, game.upgrades, game.world.ship.cargo());
        message = result.ok ? "MEJORADO" : result.error;
        if (result.ok) {
            game.playSfx(AudioService.Sfx.MODULE);
            game.saveRun();
        }
    }

    private void deposit() {
        int moved = game.session.playerStation.depositAll(
                game.world.ship.cargo());
        message = moved > 0 ? "DESCARGADAS " + moved + " UM" : "ALMACEN NO DISPONIBLE";
        if (moved > 0) {
            game.saveRun();
        }
    }

    private void refine() {
        int batches = game.session.playerStation.refine(
                game.world.ship.cargo(), game.world.ship, game.session.pilot);
        if (batches > 0) {
            message = "REFINADOS " + batches + " LOTES";
            game.saveRun();
        } else {
            message = "SIN REFINERIA O SIN HIELO";
        }
    }

    private void marketMessage() {
        Market market = game.session.playerStation.tradeMarket();
        if (market == null) {
            message = "SIN MERCADO";
        } else {
            message = String.format(java.util.Locale.US,
                    "MERCADO: HIELO C%.1f V%.1f",
                    market.buyPrice(Commodity.HIELO),
                    market.sellPrice(Commodity.HIELO));
        }
    }

    private void repair() {
        if (game.session.playerStation.repairAll(game.world.ship,
                game.upgrades)) {
            message = "NAVE REPARADA";
            game.saveRun();
        } else {
            message = "SIN TALLER O SIN DANOS";
        }
    }

    void draw(float w, float h) {
        if (btnUndock == null) {
            layout(w, h);
        }
        float u = game.u;
        float camX = game.time * 9f * u;
        float camY = game.time * 4f * u;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();
        batch.begin();
        Visuals.additive(batch);
        space.stars(w * 0.5f + camX, h * 0.5f + camY, w, h, u, 0.8f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        ui.panel(px, py, pw, ph, 20f * u, PANEL_C, Visuals.PANEL_EDGE);
        for (int i = 0; i < slotButtons.size; i++) {
            Button b = slotButtons.get(i);
            ui.panel(b.bounds.x - 4f * u, b.bounds.y - 4f * u,
                    b.bounds.width + 8f * u, b.bounds.height + 8f * u,
                    10f * u, i == selectedSlot ? CARD_SEL_C : CARD_C,
                    i == selectedSlot ? Visuals.ACCENT : CARD_EDGE_C);
        }
        for (int i = 0; i < catalogButtons.size; i++) {
            boolean can = game.session.playerStation.moduleAt(
                    selectedSlot) == null && selectedCatalog == i;
            ui.drawButton(catalogButtons.get(i),
                    can ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        }
        ui.drawButton(btnBuild, Visuals.BTN_PRIMARY,
                game.session.playerStation.moduleAt(selectedSlot) == null);
        ui.drawButton(btnUpgrade, Visuals.BTN_SECONDARY,
                game.session.playerStation.moduleAt(selectedSlot) != null);
        ui.drawButton(btnDeposit, Visuals.BTN_SECONDARY,
                game.session.playerStation.storage());
        ui.drawButton(btnRefine, Visuals.BTN_SECONDARY,
                game.session.playerStation.refinery());
        ui.drawButton(btnMarket, Visuals.BTN_SECONDARY,
                game.session.playerStation.market());
        ui.drawButton(btnRepair, Visuals.BTN_WARM,
                game.session.playerStation.repair());
        ui.drawButton(btnUndock, Visuals.BTN_PRIMARY, true);
        ui.drawButton(btnEnter, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("ESTACION PERSONAL", px + 22f * u, h - 34f * u,
                1.0f * u, Visuals.ACCENT);
        ui.textLeftFit("SERVICIOS " + servicesText()
                        + "   ALMACEN " + Math.round(
                        game.session.playerStation.stationStorage().totalUnits())
                        + "/" + Math.round(game.session.playerStation.storageCapacity())
                        + " UM",
                px + 22f * u, h - 60f * u, 0.70f * u, pw - 44f * u,
                Visuals.TEXT_DIM);
        ui.textLeft("MODULOS " + game.session.playerStation.moduleCount()
                        + "/" + PlayerStation.SLOTS,
                px + 22f * u, h - 84f * u, 0.68f * u, Visuals.TEXT_OFF);
        if (!message.isEmpty()) {
            ui.textLeftFit(message, px + 22f * u, h - 102f * u, 0.64f * u,
                    pw - 44f * u, Visuals.ACCENT);
        }
        drawSlots();
        drawFooter();
        batch.end();
    }

    private String servicesText() {
        StringBuilder text = new StringBuilder();
        if (game.session.playerStation.storage()) {
            text.append("ALM ");
        }
        if (game.session.playerStation.repair()) {
            text.append("TAL ");
        }
        if (game.session.playerStation.refinery()) {
            text.append("REF ");
        }
        if (game.session.playerStation.market()) {
            text.append("MER ");
        }
        return text.length() == 0 ? "NINGUNO" : text.toString().trim();
    }

    private void drawSlots() {
        float u = game.u;
        for (int i = 0; i < slotButtons.size; i++) {
            Button b = slotButtons.get(i);
            StationModule module = game.session.playerStation.moduleAt(i);
            float x = b.bounds.x + 12f * u;
            float titleY = b.bounds.y + b.bounds.height - 18f * u;
            float descY = b.bounds.y + 14f * u;
            if (module == null) {
                ui.textLeftFit("RANURA " + (i + 1) + "  VACIA", x, titleY,
                        0.82f * u, b.bounds.width * 0.7f, Visuals.TEXT_OFF);
                ui.textLeftFit("Elige modulo abajo y CONSTRUIR", x, descY,
                        0.60f * u, b.bounds.width, Visuals.TEXT_DIM);
            } else {
                ui.textLeftFit(module.def.displayName + "  Nv." + module.level(),
                        x, titleY, 0.82f * u, b.bounds.width * 0.6f,
                        Visuals.TEXT);
                ui.textLeftFit(module.def.description, x, descY, 0.60f * u,
                        b.bounds.width * 0.7f, Visuals.TEXT_DIM);
                ui.textLeft("C" + module.def.creditCost * (module.level() + 1),
                        b.bounds.x + b.bounds.width - 70f * u, titleY,
                        0.68f * u, Visuals.GOLD);
            }
        }
    }

    private void drawFooter() {
        float u = game.u;
        for (int i = 0; i < catalogButtons.size; i++) {
            ui.textCentered(catalog[i].displayName, catalogButtons.get(i)
                            .bounds.x + catalogButtons.get(i).bounds.width * 0.5f,
                    py + 238f * u, 0.62f * u,
                    selectedCatalog == i ? Visuals.GOLD : Visuals.TEXT_DIM);
        }
        ui.drawButtonText(btnBuild, true);
        ui.drawButtonText(btnUpgrade, true);
        ui.drawButtonText(btnDeposit, true);
        ui.drawButtonText(btnRepair, true);
        ui.drawButtonText(btnRefine, true);
        ui.drawButtonText(btnMarket, true);
        ui.drawButtonText(btnUndock, true);
        ui.drawButtonText(btnEnter, true);
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
