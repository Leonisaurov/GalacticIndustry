package com.example.codegame;

/**
 * Transacciones del jugador con el mercado de una estación. Valida cantidad,
 * créditos, capacidad y stock; actualiza stock y precios tras cada operación.
 * El registro de la transacción queda en manos del llamador mediante
 * {@link Listener}.
 */
final class TradeService {

    /** Datos de una transacción completada. */
    static final class Trade {
        final boolean bought;
        final Commodity commodity;
        final int amount;
        final float unitPrice;
        final int total;

        Trade(boolean bought, Commodity commodity, int amount,
              float unitPrice, int total) {
            this.bought = bought;
            this.commodity = commodity;
            this.amount = amount;
            this.unitPrice = unitPrice;
            this.total = total;
        }

        String toJson() {
            return String.format(java.util.Locale.US,
                    "{\"event\":\"trade\",\"side\":\"%s\",\"commodity\":\"%s\","
                            + "\"amount\":%d,\"unitPrice\":%.2f,\"total\":%d}",
                    bought ? "buy" : "sell", commodity.name(), amount,
                    unitPrice, total);
        }
    }

    /** Resultado de una operación: éxito con datos o error legible. */
    static final class Result {
        final boolean ok;
        final Trade trade;
        final String error;

        private Result(boolean ok, Trade trade, String error) {
            this.ok = ok;
            this.trade = trade;
            this.error = error;
        }

        static Result ok(Trade trade) {
            return new Result(true, trade, null);
        }

        static Result error(String message) {
            return new Result(false, null, message);
        }
    }

    interface Listener {
        void onTrade(Trade trade);
    }

    private TradeService() {
    }

    static Result buy(GameWorld world, Upgrades upgrades, Commodity commodity,
                      int amount, Listener listener) {
        if (world == null) {
            return Result.error("Cantidad invalida");
        }
        return buy(world.station.market, upgrades, world.ship.cargo(),
                commodity, amount, listener);
    }

    static Result buy(Market market, Upgrades upgrades, CargoHold cargo,
                      Commodity commodity, int amount, Listener listener) {
        if (market == null || upgrades == null || cargo == null
                || commodity == null || amount <= 0) {
            return Result.error("Cantidad invalida");
        }
        if (amount > market.stock(commodity)) {
            return Result.error("Sin stock");
        }
        if (!cargo.canAdd(commodity, amount)) {
            return Result.error("Sin espacio en bodega");
        }
        float unit = market.buyPrice(commodity);
        int total = Math.max(1, Math.round(unit * amount));
        if (!upgrades.spendCoins(total)) {
            return Result.error("Sin creditos");
        }
        cargo.addExact(commodity, amount);
        market.addStock(commodity, -amount);
        Trade trade = new Trade(true, commodity, amount, unit, total);
        if (listener != null) {
            listener.onTrade(trade);
        }
        return Result.ok(trade);
    }

    static Result sell(GameWorld world, Upgrades upgrades, Commodity commodity,
                       int amount, Listener listener) {
        if (world == null) {
            return Result.error("Cantidad invalida");
        }
        return sell(world.station.market, upgrades, world.ship.cargo(),
                commodity, amount, listener);
    }

    static Result sell(Market market, Upgrades upgrades, CargoHold cargo,
                       Commodity commodity, int amount, Listener listener) {
        if (market == null || upgrades == null || cargo == null
                || commodity == null || amount <= 0) {
            return Result.error("Cantidad invalida");
        }
        if (cargo.amount(commodity) < amount) {
            return Result.error("Sin carga suficiente");
        }
        float unit = market.sellPrice(commodity);
        int total = Math.max(1, Math.round(unit * amount));
        cargo.removeExact(commodity, amount);
        market.addStock(commodity, amount);
        upgrades.addCoins(total);
        Trade trade = new Trade(false, commodity, amount, unit, total);
        if (listener != null) {
            listener.onTrade(trade);
        }
        return Result.ok(trade);
    }
}
