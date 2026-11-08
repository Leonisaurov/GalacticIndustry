package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Pantalla de mercado de la estación: lista de minerales con stock, precios de
 * compra/venta y un selector de cantidad. Las operaciones se confirman antes de
 * ejecutarse con {@link TradeService}.
 */
final class MarketScreen {

    private static final Color PANEL_C = new Color(0.020f, 0.035f, 0.080f, 0.92f);
    private static final Color CARD_C = new Color(0.07f, 0.11f, 0.21f, 0.92f);
    private static final Color CARD_SEL_C = new Color(0.10f, 0.22f, 0.38f, 0.95f);
    private static final Color CARD_EDGE_C = new Color(0.26f, 0.48f, 0.75f, 0.40f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final SpaceBackground space;

    private final Array<Button> rowButtons = new Array<>();
    private final Array<Button> qtyButtons = new Array<>();
    private Button btnTodo, btnNada, btnBuy, btnSell, btnBack;
    private int selected = Commodity.HIERRO.ordinal();
    private int quantity;
    private boolean pending;
    private boolean pendingBuy;
    private String message = "";

    private float px;
    private float py;
    private float pw;
    private float ph;
    private float listTop;
    private float rowH;
    private float rowGap;

    MarketScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes,
                 SpaceBackground space) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        this.space = space;
    }

    int quantity() {
        return quantity;
    }

    boolean pending() {
        return pending;
    }

    void enter() {
        quantity = 0;
        pending = false;
        pendingBuy = false;
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

        listTop = h - 118f * u;
        float bottom = py + 275f * u;
        rowGap = 6f * u;
        rowH = Math.min(200f * u, (listTop - bottom - 5 * rowGap) / 6f);
        rowH = Math.max(38f * u, rowH);

        rowButtons.clear();
        for (int i = 0; i < Commodity.values().length; i++) {
            float top = listTop - i * (rowH + rowGap);
            rowButtons.add(new Button("", px + 22f * u, top - rowH,
                    pw - 44f * u, rowH));
        }

        qtyButtons.clear();
        float qtyY = py + 196f * u;
        float qtyH = 42f * u;
        float qtyW = (pw - 44f * u) / 5f;
        for (int i = 0; i < 5; i++) {
            qtyButtons.add(new Button(i == 2 ? "0" : (i < 2 ? "-" : "+"),
                    px + 22f * u + i * qtyW, qtyY, qtyW - 4f * u, qtyH));
        }
        float halfW = (pw - 44f * u - 8f * u) * 0.5f;
        btnTodo = new Button("TODO", px + 22f * u, py + 146f * u, halfW, 40f * u);
        btnNada = new Button("NADA", px + 22f * u + halfW + 8f * u,
                py + 146f * u, halfW, 40f * u);
        btnBuy = new Button("COMPRAR", px + 22f * u, py + 94f * u, halfW, 44f * u);
        btnSell = new Button("VENDER", px + 22f * u + halfW + 8f * u,
                py + 94f * u, halfW, 44f * u);
        btnBack = new Button("VOLVER", px + pw - 110f * u, h - 60f * u,
                92f * u, 44f * u);
    }

    private Commodity selectedCommodity() {
        return Commodity.values()[selected];
    }

    boolean handle() {
        if (btnBack == null) {
            layout();
        }
        if (game.tapped(btnBack)) {
            game.closeMarket();
            return true;
        }
        if (pending) {
            if (pendingBuy && game.tapped(btnBuy)) {
                confirm();
                return true;
            }
            if (!pendingBuy && game.tapped(btnSell)) {
                confirm();
                return true;
            }
            if (game.tapped(pendingBuy ? btnSell : btnBuy)) {
                pending = false;
                message = "Cancelado";
                return true;
            }
            return false;
        }
        for (int i = 0; i < rowButtons.size; i++) {
            if (game.tapped(rowButtons.get(i))) {
                selected = i;
                setQuantity(0);
                message = "";
                return true;
            }
        }
        for (int i = 0; i < qtyButtons.size; i++) {
            if (!game.tapped(qtyButtons.get(i))) {
                continue;
            }
            int delta = i == 0 ? -10 : i == 1 ? -1 : i == 2 ? 0
                    : i == 3 ? 1 : 10;
            if (delta == 0) {
                setQuantity(0);
            } else {
                setQuantity(quantity + delta);
            }
            return true;
        }
        if (game.tapped(btnTodo)) {
            setQuantity(maxSell());
            return true;
        }
        if (game.tapped(btnNada)) {
            setQuantity(0);
            return true;
        }
        if (game.tapped(btnBuy)) {
            arm(true);
            return true;
        }
        if (game.tapped(btnSell)) {
            arm(false);
            return true;
        }
        return false;
    }

    private void arm(boolean buy) {
        if (quantity <= 0) {
            message = "Elige cantidad";
            return;
        }
        if (buy && quantity > maxBuy()) {
            message = "No hay saldo, stock o espacio";
            return;
        }
        if (!buy && quantity > maxSell()) {
            message = "No hay carga suficiente";
            return;
        }
        pending = true;
        pendingBuy = buy;
        message = "";
    }

    private void confirm() {
        Market market = game.activeMarket();
        int qty = quantity;
        Commodity commodity = selectedCommodity();
        TradeService.Listener listener = game::logTrade;
        TradeService.Result result = pendingBuy
                ? TradeService.buy(market, game.upgrades, game.world.ship.cargo(),
                commodity, qty, listener)
                : TradeService.sell(market, game.upgrades, game.world.ship.cargo(),
                commodity, qty, listener);
        if (result.ok) {
            message = pendingBuy ? "Comprados " + qty : "Vendidos " + qty;
            setQuantity(0);
            game.saveRun();
        } else {
            message = result.error;
        }
        pending = false;
    }

    private void setQuantity(int value) {
        int max = Math.max(maxBuy(), maxSell());
        quantity = Math.max(0, Math.min(value, max));
    }

    private int maxSell() {
        return game.world.ship.cargo().amount(selectedCommodity());
    }

    private int maxBuy() {
        Market market = game.activeMarket();
        Commodity commodity = selectedCommodity();
        float price = market.buyPrice(commodity);
        int affordable = price <= 0f ? 0
                : (int) Math.floor(game.upgrades.coins() / price);
        int fit = (int) Math.floor((game.world.ship.cargo().freeMass() + 0.0001f)
                / commodity.massPerUnit);
        return Math.max(0, Math.min(market.stock(commodity),
                Math.min(affordable, fit)));
    }

    private float pendingTotal() {
        Market market = game.activeMarket();
        float price = pendingBuy ? market.buyPrice(selectedCommodity())
                : market.sellPrice(selectedCommodity());
        return Math.max(1f, Math.round(price * quantity));
    }

    void draw(float w, float h) {
        if (btnBack == null) {
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
        btnBuy.label = pending ? (pendingBuy ? "CONFIRMAR" : "CANCELAR") : "COMPRAR";
        btnSell.label = pending ? (!pendingBuy ? "CONFIRMAR" : "CANCELAR") : "VENDER";
        boolean idle = !pending;
        for (int i = 0; i < rowButtons.size; i++) {
            ui.panel(rowButtons.get(i).bounds.x - 5f * u, rowButtons.get(i).bounds.y - 5f * u,
                    rowButtons.get(i).bounds.width + 10f * u,
                    rowButtons.get(i).bounds.height + 10f * u, 12f * u,
                    i == selected ? CARD_SEL_C : CARD_C,
                    i == selected ? Visuals.ACCENT : CARD_EDGE_C);
        }
        for (int i = 0; i < qtyButtons.size; i++) {
            ui.drawButton(qtyButtons.get(i), Visuals.BTN_SECONDARY,
                    idle && qtyEnabled(i));
        }
        ui.drawButton(btnTodo, Visuals.BTN_SECONDARY, idle && maxSell() > 0);
        ui.drawButton(btnNada, Visuals.BTN_SECONDARY, idle && quantity > 0);
        ui.drawButton(btnBuy,
                pending ? (pendingBuy ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY)
                        : Visuals.BTN_PRIMARY,
                pending || (quantity > 0 && quantity <= maxBuy()));
        ui.drawButton(btnSell,
                pending ? (!pendingBuy ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY)
                        : Visuals.BTN_WARM,
                pending || (quantity > 0 && quantity <= maxSell()));
        ui.drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("MERCADO " + game.marketName(),
                px + 24f * u, h - 36f * u, 1.0f * u, Visuals.ACCENT);
        ui.textLeft("CREDITOS " + game.upgrades.coins()
                        + "   BODEGA " + Math.round(game.world.ship.cargo().usedMass())
                        + "/" + Math.round(game.world.ship.cargo().capacity()) + " UM",
                px + 24f * u, h - 62f * u, 0.78f * u, Visuals.TEXT_DIM);
        drawRows();
        drawControls();
        ui.drawButtonText(btnBack, true);
        batch.end();
    }

    private boolean qtyEnabled(int i) {
        if (i == 0) return quantity >= 10;
        if (i == 1) return quantity >= 1;
        if (i == 2) return quantity > 0;
        return quantity < Math.max(maxBuy(), maxSell());
    }

    private void drawRows() {
        float u = game.u;
        Market market = game.activeMarket();
        for (int i = 0; i < rowButtons.size; i++) {
            Commodity c = Commodity.values()[i];
            Button b = rowButtons.get(i);
            float x = b.bounds.x + 14f * u;
            float mid = b.bounds.y + b.bounds.height * 0.5f;
            float titleY = mid + 12f * u;
            float descY = mid - 12f * u;
            ui.textLeftFit(c.displayName + "  " + market.stock(c) + "/" + market.target(c),
                    x, titleY, 0.86f * u, b.bounds.width * 0.52f, Visuals.TEXT);
            ui.textLeftFit("compra " + String.format(java.util.Locale.US, "%.2f", market.buyPrice(c))
                            + "  venta " + String.format(java.util.Locale.US, "%.2f", market.sellPrice(c)),
                    x, descY, 0.66f * u, b.bounds.width * 0.58f, Visuals.TEXT_DIM);
            int have = game.world.ship.cargo().amount(c);
            ui.textLeft("T " + have, b.bounds.x + b.bounds.width - 52f * u,
                    titleY, 0.78f * u, have > 0 ? Visuals.GOOD : Visuals.TEXT_OFF);
        }
    }

    private void drawControls() {
        float u = game.u;
        Market market = game.activeMarket();
        Commodity c = selectedCommodity();
        ui.textLeftFit("SELECCION: " + c.displayName + "   CANTIDAD " + quantity
                        + "   MAX " + Math.max(maxSell(), maxBuy()),
                px + 24f * u, py + 246f * u, 0.72f * u, pw - 48f * u, Visuals.TEXT);
        ui.textCentered(qtyText(0), qtyButtons.get(0).bounds.x + qtyButtons.get(0).bounds.width * 0.5f,
                py + 208f * u, 0.82f * u, Visuals.TEXT);
        ui.textCentered(qtyText(1), qtyButtons.get(1).bounds.x + qtyButtons.get(1).bounds.width * 0.5f,
                py + 208f * u, 0.82f * u, Visuals.TEXT);
        ui.textCentered(String.valueOf(quantity),
                qtyButtons.get(2).bounds.x + qtyButtons.get(2).bounds.width * 0.5f,
                py + 208f * u, 0.95f * u, Visuals.GOLD);
        ui.textCentered(qtyText(3), qtyButtons.get(3).bounds.x + qtyButtons.get(3).bounds.width * 0.5f,
                py + 208f * u, 0.82f * u, Visuals.TEXT);
        ui.textCentered(qtyText(4), qtyButtons.get(4).bounds.x + qtyButtons.get(4).bounds.width * 0.5f,
                py + 208f * u, 0.82f * u, Visuals.TEXT);
        ui.drawButtonText(btnTodo, maxSell() > 0);
        ui.drawButtonText(btnNada, quantity > 0);
        if (pending) {
            ui.drawButtonText(btnBuy, true);
            ui.drawButtonText(btnSell, true);
            ui.textCentered((pendingBuy ? "CONFIRMAR COMPRA " : "CONFIRMAR VENTA ")
                            + quantity + " " + c.displayName + " por "
                            + (int) pendingTotal() + " cr",
                    px + pw * 0.5f, py + 184f * u, 0.76f * u, Visuals.WARN);
            ui.textCentered("Pulsa CONFIRMAR o CANCELAR", 
                    px + pw * 0.5f, py + 72f * u, 0.62f * u, Visuals.TEXT_DIM);
        } else {
            ui.drawButtonText(btnBuy, quantity > 0 && quantity <= maxBuy());
            ui.drawButtonText(btnSell, quantity > 0 && quantity <= maxSell());
            if (!message.isEmpty()) {
                ui.textCentered(message, px + pw * 0.5f, py + 72f * u,
                        0.68f * u, Visuals.TEXT_DIM);
            } else {
                ui.textLeft("Compra " + String.format(java.util.Locale.US, "%.2f", market.buyPrice(c))
                                + " cr/u   Venta " + String.format(java.util.Locale.US, "%.2f", market.sellPrice(c))
                                + " cr/u",
                        px + 24f * u, py + 72f * u, 0.70f * u, Visuals.TEXT_DIM);
            }
        }
    }

    private String qtyText(int i) {
        return i == 0 ? "-10" : i == 1 ? "-1" : i == 3 ? "+1" : "+10";
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}
