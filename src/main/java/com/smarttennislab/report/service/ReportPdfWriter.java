package com.smarttennislab.report.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.smarttennislab.report.dto.KpiValueResponse;
import com.smarttennislab.report.dto.MatchReportResponse;
import com.smarttennislab.report.dto.ReportCategoryResponse;
import com.smarttennislab.report.dto.SetReportResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ReportPdfWriter {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final TemplateEngine templateEngine;

    public ReportPdfWriter(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // La plantilla no busca nada: recibe las filas ya armadas, con los valores por set en el mismo
    // orden que las columnas.
    public record Row(String kpiLabel, boolean derived, String total, List<String> porSet) {}

    public record Section(String label, List<Row> rows) {}

    public byte[] write(MatchReportResponse report) {
        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("inicio", FECHA.format(report.startedAt()));
        context.setVariable("fin", report.finishedAt() == null ? "—" : FECHA.format(report.finishedAt()));
        context.setVariable("secciones", sections(report));

        String html = templateEngine.process("match-report", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el PDF", ex);
        }
    }

    private List<Section> sections(MatchReportResponse report) {
        Map<UUID, Map<String, KpiValueResponse>> porSet = new HashMap<>();
        for (SetReportResponse set : report.sets()) {
            Map<String, KpiValueResponse> valores = new HashMap<>();
            set.kpis().forEach(kpi -> valores.put(kpi.code(), kpi));
            porSet.put(set.setId(), valores);
        }

        List<Section> secciones = new ArrayList<>();
        for (ReportCategoryResponse categoria : report.categories()) {
            List<Row> filas = new ArrayList<>();
            for (KpiValueResponse kpi : categoria.kpis()) {
                List<String> valoresPorSet = new ArrayList<>();
                for (SetReportResponse set : report.sets()) {
                    KpiValueResponse delSet = porSet.get(set.setId()).get(kpi.code());
                    valoresPorSet.add(format(delSet == null ? BigDecimal.ZERO : delSet.value(), kpi));
                }
                filas.add(new Row(
                        kpi.label(),
                        kpi.kind().name().equals("DERIVED"),
                        format(kpi.value(), kpi),
                        valoresPorSet));
            }
            secciones.add(new Section(categoria.label(), filas));
        }
        return secciones;
    }

    private static String format(BigDecimal value, KpiValueResponse kpi) {
        return switch (kpi.unit()) {
            case PERCENTAGE -> value + "%";
            case MINUTES -> value + " min";
            case COUNT -> value.stripTrailingZeros().toPlainString();
        };
    }
}
