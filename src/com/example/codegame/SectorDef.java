package com.example.codegame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sector finito de unos 12.000 x 12.000 u. Define límites, semilla, perfil de
 * generación y estaciones. El runtime 2D llega en T-802; aquí solo viven los
 * datos configurables del universo.
 */
final class SectorDef {

    /** Perfil de generación de asteroides y recursos. */
    enum Profile {
        INICIAL,
        CINTURON,
        METALICO,
        HELADO
    }

    static final float DEFAULT_WIDTH = 12000f;
    static final float DEFAULT_HEIGHT = 12000f;

    final String id;
    final String name;
    final String systemId;
    final long seed;
    final float width;
    final float height;
    final Profile profile;
    /** Radio de seguridad alrededor de la estación principal. */
    final float safeRadius;
    /** Posición abstracta en el mapa de universo (unidades jugables). */
    final float mapX;
    final float mapY;
    private final List<StationDef> stations = new ArrayList<>();

    SectorDef(String id, String name, String systemId, long seed,
              float width, float height, Profile profile, float safeRadius) {
        this(id, name, systemId, seed, width, height, profile, safeRadius, 0f, 0f);
    }

    SectorDef(String id, String name, String systemId, long seed,
              float width, float height, Profile profile, float safeRadius,
              float mapX, float mapY) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id de sector vacio");
        }
        if (width <= 0f || height <= 0f) {
            throw new IllegalArgumentException("limites de sector invalidos");
        }
        this.id = id;
        this.name = name == null ? id : name;
        this.systemId = systemId;
        this.seed = seed;
        this.width = width;
        this.height = height;
        this.profile = profile == null ? Profile.INICIAL : profile;
        this.safeRadius = Math.max(0f, safeRadius);
        this.mapX = mapX;
        this.mapY = mapY;
    }

    float distanceTo(SectorDef other) {
        if (other == null) {
            return Float.MAX_VALUE;
        }
        float dx = mapX - other.mapX;
        float dy = mapY - other.mapY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    SectorDef addStation(StationDef station) {
        if (station != null && !stations.contains(station)) {
            stations.add(station);
        }
        return this;
    }

    List<StationDef> stations() {
        return Collections.unmodifiableList(stations);
    }

    StationDef station(int index) {
        return index >= 0 && index < stations.size() ? stations.get(index) : null;
    }

    int stationCount() {
        return stations.size();
    }

    /** Centro del sector en coordenadas de mundo (0,0). */
    float centerX() {
        return 0f;
    }

    float centerY() {
        return 0f;
    }

    boolean contains(float x, float y) {
        return x >= -width * 0.5f && x <= width * 0.5f
                && y >= -height * 0.5f && y <= height * 0.5f;
    }
}
