package com.quini6.analytics.domain.enums;

public enum Modalidad {
    TRADICIONAL("Tradicional"),
    SEGUNDA("La Segunda"),
    REVANCHA("Revancha"),
    SIEMPRE_SALE("Siempre Sale"),
    POZO_EXTRA("Pozo Extra");

    private final String displayName;

    Modalidad(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
