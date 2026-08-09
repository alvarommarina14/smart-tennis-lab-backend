package com.smarttennislab.catalog;

// El orden de las constantes es el orden en que la app dibuja las secciones.
public enum KpiCategory {

    GENERAL_EFFICIENCY("Eficiencia general"),
    SERVE("Saque"),
    RETURN("Devolución"),
    POINT_DEFINITION("Definición del punto"),
    RALLY("Desarrollo del punto"),
    POINT_OUTCOME("Resultado del punto");

    private final String label;

    KpiCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
