package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Provenance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseResourceTransformer implements ResourceTransformer {
    protected void appendProvenance(List<? extends BaseDataSetModel> list, Bundle bundle) {
        if (list == null || list.isEmpty()) return;
        if (bundle == null || ! bundle.hasEntry()) return;

        Map<String, Provenance> map = new LinkedHashMap<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Provenance provenance) {
                String referenceId = provenance.getTargetFirstRep().getReferenceElement().getIdPart();
                map.put(referenceId, provenance);
            }
        }

        if (map.isEmpty()) return;

        for (BaseDataSetModel item : list) {
            if (map.containsKey(item.getId())) {
                item.setProvenance(map.get(item.getId()));
            }
        }
    }
}
