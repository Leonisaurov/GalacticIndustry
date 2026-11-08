package com.example.codegame;

import com.badlogic.gdx.utils.Array;

/**
 * Lista de pilotos autónomos del sector. Se crean ocho mineros por defecto y el
 * máximo es 24. Por ahora solo almacena datos; no se renderizan.
 */
final class AgentRoster {

    static final float CARGO_CAPACITY = 60f;
    static final float MAX_FUEL = 120f;
    static final float SPEED = 180f;
    static final float MINING_RATE = 4f;

    final Array<PilotAgent> agents = new Array<>();

    AgentRoster() {
        this(GameData.INITIAL_AGENTS);
    }

    AgentRoster(int count) {
        createInitial(count);
    }

    /** Crea (o recrea) una plantilla de mineros determinista. */
    void createInitial(int count) {
        clear();
        int n = Math.max(0, Math.min(count, GameData.MAX_AGENTS));
        for (int i = 0; i < n; i++) {
            String number = String.format(java.util.Locale.US, "%02d", i + 1);
            agents.add(new PilotAgent("M-" + number, "MINERO " + number,
                    PilotAgent.Role.MINER, CARGO_CAPACITY, MAX_FUEL, SPEED,
                    MINING_RATE));
        }
    }

    void resetForNewRun() {
        for (int i = 0; i < agents.size; i++) {
            agents.get(i).resetForNewRun();
        }
    }

    int size() {
        return agents.size;
    }

    int maxSize() {
        return GameData.MAX_AGENTS;
    }

    PilotAgent get(int index) {
        return index >= 0 && index < agents.size ? agents.get(index) : null;
    }

    boolean add(PilotAgent agent) {
        if (agent == null || agents.size >= GameData.MAX_AGENTS) {
            return false;
        }
        agents.add(agent);
        return true;
    }

    boolean remove(PilotAgent agent) {
        return agents.removeValue(agent, true);
    }

    PilotAgent byId(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < agents.size; i++) {
            if (id.equals(agents.get(i).id)) {
                return agents.get(i);
            }
        }
        return null;
    }

    int dockedCount() {
        int count = 0;
        for (int i = 0; i < agents.size; i++) {
            if (agents.get(i).docked()) {
                count++;
            }
        }
        return count;
    }

    void clear() {
        agents.clear();
    }
}
