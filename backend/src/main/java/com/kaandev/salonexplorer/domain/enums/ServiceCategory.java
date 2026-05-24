package com.kaandev.salonexplorer.domain.enums;

public enum ServiceCategory {
    HAIR, NAILS, FACE, BODY, OTHER;

    public String dbValue() {
        return name().toLowerCase();
    }
}
