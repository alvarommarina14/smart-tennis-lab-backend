package com.smarttennislab.catalog.dto;

import com.smarttennislab.catalog.model.Discipline;
import java.util.List;

public record KpiCatalogResponse(
        Discipline discipline,
        List<KpiCategoryResponse> categories) {
}
