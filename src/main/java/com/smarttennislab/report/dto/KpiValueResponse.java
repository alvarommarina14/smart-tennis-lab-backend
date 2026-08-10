package com.smarttennislab.report.dto;

import com.smarttennislab.catalog.model.Kpi;
import com.smarttennislab.catalog.model.KpiKind;
import com.smarttennislab.catalog.model.KpiUnit;
import java.math.BigDecimal;

public record KpiValueResponse(
        String code, String label, KpiKind kind, KpiUnit unit, BigDecimal value) {

    public static KpiValueResponse of(Kpi kpi, BigDecimal value) {
        return new KpiValueResponse(kpi.name(), kpi.getLabel(), kpi.getKind(), kpi.getUnit(), value);
    }
}
