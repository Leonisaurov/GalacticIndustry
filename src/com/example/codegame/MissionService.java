package com.example.codegame;

import java.util.ArrayList;
import java.util.List;

/**
 * Contratos de una estación. Genera ofertas deterministas a partir de su
 * mercado, controla plazos y valida la entrega. El jugador acepta un contrato y
 * lo completa al acoplarse con la carga necesaria.
 */
final class MissionService {

    static final int OFFERS = 2;
    static final float DEADLINE = 600f;
    static final float REWARD_FACTOR = 1.35f;

    private final List<Mission> missions = new ArrayList<>();

    void ensureOffers(Market market, String stationName) {
        if (!missions.isEmpty()) {
            return;
        }
        long seed = stationName == null ? 7L : stationName.hashCode();
        Commodity[] values = Commodity.values();
        for (int i = 0; i < OFFERS; i++) {
            int index = (int) Math.abs((seed + i * 31L) % values.length);
            Commodity commodity = values[index];
            int amount = 15 + (int) (Math.abs(seed + i * 17L) % 26);
            float price = market == null ? commodity.basePrice
                    : market.sellPrice(commodity);
            int reward = Math.max(20, Math.round(price * amount * REWARD_FACTOR));
            missions.add(new Mission("M-" + Math.abs(seed) + "-" + i,
                    commodity, amount, reward, DEADLINE));
        }
    }

    void update(float dt) {
        if (dt <= 0f) {
            return;
        }
        for (int i = 0; i < missions.size(); i++) {
            Mission mission = missions.get(i);
            if (!mission.active()) {
                continue;
            }
            mission.remaining -= dt;
            if (mission.remaining <= 0f) {
                mission.remaining = 0f;
                mission.status = Mission.Status.FAILED;
            }
        }
    }

    Mission byId(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < missions.size(); i++) {
            if (id.equals(missions.get(i).id)) {
                return missions.get(i);
            }
        }
        return null;
    }

    boolean accept(String id) {
        Mission mission = byId(id);
        if (mission == null || !mission.offered()) {
            return false;
        }
        mission.status = Mission.Status.ACTIVE;
        mission.remaining = mission.deadline;
        return true;
    }

    /**
     * Entrega la carga y paga la recompensa. Devuelve el total pagado o 0 si no
     * se puede completar.
     */
    int complete(String id, CargoHold cargo, Upgrades upgrades) {
        Mission mission = byId(id);
        if (mission == null || !mission.active() || cargo == null
                || upgrades == null
                || cargo.amount(mission.commodity) < mission.amount) {
            return 0;
        }
        cargo.removeExact(mission.commodity, mission.amount);
        upgrades.addCoins(mission.reward);
        mission.status = Mission.Status.COMPLETED;
        mission.remaining = 0f;
        return mission.reward;
    }

    List<Mission> missions() {
        return new ArrayList<>(missions);
    }

    int offeredCount() {
        int count = 0;
        for (int i = 0; i < missions.size(); i++) {
            if (missions.get(i).offered()) {
                count++;
            }
        }
        return count;
    }

    Mission activeMission() {
        for (int i = 0; i < missions.size(); i++) {
            if (missions.get(i).active()) {
                return missions.get(i);
            }
        }
        return null;
    }

    void replace(List<Mission> saved) {
        missions.clear();
        if (saved != null) {
            missions.addAll(saved);
        }
    }
}
