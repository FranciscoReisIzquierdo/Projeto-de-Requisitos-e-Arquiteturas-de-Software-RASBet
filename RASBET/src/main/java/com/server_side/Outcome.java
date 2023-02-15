package com.server_side;

public class Outcome {
    private String equipa;
    private Double outcome;

    Outcome(String equipa, Double outcome) {
        this.equipa = equipa;
        this.outcome = outcome;
    }

    public String equipa() {
        return equipa;
    }

    public Double outcome() {
        return outcome;
    }
}
