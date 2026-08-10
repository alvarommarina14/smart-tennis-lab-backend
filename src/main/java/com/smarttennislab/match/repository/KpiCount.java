package com.smarttennislab.match.repository;

// Proyección del GROUP BY por KPI: no hace falta traer los eventos a memoria para contarlos.
public interface KpiCount {

    String getKpiCode();

    long getTotal();
}
