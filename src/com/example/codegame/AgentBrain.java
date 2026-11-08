package com.example.codegame;

/**
 * Máquina de estados de un piloto autónomo. El viaje es abstracto (tiempo), sin
 * pathfinding 2D. Decide por utilidad qué mineral minar; si no hay beneficio,
 * espera. Cada agente opera sobre el mercado de la estación.
 */
final class AgentBrain {

    static final float TRAVEL_SECONDS = 2f;
    static final float TRIP_FUEL = 20f;
    static final float FUEL_PRICE = 1f;
    static final float OPPORTUNITY_PER_SEC = 0.4f;
    static final float RISK = 5f;
    static final float IDLE_SECONDS = 6f;
    /** Un minero básico trabaja minerales comunes; los raros llegan con mejoras. */
    private static final Commodity[] COMMON = {
            Commodity.HIELO, Commodity.SILICATO, Commodity.HIERRO, Commodity.COBRE
    };

    private AgentBrain() {
    }

    static void update(PilotAgent agent, Market market, float dt) {
        if (agent == null || market == null || dt <= 0f) {
            return;
        }
        switch (agent.state) {
            case DOCKED:
                decide(agent, market);
                break;
            case TRAVEL:
                agent.timer -= dt;
                if (agent.timer <= 0f) {
                    agent.state = PilotAgent.State.MINE;
                    agent.timer = miningSeconds(agent);
                    agent.objective = "ROCA-" + agent.commodity.name();
                }
                break;
            case MINE:
                agent.timer -= dt;
                if (agent.timer <= 0f) {
                    int units = tripUnits(agent);
                    int fit = (int) Math.floor((agent.cargo.freeMass() + 0.0001f)
                            / agent.commodity.massPerUnit);
                    units = Math.min(units, Math.max(0, fit));
                    if (units > 0) {
                        agent.cargo.addExact(agent.commodity, units);
                    }
                    agent.state = PilotAgent.State.RETURN;
                    agent.timer = TRAVEL_SECONDS;
                    agent.objective = "NODRIZA";
                }
                break;
            case RETURN:
                agent.timer -= dt;
                if (agent.timer <= 0f) {
                    agent.state = PilotAgent.State.SELL;
                    agent.timer = 0f;
                }
                break;
            case SELL:
                sell(agent, market);
                agent.state = PilotAgent.State.REFUEL;
                agent.timer = 0f;
                break;
            case REFUEL:
                refuel(agent);
                agent.state = PilotAgent.State.DOCKED;
                agent.timer = 0f;
                break;
            case IDLE:
                agent.timer -= dt;
                if (agent.timer <= 0f) {
                    agent.state = PilotAgent.State.DOCKED;
                    agent.timer = 0f;
                }
                break;
            default:
                break;
        }
    }

    private static void decide(PilotAgent agent, Market market) {
        if (agent.fuel < TRIP_FUEL) {
            refuel(agent);
        }
        Commodity best = null;
        float bestUtility = 0f;
        for (Commodity commodity : COMMON) {
            float utility = utility(agent, market, commodity);
            if (utility > bestUtility) {
                bestUtility = utility;
                best = commodity;
            }
        }
        if (best == null) {
            agent.state = PilotAgent.State.IDLE;
            agent.timer = IDLE_SECONDS;
            agent.objective = "ESPERAR";
            return;
        }
        agent.commodity = best;
        agent.fuel = Math.max(0f, agent.fuel - TRIP_FUEL);
        agent.state = PilotAgent.State.TRAVEL;
        agent.timer = TRAVEL_SECONDS;
        agent.objective = "CAMPO-" + best.name();
    }

    static float utility(PilotAgent agent, Market market, Commodity commodity) {
        if (agent == null || market == null || commodity == null) {
            return 0f;
        }
        int units = Math.max(0, (int) Math.floor(agent.cargoCapacity
                / commodity.massPerUnit));
        float gross = market.sellPrice(commodity) * units;
        float fuelCost = TRIP_FUEL * FUEL_PRICE;
        float timeCost = (units / agent.miningRate) * OPPORTUNITY_PER_SEC;
        return gross - fuelCost - timeCost - RISK;
    }

    private static int tripUnits(PilotAgent agent) {
        return Math.max(1, (int) Math.floor(agent.cargoCapacity
                / agent.commodity.massPerUnit));
    }

    private static float miningSeconds(PilotAgent agent) {
        return Math.max(0.5f, tripUnits(agent) / agent.miningRate);
    }

    private static void sell(PilotAgent agent, Market market) {
        int units = agent.cargo.amount(agent.commodity);
        if (units <= 0) {
            return;
        }
        int total = Math.max(1, Math.round(market.sellPrice(agent.commodity) * units));
        agent.cargo.removeExact(agent.commodity, units);
        market.addStock(agent.commodity, units);
        agent.credits += total;
    }

    private static void refuel(PilotAgent agent) {
        float missing = agent.maxFuel - agent.fuel;
        if (missing <= 0.01f) {
            return;
        }
        int affordable = (int) Math.floor(agent.credits / FUEL_PRICE);
        int bought = Math.min((int) Math.ceil(missing), Math.max(0, affordable));
        if (bought <= 0) {
            return;
        }
        agent.credits -= Math.round(bought * FUEL_PRICE);
        agent.fuel = Math.min(agent.maxFuel, agent.fuel + bought);
    }
}
