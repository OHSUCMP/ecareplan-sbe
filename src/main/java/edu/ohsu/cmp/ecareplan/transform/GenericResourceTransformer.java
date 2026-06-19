package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorization;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.service.ResourceCategorizationService;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class GenericResourceTransformer extends BaseResourceTransformer {
    private ResourceCategorizationService resourceCategorizationService;

    public GenericResourceTransformer(ResourceCategorizationService rcs) {
        this.resourceCategorizationService = rcs;
    }

    @Override
    public PatientModel transformPatient(Patient patient) {
        return new PatientModel(patient);
    }

    @Override
    public List<AssessmentModel> transformAssessments(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<AssessmentModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof QuestionnaireResponse questionnaireResponse) {
                list.add(new AssessmentModel(questionnaireResponse));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<CarePlanModel> transformCarePlans(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<CarePlanModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof CarePlan carePlan) {
                list.add(new CarePlanModel(carePlan));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<CareTeamModel> transformCareTeams(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<CareTeamModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof CareTeam careTeam) {
                list.add(new CareTeamModel(careTeam));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<ClinicalNoteModel> transformClinicalNotes(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<ClinicalNoteModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof DocumentReference documentReference) {
                list.add(new ClinicalNoteModel(documentReference));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<ConditionModel> transformConditions(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<ConditionModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Condition condition) {
                list.add(new ConditionModel(condition));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<DiagnosticReportModel> transformDiagnosticReports(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<DiagnosticReportModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof DiagnosticReport diagnosticReport) {
                list.add(new DiagnosticReportModel(diagnosticReport));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<GoalModel> transformGoals(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<GoalModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Goal goal) {
                list.add(new GoalModel(goal));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<ImmunizationModel> transformImmunizations(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<ImmunizationModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Immunization immunization) {
                list.add(new ImmunizationModel(immunization));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<InteractionModel> transformInteractions(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<InteractionModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Encounter encounter) {
                list.add(new InteractionModel(encounter));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<MedicationModel> transformMedications(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<MedicationModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof MedicationRequest mr) {
                Medication m = null;
                CodeableConcept cc = null;
                if (mr.hasMedicationCodeableConcept()) {
                    cc = mr.getMedicationCodeableConcept();

                } else if (mr.hasMedicationReference() && FhirUtil.bundleContainsReference(bundle, mr.getMedicationReference())) {
                    m = FhirUtil.getResourceFromBundleByReference(bundle, Medication.class, mr.getMedicationReference().getReference());
                    cc = m != null && m.hasCode() ?
                            m.getCode() :
                            null;
                }
                String category = getMedicationCategory(cc);

                Practitioner p = null;
                if (mr.hasRequester() && mr.getRequester().hasReference() && FhirUtil.bundleContainsReference(bundle, mr.getRequester().getReference())) {
                    p = FhirUtil.getResourceFromBundleByReference(bundle, Practitioner.class, mr.getRequester().getReference());
                }

                list.add(new MedicationModel(mr, m, p, category));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    private String getMedicationCategory(CodeableConcept cc) {
        if (cc == null) return null;

        ResourceCategorization rc = resourceCategorizationService.getFirstCategorization(DataSetName.MEDICATIONS, cc);
        return rc != null ?
                rc.getCategory() :
                null;
    }

    @Override
    public List<ProcedureModel> transformProcedures(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<ProcedureModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Procedure procedure) {
                list.add(new ProcedureModel(procedure));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<ServiceRequestModel> transformServiceRequests(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<ServiceRequestModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof ServiceRequest serviceRequest) {
                list.add(new ServiceRequestModel(serviceRequest));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<SocialHistoryModel> transformSocialHistories(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<SocialHistoryModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Observation observation) {
                list.add(new SocialHistoryModel(observation));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<SurveyObservationModel> transformSurveyObservations(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<SurveyObservationModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Observation observation) {
                list.add(new SurveyObservationModel(observation));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<TestModel> transformTests(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<TestModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Observation observation) {
                list.add(new TestModel(observation));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }

    @Override
    public List<VitalsModel> transformVitals(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) return List.of();
        List<VitalsModel> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Observation observation) {
                list.add(new VitalsModel(observation));
            }
        }
        appendProvenance(list, bundle);
        return list;
    }
}
