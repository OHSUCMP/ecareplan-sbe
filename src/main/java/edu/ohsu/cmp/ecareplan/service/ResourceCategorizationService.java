package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorization;
import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import edu.ohsu.cmp.ecareplan.entity.vsac.ValueSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import edu.ohsu.cmp.ecareplan.repository.ResourceCategorizationRepository;
import edu.ohsu.cmp.ecareplan.service.vsac.ConceptService;
import edu.ohsu.cmp.ecareplan.service.vsac.ValueSetService;
import edu.ohsu.cmp.ecareplan.util.CodeSystemUtil;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResourceCategorizationService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ValueSetService valueSetService;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ResourceCategorizationRepository repository;

    @Scheduled(cron = "0 0 3 1 * *")
    public void refreshValueSets() {
        logger.info("refreshing Resource Categorization ValueSets -");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -14);
        Date twoWeeksAgo = cal.getTime();   // don't update any more often than once every two weeks.  this should only
                                            // apply if the application is restarted.  under normal circumstances,
                                            // this function will get called monthly by the scheduler

        for (ResourceCategorization rc : repository.findAll()) {
            try {
                ValueSet valueSet = valueSetService.getValueSet(rc.getValuesetOid());
                if (valueSet == null || valueSet.getUpdated().before(twoWeeksAgo)) {
                    valueSetService.refresh(rc.getValuesetOid());
                }
            } catch (Exception e) {
                logger.error("caught {} refreshing ValueSet with OID={} - {}", e.getClass().getName(), rc.getValuesetOid(), e.getMessage(), e);
            }
        }
        logger.info("done refreshing Resource Categorization ValueSets.");
    }

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
                    if (concept != null) {
                        for (ValueSet valueSet : concept.getValueSets()) {
                            if (categorizationMap.containsKey(valueSet.getOid())) {
                                return categorizationMap.get(valueSet.getOid());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
