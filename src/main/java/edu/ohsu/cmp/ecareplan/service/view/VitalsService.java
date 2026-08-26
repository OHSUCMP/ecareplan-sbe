package edu.ohsu.cmp.ecareplan.service.view;

import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.VitalsModel;
import edu.ohsu.cmp.ecareplan.model.view.CompositeBloodPressureModel;
import edu.ohsu.cmp.ecareplan.model.view.IVitalsModel;
import edu.ohsu.cmp.ecareplan.service.BaseService;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VitalsService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(VitalsService.class);

    private static final Coding SYSTOLIC_CODING = new Coding("http://loinc.org", "8480-6", "Systolic blood pressure");
    private static final Coding DIASTOLIC_CODING = new Coding("http://loinc.org", "8462-4", "Diastolic blood pressure");

    public List<IVitalsModel> getVitalsModels(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        List<IVitalsModel> list = new ArrayList<>();
        Map<String, Map<String, BloodPressureComponents>> map = new LinkedHashMap<>();

        for (VitalsModel vm : workspace.getAllDataSetModels(DataSet.VITALS)) {
            boolean individualSystolic = isIndividualSystolic(vm);
            boolean individualDiastolic = isIndividualDiastolic(vm);

            if (individualSystolic || individualDiastolic) {
                String iss = vm.getSourceEndpointIss();         // first break things out by source
                if ( ! map.containsKey(iss) ) {
                    map.put(iss, new LinkedHashMap<>());
                }

                String effectiveDateStr = vm.getEffectiveDate().toString();  // then break things out by date
                if ( ! map.get(iss).containsKey(effectiveDateStr) ) {
                    map.get(iss).put(effectiveDateStr, new BloodPressureComponents());
                }

                if (individualSystolic) {
                    map.get(iss).get(effectiveDateStr).setSystolic(vm);
                } else {
                    map.get(iss).get(effectiveDateStr).setDiastolic(vm);
                }

            } else {
                list.add(vm);
            }
        }

        for (Map<String, BloodPressureComponents> m : map.values()) {   // iterate over sources
            for (BloodPressureComponents bpc : m.values()) {            // iterate over generated pairs
                if (bpc.getSystolic() != null && bpc.getDiastolic() != null) {
                    logger.debug("Found matching systolic and diastolic components for composite BP reading {}/{} at {} (systolic resource ID: {}, diastolic resource ID: {})",
                            bpc.getSystolic().getResultValue(), bpc.getDiastolic().getResultValue(),
                            bpc.getSystolic().getEffectiveDate(),
                            bpc.getSystolic().getId(), bpc.getDiastolic().getId());
                    list.add(new CompositeBloodPressureModel(bpc.getSystolic(), bpc.getDiastolic()));

                } else if (bpc.getSystolic() != null) {
                    logger.debug("Found only systolic at {} from {} - skipping", bpc.getSystolic().getEffectiveDate(), bpc.getSystolic().getSourceEndpointIss());

                } else if (bpc.getDiastolic() != null) {
                    logger.debug("Found only diastolic at {} from {} - skipping", bpc.getDiastolic().getEffectiveDate(), bpc.getDiastolic().getSourceEndpointIss());
                }
            }
        }

        return list;
    }

    private boolean isIndividualSystolic(VitalsModel vm) {
        Observation o = vm.getSourceResource();
        return FhirUtil.hasCoding(o.getCode(), SYSTOLIC_CODING) && o.hasValueQuantity() && ! o.hasComponent();
    }

    private boolean isIndividualDiastolic(VitalsModel vm) {
        Observation o = vm.getSourceResource();
        return FhirUtil.hasCoding(o.getCode(), DIASTOLIC_CODING) && o.hasValueQuantity() && ! o.hasComponent();
    }

    private static final class BloodPressureComponents {
        private VitalsModel systolic = null;
        private VitalsModel diastolic = null;

        public VitalsModel getSystolic() {
            return systolic;
        }

        public void setSystolic(VitalsModel systolic) {
            this.systolic = systolic;
        }

        public VitalsModel getDiastolic() {
            return diastolic;
        }

        public void setDiastolic(VitalsModel diastolic) {
            this.diastolic = diastolic;
        }
    }
}
