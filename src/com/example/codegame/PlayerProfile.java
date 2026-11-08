package com.example.codegame;

/**
 * Identidad persistente del jugador y su progreso de cuenta.
 *
 * <p>En esta etapa el perfil solo guarda la identidad y la referencia al
 * progreso existente ({@link Upgrades}); no contiene todavia al piloto fisico
 * ni al loadout de la nave. Esos contenedores llegan en T-102 y T-103.</p>
 */
class PlayerProfile {

    static final String DEFAULT_PLAYER_ID = "local-player";
    static final String DEFAULT_ID = "pilot-1";
    static final String DEFAULT_NAME = "PILOTO";

    /** Cuenta o jugador local; permitira varias partidas/personajes. */
    String playerId;
    /** Identidad estable del personaje; permitira varios personajes en el futuro. */
    String id;
    String name;
    /** Progreso persistente compartido: monedas, niveles y partida guardada. */
    final Upgrades upgrades;

    PlayerProfile(Upgrades upgrades) {
        this(DEFAULT_PLAYER_ID, DEFAULT_ID, DEFAULT_NAME, upgrades);
    }

    PlayerProfile(String id, String name, Upgrades upgrades) {
        this(DEFAULT_PLAYER_ID, id, name, upgrades);
    }

    PlayerProfile(String playerId, String id, String name, Upgrades upgrades) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id vacio");
        }
        if (upgrades == null) {
            throw new IllegalArgumentException("upgrades nulo");
        }
        this.playerId = playerId == null || playerId.isEmpty() ? DEFAULT_PLAYER_ID : playerId;
        this.id = id;
        this.name = name == null || name.isEmpty() ? DEFAULT_NAME : name;
        this.upgrades = upgrades;
    }

    /** Aplica identidad leida de un guardado; mantiene valores por defecto si faltan. */
    void restoreIdentity(String savedPlayerId, String savedCharacterId, String savedName) {
        if (savedPlayerId != null && !savedPlayerId.isEmpty()) {
            playerId = savedPlayerId;
        }
        if (savedCharacterId != null && !savedCharacterId.isEmpty()) {
            id = savedCharacterId;
        }
        name = savedName == null || savedName.isEmpty() ? DEFAULT_NAME : savedName;
    }
}
