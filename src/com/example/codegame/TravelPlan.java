package com.example.codegame;

/**
 * Plan de viaje con dos modos (UPD-006-T17/T42): convencional (sin warp, más
 * lento y con consumo según masa y eficiencia) y warp (rápido, exige motor y
 * energía). La UI y el servicio usan el mismo plan para no duplicar costes.
 */
final class TravelPlan {

    enum Mode {
        WARP,
        CONVENTIONAL
    }

    final Mode mode;
    final String fromSectorId;
    final String toSectorId;
    final String error;
    final float distance;
    final int fuelCost;
    final int energyCost;
    /** Tiempo estimado de tránsito convencional, en horas. */
    final float hours;
    final int requiredWarpLevel;
    final int shipWarpLevel;

    private TravelPlan(Mode mode, String from, String to, String error,
                       float distance, int fuel, int energy, float hours,
                       int requiredWarp, int shipWarp) {
        this.mode = mode;
        this.fromSectorId = from;
        this.toSectorId = to;
        this.error = error == null ? "" : error;
        this.distance = distance;
        this.fuelCost = Math.max(0, fuel);
        this.energyCost = Math.max(0, energy);
        this.hours = Math.max(0f, hours);
        this.requiredWarpLevel = requiredWarp;
        this.shipWarpLevel = shipWarp;
    }

    boolean ok() {
        return error.isEmpty();
    }

    /** Presupuesto de viaje para un destino y un modo. */
    static TravelPlan quote(GameSession session, String targetId, Mode mode) {
        return quote(session, targetId, mode, null);
    }

    static TravelPlan quote(GameSession session, String targetId, Mode mode,
                            JumpGate gate) {
        Mode m = mode == null ? Mode.WARP : mode;
        if (session == null || session.currentSector == null) {
            return fail(m, "Sin sesion");
        }
        SectorDef target = session.universe.sector(targetId);
        if (target == null) {
            return fail(m, "Destino desconocido");
        }
        if (target.id.equals(session.currentSector.id)) {
            return fail(m, "Ya estas en ese sector");
        }
        if (!session.universe.isDiscovered(target.id)) {
            return fail(m, "Sector no descubierto");
        }
        float distance = session.currentSector.distanceTo(target);
        int warp = session.upgrades().level(Upgrades.MOTOR);
        Ship ship = session.ship();
        if (ship.docked) {
            return fail(m, "Despega antes de viajar");
        }
        if (m == Mode.WARP) {
            if (warp < TravelService.MIN_WARP_LEVEL) {
                return new TravelPlan(m, session.currentSector.id, target.id,
                        "Necesitas motor warp", distance, 0, 0, 0f,
                        TravelService.MIN_WARP_LEVEL, warp);
            }
            float divisor = 1f + 0.15f * warp;
            float fuel = Math.max(TravelService.MIN_FUEL,
                    (float) Math.ceil(distance * TravelService.FUEL_PER_UNIT / divisor));
            float energy = Math.max(TravelService.MIN_ENERGY,
                    (float) Math.ceil(distance * TravelService.ENERGY_PER_UNIT / divisor));
            if (gate != null && gate.connects(session.currentSector.id, target.id)) {
                fuel *= gate.costFactor;
                energy *= gate.costFactor;
            }
            int fuelCost = Math.max(1, Math.round(fuel));
            int energyCost = Math.max(1, Math.round(energy));
            String error = ship.fuel < fuelCost ? "Sin combustible"
                    : ship.capacitor.energy < energyCost ? "Sin energia" : "";
            return new TravelPlan(m, session.currentSector.id, target.id, error,
                    distance, fuelCost, energyCost, 0.5f,
                    TravelService.MIN_WARP_LEVEL, warp);
        }
        // Convencional: sin warp, consumo por distancia, masa y eficiencia.
        float mass = MassModel.fuelFactor(ship.cargo);
        float efficiency = 1f + 0.08f * session.upgrades().level(Upgrades.GASOLINA);
        float fuel = Math.max(TravelService.MIN_FUEL,
                (float) Math.ceil(distance * 0.62f * mass / efficiency));
        int fuelCost = Math.max(1, Math.round(fuel));
        String error = ship.fuel < fuelCost ? "Sin combustible" : "";
        float hours = Math.max(0.5f, distance * 0.75f);
        return new TravelPlan(m, session.currentSector.id, target.id, error,
                distance, fuelCost, 0, hours, 0, warp);
    }

    private static TravelPlan fail(Mode mode, String error) {
        return new TravelPlan(mode, "", "", error, 0f, 0, 0, 0f, 0, 0);
    }

    String modeLabel() {
        return mode == Mode.WARP ? "WARP" : "CONVENCIONAL";
    }

    String timeLabel() {
        if (mode == Mode.WARP) {
            return "INSTANTANEO";
        }
        return String.format(java.util.Locale.US, "%.1f h", hours);
    }
}
