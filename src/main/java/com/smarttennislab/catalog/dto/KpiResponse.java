package com.smarttennislab.catalog.dto;

import com.smarttennislab.catalog.Kpi;
import com.smarttennislab.catalog.KpiKind;
import com.smarttennislab.catalog.KpiUnit;

public record KpiResponse(
        String code,
        String label,
        KpiKind kind,
        KpiUnit unit) {

    public static KpiResponse from(Kpi kpi) {
        return new KpiResponse(kpi.name(), kpi.getLabel(), kpi.getKind(), kpi.getUnit());
    }
}
