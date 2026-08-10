package com.smarttennislab.report.service;

import com.smarttennislab.report.dto.KpiValueResponse;
import com.smarttennislab.report.dto.MatchReportResponse;
import com.smarttennislab.report.dto.ReportCategoryResponse;
import com.smarttennislab.report.dto.SetReportResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

@Component
public class ReportCsvWriter {

    // Una fila por KPI, una columna por set. Es el formato que se abre en Excel sin pelear.
    public byte[] write(MatchReportResponse report) {
        StringWriter out = new StringWriter();

        List<String> cabecera = new ArrayList<>(List.of("Categoría", "KPI", "Código", "Unidad", "Total"));
        report.sets().forEach(set -> cabecera.add("Set " + set.setNumber()));

        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(cabecera.toArray(String[]::new)).get();

        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            Map<UUID, Map<String, BigDecimal>> porSet = valoresPorSet(report.sets());

            for (ReportCategoryResponse categoria : report.categories()) {
                for (KpiValueResponse kpi : categoria.kpis()) {
                    List<Object> fila = new ArrayList<>(
                            List.of(categoria.label(), kpi.label(), kpi.code(), kpi.unit(), kpi.value()));
                    for (SetReportResponse set : report.sets()) {
                        fila.add(porSet.get(set.setId()).getOrDefault(kpi.code(), BigDecimal.ZERO));
                    }
                    printer.printRecord(fila);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el CSV", ex);
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<UUID, Map<String, BigDecimal>> valoresPorSet(List<SetReportResponse> sets) {
        Map<UUID, Map<String, BigDecimal>> porSet = new HashMap<>();
        for (SetReportResponse set : sets) {
            Map<String, BigDecimal> valores = new HashMap<>();
            set.kpis().forEach(kpi -> valores.put(kpi.code(), kpi.value()));
            porSet.put(set.setId(), valores);
        }
        return porSet;
    }
}
