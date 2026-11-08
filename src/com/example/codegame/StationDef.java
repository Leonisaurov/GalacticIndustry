package com.example.codegame;

/**
 * Definición de una estación. Los valores son datos estáticos; la instancia
 * con posición y almacén vive en {@link Station}.
 */
final class StationDef {

    final String name;
    /** Radio visual de la estructura, en unidades del mundo. */
    final float radius;
    /** Distancia máxima para iniciar el acople (T-502). */
    final float dockRadius;
    /** Radio de la zona segura: no se generan asteroides dentro. */
    final float safeRadius;

    final boolean repair;
    final boolean refuel;
    final boolean oxygen;
    final boolean upgrades;
    final boolean market;
    final boolean storage;

    StationDef(String name, float radius, float dockRadius, float safeRadius,
               boolean repair, boolean refuel, boolean oxygen, boolean upgrades,
               boolean market, boolean storage) {
        this.name = name;
        this.radius = radius;
        this.dockRadius = dockRadius;
        this.safeRadius = safeRadius;
        this.repair = repair;
        this.refuel = refuel;
        this.oxygen = oxygen;
        this.upgrades = upgrades;
        this.market = market;
        this.storage = storage;
    }

    int serviceCount() {
        int n = 0;
        if (repair) n++;
        if (refuel) n++;
        if (oxygen) n++;
        if (upgrades) n++;
        if (market) n++;
        if (storage) n++;
        return n;
    }
}
