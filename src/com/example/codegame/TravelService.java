package com.example.codegame;

/**
 * Viaje warp libre entre sectores: no requiere puertas. El coste depende de la
 * distancia de mapa, del motor warp (equipamiento) y de una puerta opcional.
 * Al viajar se captura el sector de origen y se carga el destino desde el
 * streaming, conservando la nave.
 */
final class TravelService {

    static final float FUEL_PER_UNIT = 2f;
    static final float ENERGY_PER_UNIT = 4f;
    static final int MIN_WARP_LEVEL = 1;
    static final float MIN_FUEL = 2f;
    static final float MIN_ENERGY = 4f;

    private TravelService() {
    }

    /** Presupuesto de un salto. */
    static final class Quote {
        boolean ok;
        String error = "";
        String fromSectorId;
        String toSectorId;
        float distance;
        int fuelCost;
        int energyCost;
        int warpLevel;
        boolean gateUsed;

        boolean affordable() {
            return ok;
        }
    }

    static final class Result {
        final boolean ok;
        final String error;
        final Quote quote;

        private Result(boolean ok, String error, Quote quote) {
            this.ok = ok;
            this.error = error;
            this.quote = quote;
        }
    }

    static Quote quote(GameSession session, String targetId) {
        return quote(session, targetId, null);
    }

    static Quote quote(GameSession session, String targetId, JumpGate gate) {
        Quote quote = new Quote();
        if (session == null || session.currentSector == null) {
            quote.error = "Sin sesion";
            return quote;
        }
        SectorDef target = session.universe.sector(targetId);
        if (target == null) {
            quote.error = "Destino desconocido";
            return quote;
        }
        if (target.id.equals(session.currentSector.id)) {
            quote.error = "Ya estas en ese sector";
            return quote;
        }
        int warp = session.upgrades().level(Upgrades.MOTOR);
        quote.warpLevel = warp;
        quote.fromSectorId = session.currentSector.id;
        quote.toSectorId = target.id;
        quote.distance = session.currentSector.distanceTo(target);
        if (warp < MIN_WARP_LEVEL) {
            quote.error = "Necesitas motor warp";
            return quote;
        }
        float divisor = 1f + 0.15f * warp;
        float fuel = Math.max(MIN_FUEL,
                (float) Math.ceil(quote.distance * FUEL_PER_UNIT / divisor));
        float energy = Math.max(MIN_ENERGY,
                (float) Math.ceil(quote.distance * ENERGY_PER_UNIT / divisor));
        if (gate != null && gate.connects(quote.fromSectorId, quote.toSectorId)) {
            fuel *= gate.costFactor;
            energy *= gate.costFactor;
            quote.gateUsed = true;
        }
        quote.fuelCost = Math.max(1, Math.round(fuel));
        quote.energyCost = Math.max(1, Math.round(energy));
        if (session.sector.ship.fuel < quote.fuelCost) {
            quote.error = "Sin combustible";
            return quote;
        }
        if (session.sector.ship.capacitor.energy < quote.energyCost) {
            quote.error = "Sin energia";
            return quote;
        }
        if (session.ship().docked) {
            quote.error = "Despega antes de viajar";
            return quote;
        }
        quote.ok = true;
        return quote;
    }

    static Result travel(GameSession session, String targetId) {
        return travel(session, targetId, null);
    }

    static Result travel(GameSession session, String targetId, JumpGate gate) {
        Quote quote = quote(session, targetId, gate);
        if (!quote.ok) {
            return new Result(false, quote.error, quote);
        }
        Ship ship = session.sector.ship;
        ship.fuel = Math.max(0f, ship.fuel - quote.fuelCost);
        ship.capacitor.drain(quote.energyCost);
        ship.thrusting = false;
        ship.laser.stop();
        ship.tractor.stop();
        session.sector.pilot.cancel();
        GameWorld next = session.switchSector(targetId);
        if (next == null) {
            return new Result(false, "No se pudo cargar el destino", quote);
        }
        return new Result(true, "", quote);
    }

    /** Siguiente sector del universo, para el mapa y las pruebas de viaje. */
    static String nextSectorId(GameSession session) {
        return neighbourSectorId(session, 1);
    }

    /** Sector anterior del universo, para volver. */
    static String previousSectorId(GameSession session) {
        return neighbourSectorId(session, -1);
    }

    private static String neighbourSectorId(GameSession session, int delta) {
        if (session == null || session.currentSector == null) {
            return null;
        }
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (GalaxyDef galaxy : session.universe.galaxies()) {
            for (SystemDef system : galaxy.systems) {
                for (SectorDef sector : system.sectors) {
                    ids.add(sector.id);
                }
            }
        }
        int index = ids.indexOf(session.currentSector.id);
        if (index < 0 || ids.size() < 2) {
            return null;
        }
        int next = (index + delta + ids.size()) % ids.size();
        return ids.get(next);
    }
}
