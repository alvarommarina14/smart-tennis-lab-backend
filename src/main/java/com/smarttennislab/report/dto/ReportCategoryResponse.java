package com.smarttennislab.report.dto;

import java.util.List;

public record ReportCategoryResponse(String code, String label, List<KpiValueResponse> kpis) {}
