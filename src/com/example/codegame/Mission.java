package com.example.codegame;

/** Contrato de estación: entregar una cantidad de mineral antes de un plazo. */
final class Mission {

    enum Status {
        OFFERED,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    final String id;
    final Commodity commodity;
    final int amount;
    final int reward;
    /** Plazo total en segundos. */
    final float deadline;
    Status status = Status.OFFERED;
    float remaining;

    Mission(String id, Commodity commodity, int amount, int reward,
            float deadline) {
        this.id = id;
        this.commodity = commodity == null ? Commodity.HIERRO : commodity;
        this.amount = Math.max(1, amount);
        this.reward = Math.max(1, reward);
        this.deadline = Math.max(30f, deadline);
        this.remaining = this.deadline;
    }

    void restoreState(Status status, float remaining) {
        this.status = status == null ? Status.OFFERED : status;
        this.remaining = Math.max(0f, Math.min(deadline, remaining));
    }

    String title() {
        return "ENTREGAR " + amount + " " + commodity.displayName.toUpperCase();
    }

    boolean offered() {
        return status == Status.OFFERED;
    }

    boolean active() {
        return status == Status.ACTIVE;
    }

    boolean finished() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }
}
