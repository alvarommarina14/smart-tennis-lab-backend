package com.smarttennislab.catalog.dto;

import java.util.List;

public record KpiCategoryResponse(
        String code,
        String label,
        List<KpiResponse> kpis) {
}
