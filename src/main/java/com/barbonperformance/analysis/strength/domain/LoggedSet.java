package com.barbonperformance.analysis.strength.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class LoggedSet {

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(nullable = false)
    private int reps;

    @Column
    private BigDecimal weight;

    protected LoggedSet() {
        // for JPA
    }

    public LoggedSet(int setNumber, int reps, BigDecimal weight) {
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public int getReps() {
        return reps;
    }

    public BigDecimal getWeight() {
        return weight;
    }
}
