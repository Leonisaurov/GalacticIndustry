package com.example.codegame;

/**
 * Simulación económica ligera con tick y LOD. Los agentes avanzan cada 2 s y los
 * mercados cada 10 s; nunca se ejecuta trabajo por frame. El reloj acumula
 * tiempo y ejecuta solo los ticks completos.
 */
final class EconomySim {

    /** Tick de mercados (producción/consumo). */
    static final float MARKET_TICK_SECONDS = 10f;
    /** Tick de agentes cerca del jugador. */
    static final float AGENT_TICK_SECONDS = SimClock.NEAR_SECONDS;
    /** Fracción del hueco con el objetivo que se corrige por tick. */
    static final float REVERSION = 0.02f;
    /** Tope de stock: nunca supera 3 veces el objetivo. */
    static final int MAX_STOCK_FACTOR = 3;
    /** Tope de simulación offline: 12 h. */
    static final float MAX_OFFLINE_SECONDS = 12f * 3600f;

    private final SimClock clock = new SimClock();
    private float marketAccumulator;

    void reset() {
        clock.reset();
        marketAccumulator = 0f;
    }

    /** Acumula tiempo y ejecuta agentes cada 2 s y mercados cada 10 s. */
    void update(float dt, GameWorld world) {
        if (world == null || dt <= 0f) {
            return;
        }
        int agentTicks = clock.advanceNear(dt);
        for (int i = 0; i < agentTicks && i < 1000; i++) {
            tickAgents(world, AGENT_TICK_SECONDS);
        }
        marketAccumulator += dt;
        int marketTicks = (int) (marketAccumulator / MARKET_TICK_SECONDS);
        marketTicks = Math.min(marketTicks, 1000);
        if (marketTicks > 0) {
            marketAccumulator -= marketTicks * MARKET_TICK_SECONDS;
            for (int i = 0; i < marketTicks; i++) {
                tick(world.station.market);
            }
        }
    }

    /**
     * Simulación offline: usa ticks de 60 s y se limita a 12 h. No crea hilos;
     * se llama al cargar o al volver a la partida.
     */
    void updateOffline(float seconds, GameWorld world) {
        if (world == null || seconds <= 0f) {
            return;
        }
        int ticks = clock.advanceOffline(Math.min(seconds, MAX_OFFLINE_SECONDS));
        for (int i = 0; i < ticks; i++) {
            tickAgents(world, SimClock.OFFLINE_SECONDS);
            tick(world.station.market);
        }
    }

    private static void tickAgents(GameWorld world, float dt) {
        for (int i = 0; i < world.agents.size(); i++) {
            AgentBrain.update(world.agents.get(i), world.station.market, dt);
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
