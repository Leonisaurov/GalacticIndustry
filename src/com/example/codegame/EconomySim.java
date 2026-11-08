package com.example.codegame;

/**
 * Simulación económica ligera: cada {@link #TICK_SECONDS} segundos actualiza el
 * stock de cada mercado con producción, consumo y una reversión suave al
 * objetivo. No se ejecuta por frame y el stock queda acotado para que los
 * precios no oscilen sin control.
 */
final class EconomySim {

    /** Intervalo de simulación; no se hace trabajo por frame. */
    static final float TICK_SECONDS = 10f;
    /** Fracción del hueco con el objetivo que se corrige por tick. */
    static final float REVERSION = 0.02f;
    /** Tope de stock: nunca supera 3 veces el objetivo. */
    static final int MAX_STOCK_FACTOR = 3;

    private float accumulator;

    void reset() {
        accumulator = 0f;
    }

    /** Acumula tiempo y ejecuta solo los ticks completos. */
    void update(float dt, GameWorld world) {
        if (world == null || dt <= 0f) {
            return;
        }
        accumulator += dt;
        int guard = 0;
        while (accumulator >= TICK_SECONDS && guard < 1000) {
            accumulator -= TICK_SECONDS;
            tick(world.station.market);
            guard++;
        }
    }

    static void tick(Market market) {
        if (market == null) {
            return;
        }
        for (Commodity commodity : Commodity.values()) {
            int stock = market.stock(commodity);
            int target = market.target(commodity);
            int delta = market.production(commodity) - market.consumption(commodity);
            int reversion = Math.round((target - stock) * REVERSION);
            int next = stock + delta + reversion;
            int max = target * MAX_STOCK_FACTOR;
            market.setStock(commodity, Math.max(0, Math.min(next, max)));
        }
    }
}
