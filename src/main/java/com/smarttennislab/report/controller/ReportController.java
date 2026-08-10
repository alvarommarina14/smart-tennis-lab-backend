package com.smarttennislab.report.controller;

import com.smarttennislab.auth.model.CoachPrincipal;
import com.smarttennislab.report.dto.MatchReportResponse;
import com.smarttennislab.report.service.MatchReportService;
import com.smarttennislab.report.service.ReportCsvWriter;
import com.smarttennislab.report.service.ReportPdfWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/report")
@Tag(name = "Reportes", description = "Totales, desglose por set y export")
public class ReportController {

    private final MatchReportService reportService;
    private final ReportCsvWriter csvWriter;
    private final ReportPdfWriter pdfWriter;

    public ReportController(
            MatchReportService reportService, ReportCsvWriter csvWriter, ReportPdfWriter pdfWriter) {
        this.reportService = reportService;
        this.csvWriter = csvWriter;
        this.pdfWriter = pdfWriter;
    }

    @GetMapping
    @Operation(summary = "Reporte del partido con totales y desglose por set")
    public MatchReportResponse report(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID matchId) {
        return reportService.report(coach.id(), matchId);
    }

    @GetMapping(produces = "text/csv")
    @Operation(summary = "Descargar el reporte en CSV")
    public ResponseEntity<byte[]> csv(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID matchId) {
        MatchReportResponse report = reportService.report(coach.id(), matchId);
        return download(csvWriter.write(report), fileName(report, "csv"), "text/csv; charset=UTF-8");
    }

    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descargar el reporte en PDF")
    public ResponseEntity<byte[]> pdf(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID matchId) {
        MatchReportResponse report = reportService.report(coach.id(), matchId);
        return download(pdfWriter.write(report), fileName(report, "pdf"), MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> download(byte[] body, String fileName, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }

    private static String fileName(MatchReportResponse report, String extension) {
        String alumno = report.playerName() == null ? "partido" : report.playerName().replace(' ', '-');
        return "reporte-" + alumno.toLowerCase() + "." + extension;
    }
}
