package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.DiagnosticReport;

public class DiagnosticReportModel extends BaseDataSetModel<DiagnosticReport> {
    public DiagnosticReportModel(DiagnosticReport diagnosticReport) {
        super(diagnosticReport);
    }

    @Override
    public DiagnosticReport toResourceForSDSExport() {
        return sourceResource;
    }
}
