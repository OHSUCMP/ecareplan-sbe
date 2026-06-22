package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.ResourceCategorization;
import edu.ohsu.cmp.ecareplan.entity.ResourceCategorizationCoding;
import edu.ohsu.cmp.ecareplan.entity.ResourceCategorizationValueSet;
import edu.ohsu.cmp.ecareplan.entity.vsac.Concept;
import edu.ohsu.cmp.ecareplan.entity.vsac.ValueSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import edu.ohsu.cmp.ecareplan.repository.ResourceCategorizationCodingRepository;
import edu.ohsu.cmp.ecareplan.repository.ResourceCategorizationValueSetRepository;
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
    private ResourceCategorizationValueSetRepository valueSetRepository;

    @Autowired
    private ResourceCategorizationCodingRepository codingRepository;

    @Scheduled(cron = "0 0 3 1 * *")
    public void refreshValueSets() {
        logger.info("refreshing Resource Categorization ValueSets -");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -14);
        Date twoWeeksAgo = cal.getTime();   // don't update any more often than once every two weeks.  this should only
                                            // apply if the application is restarted.  under normal circumstances,
                                            // this function will get called monthly by the scheduler

        for (ResourceCategorizationValueSet rc : valueSetRepository.findAll()) {
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
            List<ResourceCategorizationValueSet> rcvsList = valueSetRepository.findByDataSetName(dataSetName);
            if ( ! rcvsList.isEmpty() ) {
                // collect all valueSet OIDs associated with concepts represented in codeableConcept
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

                // check for matching valueSet OIDs in categorization entities, adding any that are found
                for (ResourceCategorizationValueSet rc : rcvsList) {
                    if (conceptValueSetOids.contains(rc.getValuesetOid())) {
                        list.add(rc);
                    }
                }
            }

            // now check for specific coding categorizations
            List<ResourceCategorizationCoding> rccList = codingRepository.findByDataSetName(dataSetName);
            if ( ! rccList.isEmpty() ) {
                Set<String> codingKeySet = new HashSet<>();
                // collect all codings represented in codeableConcept, stored as system|code, since that's how
                // we'll look for it later
                for (Coding coding : codeableConcept.getCoding()) {
                    if (coding.hasCode() && coding.hasSystem()) {
                        String codeSystemUrl = CodeSystemUtil.getUrl(coding.getSystem());
                        String key = buildKey(codeSystemUrl,coding.getCode());
                        codingKeySet.add(key);
                    }
                }

                // check for any matching codings in categorization entities, adding any that are found
                for (ResourceCategorizationCoding rcc : rccList) {
                    if (codingKeySet.contains(buildKey(rcc.getCodeSystemUrl(), rcc.getCode()))) {
                        list.add(rcc);
                    }
                }
            }
        }

        return list;
    }

    // getFirstCategorization is intended to perform the same logic as getCategorizations above,
    // except that it will short-circuit its operation as soon as a match is made
    public ResourceCategorization getFirstCategorization(DataSetName dataSetName, CodeableConcept codeableConcept) {
        if (codeableConcept != null && codeableConcept.hasCoding()) {
            Map<String, ResourceCategorizationValueSet> valueSetCategorizationMap = new HashMap<>();
            for (ResourceCategorizationValueSet rc : valueSetRepository.findByDataSetName(dataSetName)) {
                valueSetCategorizationMap.put(rc.getValuesetOid(), rc);
            }

            if ( ! valueSetCategorizationMap.isEmpty() ) {
                for (Coding coding : codeableConcept.getCoding()) {
                    if (coding.hasCode() && coding.hasSystem()) {
                        String codeSystemOid = CodeSystemUtil.getOid(coding.getSystem());
                        Concept concept = conceptService.getConcept(coding.getCode(), codeSystemOid);
                        if (concept != null) {
                            for (ValueSet valueSet : concept.getValueSets()) {
                                if (valueSetCategorizationMap.containsKey(valueSet.getOid())) {
                                    return valueSetCategorizationMap.get(valueSet.getOid());
                                }
                            }
                        }
                    }
                }
            }

            Map<String, ResourceCategorizationCoding> codingCategorizationMap = new HashMap<>();
            for (ResourceCategorizationCoding rc : codingRepository.findByDataSetName(dataSetName)) {
                codingCategorizationMap.put(buildKey(rc.getCodeSystemUrl(), rc.getCode()), rc);
            }

            if ( ! codingCategorizationMap.isEmpty() ) {
                for (Coding coding : codeableConcept.getCoding()) {
                    if (coding.hasCode() && coding.hasSystem()) {
                        String codeSystemUrl = CodeSystemUtil.getUrl(coding.getSystem());
                        String key = buildKey(codeSystemUrl, coding.getCode());
                        if (codingCategorizationMap.containsKey(key)) {
                            return codingCategorizationMap.get(key);
                        }
                    }
                }
            }
        }

        return null;
    }

    private String buildKey(String systemUrl, String code) {
        return systemUrl + "|" + code;
    }
}
