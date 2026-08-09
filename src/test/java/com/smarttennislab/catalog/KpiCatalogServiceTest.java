package com.smarttennislab.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttennislab.catalog.dto.KpiCatalogResponse;
import com.smarttennislab.catalog.dto.KpiCategoryResponse;
import com.smarttennislab.catalog.dto.KpiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class KpiCatalogServiceTest {

    private final KpiCatalogService service = new KpiCatalogService();

    @Test
    void catalogoDeSinglesTraeLos23KpisDeLaV1() {
        KpiCatalogResponse catalog = service.catalogFor(Discipline.SINGLES);

        long total = catalog.categories().stream()
                .mapToLong(category -> category.kpis().size())
                .sum();

        assertThat(total).isEqualTo(23);
    }

    @Test
    void hay19ContadoresQueElProfePuedeTocar() {
        assertThat(Kpi.counters(Discipline.SINGLES)).hasSize(19);
    }

    @Test
    void los4KpisRestantesSonCalculados() {
        List<Kpi> derived = Kpi.forDiscipline(Discipline.SINGLES).stream()
                .filter(kpi -> kpi.getKind() == KpiKind.DERIVED)
                .toList();

        assertThat(derived).containsExactlyInAnyOrder(
                Kpi.TOTAL_POINTS_WON,
                Kpi.TOTAL_POINTS_PLAYED,
                Kpi.MATCH_DURATION_MINUTES,
                Kpi.POINTS_WON_PCT);
    }

    @Test
    void lasCategoriasVienenEnElOrdenDelDocumentoBase() {
        KpiCatalogResponse catalog = service.catalogFor(Discipline.SINGLES);

        assertThat(catalog.categories())
                .extracting(KpiCategoryResponse::code)
                .containsExactly(
                        "GENERAL_EFFICIENCY",
                        "SERVE",
                        "RETURN",
                        "POINT_DEFINITION",
                        "RALLY",
                        "POINT_OUTCOME");
    }

    @Test
    void laCategoriaSaqueTraeSus7KpisEnOrden() {
        KpiCatalogResponse catalog = service.catalogFor(Discipline.SINGLES);

        KpiCategoryResponse serve = catalog.categories().stream()
                .filter(category -> category.code().equals("SERVE"))
                .findFirst()
                .orElseThrow();

        assertThat(serve.kpis())
                .extracting(KpiResponse::code)
                .containsExactly(
                        "FIRST_SERVE_IN",
                        "FIRST_SERVE_OUT",
                        "SECOND_SERVE_IN",
                        "ACE",
                        "DOUBLE_FAULT",
                        "SERVE_ERROR_OUT",
                        "SERVE_ERROR_NET");
    }

    @Test
    void fromCodeIgnoraCodigosDesconocidos() {
        assertThat(Kpi.fromCode("ACE")).contains(Kpi.ACE);
        assertThat(Kpi.fromCode("NO_EXISTE")).isEmpty();
        assertThat(Kpi.fromCode(null)).isEmpty();
    }
}
