package com.smarttennislab.catalog;

import com.smarttennislab.catalog.dto.KpiCatalogResponse;
import com.smarttennislab.catalog.dto.KpiCategoryResponse;
import com.smarttennislab.catalog.dto.KpiResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KpiCatalogService {

    // Se respeta el orden de declaración de los enums: es el orden en que el profe espera ver los
    // botones en la cancha. Las categorías sin KPIs para la disciplina pedida no se devuelven.
    public KpiCatalogResponse catalogFor(Discipline discipline) {
        List<Kpi> applicable = Kpi.forDiscipline(discipline);

        List<KpiCategoryResponse> categories = Arrays.stream(KpiCategory.values())
                .map(category -> new KpiCategoryResponse(
                        category.name(),
                        category.getLabel(),
                        applicable.stream()
                                .filter(kpi -> kpi.getCategory() == category)
                                .map(KpiResponse::from)
                                .toList()))
                .filter(category -> !category.kpis().isEmpty())
                .toList();

        return new KpiCatalogResponse(discipline, categories);
    }
}
