package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorization;
import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import edu.ohsu.cmp.ecareplan.entity.vsac.ValueSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import edu.ohsu.cmp.ecareplan.repository.ResourceCategorizationRepository;
import edu.ohsu.cmp.ecareplan.service.vsac.ConceptService;
import edu.ohsu.cmp.ecareplan.util.CodeSystemUtil;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResourceCategorizationService extends BaseService {
    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ResourceCategorizationRepository repository;

    public List<ResourceCategorization> getCategorizations(DataSetName dataSetName, CodeableConcept codeableConcept) {
        List<ResourceCategorization> list = new ArrayList<>();

        if (codeableConcept != null && codeableConcept.hasCoding()) {
            Set<String> conceptValueSetOids = new HashSet<>();

            for (Coding coding : codeableConcept.getCoding()) {
                if (coding.hasCode() && coding.hasSystem()) {
                    String codeSystemOid = CodeSystemUtil.getOid(coding.getSystem());
                    Concept concept = conceptService.getConcept(coding.getCode(), codeSystemOid);
                    for (ValueSet valueSet : concept.getValueSets()) {
                        conceptValueSetOids.add(valueSet.getOid());
                    }
                }
            }

            for (ResourceCategorization rc : repository.findByDataSetName(dataSetName)) {
                if (conceptValueSetOids.contains(rc.getValuesetOid())) {
                    list.add(rc);
                }
            }
        }

        return list;
    }

    // getFirstCategorization is intended to perform the same logic as getCategorizations above,
    // except that it will short-circuit its operation as soon as a match is made
    public ResourceCategorization getFirstCategorization(DataSetName dataSetName, CodeableConcept codeableConcept) {
        if (codeableConcept != null && codeableConcept.hasCoding()) {
            Map<String, ResourceCategorization> categorizationMap = new HashMap<>();
            for (ResourceCategorization rc : repository.findByDataSetName(dataSetName)) {
                categorizationMap.put(rc.getValuesetOid(), rc);
            }

            for (Coding coding : codeableConcept.getCoding()) {
                if (coding.hasCode() && coding.hasSystem()) {
                    String codeSystemOid = CodeSystemUtil.getOid(coding.getSystem());
                    Concept concept = conceptService.getConcept(coding.getCode(), codeSystemOid);
                    for (ValueSet valueSet : concept.getValueSets()) {
                        if (categorizationMap.containsKey(valueSet.getOid())) {
                            return categorizationMap.get(valueSet.getOid());
                        }
                    }
                }
            }
        }

        return null;
    }
}
