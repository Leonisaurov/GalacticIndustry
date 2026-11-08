package com.example.codegame;

/**
 * Piloto autónomo resumido: identidad, rol, nave abstracta, carga, combustible,
 * estado y objetivo. No tiene posición 2D ni se renderiza todavía; el cerebro y
 * el ciclo minero llegan en T-702.
 */
final class PilotAgent {

    enum Role {
        MINER,
        HAULER
    }

    /** Estados del ciclo autónomo (T-702 los usará). */
    enum State {
        DOCKED,
        TRAVEL,
        MINE,
        RETURN,
        SELL,
        REFUEL,
        IDLE
    }

    final String id;
    final String name;
    final Role role;
    final float cargoCapacity;
    final float maxFuel;
    final float speed;
    final float miningRate;
    final CargoHold cargo;
    State state = State.DOCKED;
    float fuel;
    String objective = "NODRIZA";

    PilotAgent(String id, String name, Role role, float cargoCapacity,
               float maxFuel, float speed, float miningRate) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.cargoCapacity = Math.max(1f, cargoCapacity);
        this.maxFuel = Math.max(1f, maxFuel);
        this.speed = Math.max(1f, speed);
        this.miningRate = Math.max(0.1f, miningRate);
        this.cargo = new CargoHold(this.cargoCapacity);
        this.fuel = this.maxFuel;
    }

    float fuelFraction() {
        return maxFuel <= 0f ? 0f : fuel / maxFuel;
    }

    void resetForNewRun() {
        state = State.DOCKED;
        fuel = maxFuel;
        cargo.clear();
        objective = "NODRIZA";
    }

    boolean docked() {
        return state == State.DOCKED;
    }
}
