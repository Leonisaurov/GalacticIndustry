package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Concesionario de naves (UPD-006-T28/T29/T37/T38): presupuesto visible,
 * chatarra/usadas/nuevas, inspección, paquetes, compra personalizada con
 * combustible inicial y validación de configuraciones viables.
 */
final class ShipDealerScreen {

    private static final Color PANEL_C = new Color(0.030f, 0.042f, 0.058f, 0.95f);
    private static final Color CARD_C = new Color(0.060f, 0.080f, 0.100f, 0.94f);
    private static final Color CARD_SEL_C = new Color(0.12f, 0.115f, 0.085f, 0.96f);
    private static final Color EDGE_C = new Color(0.24f, 0.33f, 0.40f, 0.50f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;

    private final Array<Button> rows = new Array<>();
    private Button btnBuy, btnInspect, btnBack, btnFuelMinus, btnFuelPlus, btnRescue,
            btnModAdd, btnModRemove;
    private final Array<ShipModuleDef> extras = new Array<>();
    private int selected;
    private int fuelUnits = 120;
    private String message = "";
    private float px, py, pw, ph;

    ShipDealerScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
    }

    void enter() {
        selected = 0;
        fuelUnits = 120;
        extras.clear();
        message = "";
        layout();
        clampFuel();
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
        rows.clear();
        int count = market() == null ? 0 : market().size();
        float rowH = Math.min(52f * u, (h * 0.34f) / Math.max(1, count));
        float gap = 5f * u;
        float top = h * 0.49f;
        for (int i = 0; i < count; i++) {
            rows.add(new Button("", px + 20f * u, top - (i + 1) * rowH - i * gap,
                    pw - 40f * u, rowH));
        }
        float bw = Math.min(w * 0.30f, 132f * u);
        btnInspect = new Button("INSPECCIONAR", px + 20f * u, py + 106f * u,
                bw, 44f * u);
        btnBuy = new Button("COMPRAR", px + 20f * u + bw + 6f * u,
                py + 106f * u, bw, 44f * u);
        btnBack = new Button("VOLVER", px + 20f * u + (bw + 6f * u) * 2f,
                py + 106f * u, Math.min(bw, pw - 40f * u - (bw + 6f * u) * 2f),
                44f * u);
        btnFuelMinus = new Button("-", px + 20f * u, py + 52f * u, 44f * u, 40f * u);
        btnFuelPlus = new Button("+", px + 20f * u + 50f * u, py + 52f * u,
                44f * u, 40f * u);
        btnModAdd = new Button("MOD +", px + 20f * u + 104f * u, py + 52f * u,
                70f * u, 40f * u);
        btnModRemove = new Button("MOD -", px + 20f * u + 180f * u,
                py + 52f * u, 70f * u, 40f * u);
        btnRescue = new Button("NAVE DE SERVICIO", px + 20f * u, py + 6f * u,
                Math.min(190f * u, pw * 0.44f), 40f * u);
    }

    private ShipMarket market() {
        return game.session == null ? null : game.session.shipMarket;
    }

    private ShipOffer offer() {
        ShipMarket market = market();
        return market == null ? null : market.offer(selected);
    }

    private void clampFuel() {
        ShipOffer offer = offer();
        if (offer == null) {
            fuelUnits = 0;
            return;
        }
        int cap = (int) offer.spec.fuelCapacity;
        fuelUnits = MathUtils.clamp(fuelUnits, 60, cap);
    }

    boolean handle() {
        if (btnBuy == null) {
            layout();
        }
        if (game.tapped(btnBack)) {
            game.closeShipDealer();
            return true;
        }
        for (int i = 0; i < rows.size; i++) {
            if (game.tapped(rows.get(i))) {
                selected = i;
                clampFuel();
                message = "";
                return true;
            }
        }
        if (game.tapped(btnInspect)) {
            inspect();
            return true;
        }
        if (game.tapped(btnFuelMinus)) {
            fuelUnits = Math.max(60, fuelUnits - 10);
            return true;
        }
        if (game.tapped(btnFuelPlus)) {
            fuelUnits = Math.min((int) (offer() == null ? 60
                    : offer().spec.fuelCapacity), fuelUnits + 10);
            return true;
        }
        if (game.tapped(btnModAdd)) {
            addExtraModule();
            return true;
        }
        if (game.tapped(btnModRemove)) {
            if (extras.size > 0) {
                extras.removeIndex(extras.size - 1);
                message = "";
            }
            return true;
        }
        if (game.tapped(btnBuy)) {
            buy();
            return true;
        }
        if (needsRescue() && game.tapped(btnRescue)) {
            if (game.grantServiceShip()) {
                message = "NAVE DE SERVICIO ENTREGADA";
            }
            return true;
        }
        return false;
    }

    private void inspect() {
        ShipOffer offer = offer();
        if (offer == null) {
            return;
        }
        if (offer.inspected) {
            message = "YA INSPECCIONADA";
            return;
        }
        if (offer.inspect(game.upgrades)) {
            message = "INSPECCION COMPLETADA";
        } else {
            message = "SIN CREDITOS PARA INSPECCION";
        }
    }

    private void buy() {
        ShipOffer offer = offer();
        if (offer == null) {
            return;
        }
        if (game.session.hasShip) {
            message = "YA TIENES NAVE";
            return;
        }
        int cost = game.session.purchaseCost(offer, fuelUnits) + extraCost();
        if (game.upgrades.coins() < cost) {
            message = "SIN CREDITOS";
            return;
        }
        if (fuelUnits < 60) {
            message = "COMBUSTIBLE MINIMO 60 UM";
            return;
        }
        if (game.purchaseShip(offer, fuelUnits, extras)) {
            message = "NAVE COMPRADA";
            game.closeShipDealer();
        } else {
            message = "NO SE PUDO COMPRAR";
        }
    }

    /** Añade el siguiente módulo compatible que no venga ya en la nave. */
    private void addExtraModule() {
        ShipOffer offer = offer();
        if (offer == null || extras.size >= ShipLoadout.MODULE_SLOTS) {
            return;
        }
        for (ShipModuleDef def : ShipModuleDef.values()) {
            if (offer.spec.hasModule(def) || extras.contains(def, true)) {
                continue;
            }
            if (!ShipSlotDef.slotFor(offer.spec.shipClass,
                    extras.size).accepts(def)) {
                continue;
            }
            extras.add(def);
            message = "";
            return;
        }
        message = "SIN MODULO COMPATIBLE";
    }

    private int extraCost() {
        int cost = 0;
        for (int i = 0; i < extras.size; i++) {
            cost += extras.get(i).creditCost;
        }
        return cost;
    }

    int offerCount() {
        return market() == null ? 0 : market().size();
    }

    String selectedCategory() {
        return offer() == null ? "" : offer().spec.category.name();
    }

    int selectedPrice() {
        return offer() == null ? 0 : offer().spec.price;
    }

    boolean selectedInspected() {
        return offer() != null && offer().inspected;
    }

    int extraCount() {
        return extras.size;
    }

    private boolean needsRescue() {
        if (game.session == null || game.session.hasShip || market() == null
                || market().cheapest() == null) {
            return false;
        }
        int cheapest = market().cheapest().spec.price + 60;
        return game.upgrades.coins() < cheapest;
    }

    void draw(float w, float h) {
        if (btnBuy == null) {
            layout(w, h);
        }
        float u = game.u;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();
        beginShapes();
        ui.grid(w, h, 80f * u, 0.014f);
        ui.frame(px, py, pw, ph, 14f * u, PANEL_C, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.divider(px + 22f * u, h - 92f * u, pw - 44f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        drawRows();
        drawDetailPanel(h);
        ui.drawButton(btnInspect, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnBuy, Visuals.BTN_PRIMARY, !game.session.hasShip);
        ui.drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnFuelMinus, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnFuelPlus, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnModAdd, Visuals.BTN_SECONDARY, true);
        ui.drawButton(btnModRemove, Visuals.BTN_SECONDARY, true);
        if (needsRescue()) {
            ui.drawButton(btnRescue, Visuals.BTN_WARM, true);
        }
        shapes.end();

        batch.begin();
        ui.textLeft("CONCESIONARIO", px + 24f * u, h - 30f * u, 0.94f * u,
                Visuals.TEXT);
        ui.textRight(game.upgrades.coins() + " CR", px + pw - 24f * u,
                h - 30f * u, 0.84f * u, Visuals.GOLD);
        ui.textLeftFit("PRESUPUESTO DE ARRANQUE 1200 CR   GARANTIA DE MOVILIDAD",
                px + 24f * u, h - 58f * u, 0.56f * u, pw - 48f * u,
                Visuals.TEXT_OFF);
        drawRowTexts();
        drawDetail(w, h);
        ui.drawButtonText(btnInspect, true);
        ui.drawButtonText(btnBuy, true);
        ui.drawButtonText(btnBack, true);
        ui.drawButtonText(btnFuelMinus, true);
        ui.drawButtonText(btnFuelPlus, true);
        ui.drawButtonText(btnModAdd, true);
        ui.drawButtonText(btnModRemove, true);
        if (needsRescue()) {
            ui.drawButtonText(btnRescue, true);
        }
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, w * 0.5f, py + 172f * u, 0.62f * u,
                    pw - 40f * u, Visuals.WARN);
        }
        batch.end();
    }

    private void drawRows() {
        float u = game.u;
        for (int i = 0; i < rows.size; i++) {
            Button row = rows.get(i);
            ShipOffer offer = market().offer(i);
            boolean chosen = i == selected;
            ui.panel(row.bounds.x - 3f * u, row.bounds.y - 3f * u,
                    row.bounds.width + 6f * u, row.bounds.height + 6f * u,
                    6f * u, chosen ? CARD_SEL_C : CARD_C,
                    chosen ? Visuals.ACCENT : EDGE_C);
            Color dot = offer.spec.category == ShipSpec.Category.SCRAP
                    ? Visuals.WARN : offer.spec.category == ShipSpec.Category.NEW
                    ? Visuals.GOOD : Visuals.INFO;
            ui.led(row.bounds.x + 14f * u,
                    row.bounds.y + row.bounds.height * 0.5f, 3.4f * u, dot);
        }
    }

    private void drawRowTexts() {
        float u = game.u;
        for (int i = 0; i < rows.size; i++) {
            Button row = rows.get(i);
            ShipOffer offer = market().offer(i);
            float mid = row.bounds.y + row.bounds.height * 0.5f;
            ui.textLeftFit(offer.spec.label, row.bounds.x + 28f * u,
                    mid + 14f * u, 0.70f * u, row.bounds.width * 0.55f,
                    Visuals.TEXT);
            ui.textLeftFit(offer.spec.categoryLabel() + "  CASCO "
                            + offer.spec.conditionLabel() + "  "
                            + Math.round(offer.spec.fuelCapacity) + " UM  MODS "
                            + offer.spec.moduleCount(),
                    row.bounds.x + 28f * u, mid - 8f * u, 0.52f * u,
                    row.bounds.width * 0.66f, Visuals.TEXT_DIM);
            ui.textRight(offer.spec.price + " CR",
                    row.bounds.x + row.bounds.width - 12f * u, mid + 6f * u,
                    0.72f * u, Visuals.GOLD);
        }
    }

    /** Fondo del panel de detalle; se dibuja en la pasada de formas. */
    private void drawDetailPanel(float h) {
        float u = game.u;
        float top = h * 0.49f + 18f * u;
        float bottom = py + 196f * u;
        ui.panel(px + 20f * u, bottom, pw - 40f * u,
                Math.max(0f, top - bottom), 6f * u, CARD_C, EDGE_C);
    }

    private void drawDetail(float w, float h) {
        float u = game.u;
        ShipOffer offer = offer();
        float top = h * 0.49f + 18f * u;
        if (offer == null) {
            ui.textLeft("SIN OFERTAS", px + 32f * u, top - 24f * u,
                    0.66f * u, Visuals.TEXT_OFF);
            return;
        }
        ShipSpec spec = offer.spec;
        float y = top - 24f * u;
        ui.textLeftFit(spec.label, px + 32f * u, y, 0.72f * u,
                pw - 64f * u, Visuals.TEXT);
        ui.textRight(offer.inspected ? "INSPECCIONADA" : "SIN INSPECCION",
                px + pw - 32f * u, y, 0.56f * u,
                offer.inspected ? Visuals.GOOD : Visuals.WARN);
        y -= 24f * u;
        ui.textLeftFit(spec.description, px + 32f * u, y, 0.48f * u,
                pw - 64f * u, Visuals.TEXT_DIM);
        y -= 24f * u;
        ui.textLeftFit("CLASE " + spec.shipClass.displayName
                        + "   CASCO " + Math.round(spec.hull)
                        + "   DEPOSITO " + Math.round(spec.fuelCapacity) + " UM",
                px + 32f * u, y, 0.52f * u, pw - 64f * u, Visuals.TEXT);
        y -= 20f * u;
        ui.textLeftFit("EFICIENCIA x" + String.format(java.util.Locale.US, "%.2f",
                        spec.efficiency)
                        + "   MASA x" + String.format(java.util.Locale.US, "%.2f",
                        spec.massFactor)
                        + "   BODEGA " + Math.round(spec.cargoCapacity) + " UM",
                px + 32f * u, y, 0.52f * u, pw - 64f * u, Visuals.TEXT);
        y -= 20f * u;
        StringBuilder mods = new StringBuilder("MODULOS: ");
        if (spec.moduleCount() == 0) {
            mods.append("NINGUNO");
        } else {
            for (int i = 0; i < spec.modules.length; i++) {
                if (i > 0) {
                    mods.append(", ");
                }
                mods.append(spec.modules[i].toUpperCase(java.util.Locale.US));
            }
        }
        ui.textLeftFit(mods.toString(), px + 32f * u, y, 0.48f * u,
                pw - 64f * u, Visuals.TEXT_DIM);
        y -= 20f * u;
        StringBuilder extra = new StringBuilder();
        for (int i = 0; i < extras.size; i++) {
            if (i > 0) {
                extra.append(", ");
            }
            extra.append(extras.get(i).displayName);
        }
        ui.textLeftFit("EXTRA: " + (extras.size == 0 ? "NINGUNO" : extra.toString()),
                px + 32f * u, y, 0.48f * u, pw - 64f * u, Visuals.ACCENT);
        y -= 20f * u;
        ui.textLeftFit("COMBUSTIBLE INICIAL " + fuelUnits + " UM   COSTE "
                        + (game.session.purchaseCost(offer, fuelUnits) + extraCost())
                        + " CR", px + 32f * u, y, 0.54f * u, pw - 64f * u,
                Visuals.GOLD);
        if (needsRescue()) {
            ui.textLeftFit("SALDO INSUFICIENTE: LA NODRIZA OFRECE NAVE DE SERVICIO",
                    px + 32f * u, y - 20f * u, 0.48f * u, pw - 64f * u,
                    Visuals.WARN);
        }
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
