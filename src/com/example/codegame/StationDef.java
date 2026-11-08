package com.example.codegame;

/**
 * Definición de una estación. Los valores son datos estáticos; la instancia
 * con posición y almacén vive en {@link Station}.
 *
 * <p>Además de los servicios, describe la geometría de atraque (boca, punto de
 * espera y corredor) y los parámetros del campo de contención (UPD-006-T02 y
 * UPD-006-T04). Las estaciones que no definen estos valores usan los mismos
 * valores por defecto que la nodriza histórica: boca en la parte inferior.</p>
 */
final class StationDef {

    /** Ángulo por defecto de la boca de atraque: abajo, como la bahía dibujada. */
    static final float DEFAULT_DOCK_ANGLE = 270f;
    /** Semiancho por defecto del corredor de aproximación. */
    static final float DEFAULT_APPROACH_HALF_WIDTH = 100f;
    /** Distancia por defecto del punto de espera respecto al centro. */
    static final float DEFAULT_APPROACH_EXTRA = 220f;

    final String name;
    /** Radio visual de la estructura, en unidades del mundo. */
    final float radius;
    /** Distancia máxima para iniciar el acople en la estación personal. */
    final float dockRadius;
    /** Radio de la zona segura: no se generan asteroides dentro. */
    final float safeRadius;

    /** Dirección de la boca de atraque, en grados. */
    final float dockAngleDeg;
    /** Distancia desde el centro de la estación hasta la boca. */
    final float dockOffset;
    /** Distancia del punto de espera respecto al centro. */
    final float approachDistance;
    /** Semiancho del corredor de entrada. */
    final float approachHalfWidth;
    /** Radio alrededor de la boca en el que se completa el acople. */
    final float dockPointRadius;
    /** Velocidad máxima para acoplar, en u/s. */
    final float dockMaxSpeed;
    /** Desviación máxima de rumbo respecto al eje de entrada, en grados. */
    final float dockMaxAngle;

    /** Energía del campo de contención (UPD-006-T04). */
    final float energyCapacity;
    /** Recarga de energía por segundo. */
    final float energyRegen;
    /** Consumo continuo del campo por segundo mientras está activo. */
    final float energyIdleDrain;
    /** Coste energético por unidad de impacto. */
    final float energyPerImpact;

    final boolean repair;
    final boolean refuel;
    final boolean oxygen;
    final boolean upgrades;
    final boolean market;
    final boolean storage;

    StationDef(String name, float radius, float dockRadius, float safeRadius,
               boolean repair, boolean refuel, boolean oxygen, boolean upgrades,
               boolean market, boolean storage) {
        this(name, radius, dockRadius, safeRadius, repair, refuel, oxygen,
                upgrades, market, storage, DEFAULT_DOCK_ANGLE);
    }

    StationDef(String name, float radius, float dockRadius, float safeRadius,
               boolean repair, boolean refuel, boolean oxygen, boolean upgrades,
               boolean market, boolean storage, float dockAngleDeg) {
        this(name, radius, dockRadius, safeRadius, repair, refuel, oxygen,
                upgrades, market, storage, dockAngleDeg,
                Math.max(0f, radius * 0.92f),
                Math.max(dockRadius, radius + DEFAULT_APPROACH_EXTRA),
                DEFAULT_APPROACH_HALF_WIDTH, 28f, 30f, 12f,
                1000f, 9f, 2.2f, 0.010f);
    }

    StationDef(String name, float radius, float dockRadius, float safeRadius,
               boolean repair, boolean refuel, boolean oxygen, boolean upgrades,
               boolean market, boolean storage, float dockAngleDeg,
               float dockOffset, float approachDistance, float approachHalfWidth,
               float dockPointRadius, float dockMaxSpeed, float dockMaxAngle,
               float energyCapacity, float energyRegen, float energyIdleDrain,
               float energyPerImpact) {
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
        this.dockAngleDeg = dockAngleDeg;
        this.dockOffset = Math.max(0f, dockOffset);
        this.approachDistance = Math.max(this.dockOffset + 20f, approachDistance);
        this.approachHalfWidth = Math.max(20f, approachHalfWidth);
        this.dockPointRadius = Math.max(8f, dockPointRadius);
        this.dockMaxSpeed = Math.max(1f, dockMaxSpeed);
        this.dockMaxAngle = Math.max(1f, dockMaxAngle);
        this.energyCapacity = Math.max(1f, energyCapacity);
        this.energyRegen = Math.max(0f, energyRegen);
        this.energyIdleDrain = Math.max(0f, energyIdleDrain);
        this.energyPerImpact = Math.max(0f, energyPerImpact);
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
