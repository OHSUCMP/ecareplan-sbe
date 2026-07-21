package edu.ohsu.cmp.ecareplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.ecareplan.entity.MedicationFlag;
import edu.ohsu.cmp.ecareplan.entity.MedicationFlagRxClass;
import edu.ohsu.cmp.ecareplan.http.HttpRequest;
import edu.ohsu.cmp.ecareplan.http.HttpResponse;
import edu.ohsu.cmp.ecareplan.model.dataset.MedicationModel;
import edu.ohsu.cmp.ecareplan.repository.MedicationFlagRepository;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class MedicationFlagService {
    private final Logger logger = LoggerFactory.getLogger(MedicationFlagService.class);
    private final Cache<String, JsonNode> cache;
    private final ExecutorService executorService;

    private static final String RXNORM_SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";
    private static final int POOL_SIZE = 5;

    @Autowired
    private MedicationFlagRepository repository;

    public MedicationFlagService() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public void appendMedicationFlags(MedicationModel medicationModel) {
        if (medicationModel == null) return;

        Set<String> rxCuis = new HashSet<>();
        for (Coding coding : medicationModel.getCodings(RXNORM_SYSTEM)) {
            if (coding.hasSystem() && coding.hasCode()) {
                rxCuis.add(coding.getCode());
            }
        }

        medicationModel.setFlags(getMedicationFlags(rxCuis));
    }

    public List<MedicationFlag> getMedicationFlags(Collection<String> rxCuis) {
        if (rxCuis == null || rxCuis.isEmpty()) return null;

        Map<String, MedicationFlag> flagMap = new HashMap<>();
        for (MedicationFlag flag : repository.findAll()) {
            for (MedicationFlagRxClass rxClass : flag.getRxClassList()) {
                flagMap.put(rxClass.getRxClass(), flag);
            }
        }

        Map<String, MedicationFlag> map = new LinkedHashMap<>();

        List<RxClassSummary> summaryList = getRxClass(rxCuis);
        for (RxClassSummary summary : summaryList) {
            if (flagMap.containsKey(summary.getClassId())) {
                map.put(summary.getClassId(), flagMap.get(summary.getClassId()));
            }
        }

        return new ArrayList<>(map.values());
    }


    private List<RxClassSummary> getRxClass(String rxCui) {
        return getRxClass(Collections.singletonList(rxCui));
    }


    private List<RxClassSummary> getRxClass(Collection<String> rxCuiList) {
        if (rxCuiList == null || rxCuiList.isEmpty()) return null;

        List<Callable<List<RxClassSummary>>> callables = new ArrayList<>();
        for (String rxCui : rxCuiList) {
            callables.add(new Callable<>() {
                @Override
                public List<RxClassSummary> call() {
                    try {
                        JsonNode root = getRxClassByRxCui(rxCui);
                        JsonNode rxclassDrugInfoList = root.path("rxclassDrugInfoList");
                        JsonNode rxclassDrugInfo = rxclassDrugInfoList.path("rxclassDrugInfo");

                        if (rxclassDrugInfo.isArray()) {
                            Map<String, RxClassSummary> map = new LinkedHashMap<>();
                            for (JsonNode rxcdi : rxclassDrugInfo) {
                                // first, ensure that we only consider those rxclassDrugInfo items that reference
                                // RxCuis that we care about
                                JsonNode mc = rxcdi.path("minConcept");
                                String mcRxCui = mc.path("rxcui").asText();
                                if ( ! rxCuiList.contains(mcRxCui) ) {
                                    continue;
                                }

                                logger.debug("getRxClass: found minConcept with RxCui={}", mcRxCui);

                                // this rxClassDrugInfo item references a minConcept with an rxcui that is in the list
                                // that we care about.  grab its class and add it to the list

                                JsonNode rxcmci = rxcdi.path("rxclassMinConceptItem");
                                if (rxcmci != null && rxcmci.path("classType").asText().equals("ATC1-4")) {
                                    String classId = rxcmci.path("classId").asText(null);
                                    String className = rxcmci.path("className").asText(null);

                                    logger.debug("getRxClass: adding classId={}, className={} for RxCui={}", classId, className, mcRxCui);

                                    String key = rxCui + ":" + classId;
                                    if ( ! map.containsKey(key) ) {
                                        map.put(key, new RxClassSummary(mcRxCui, classId, className));
                                    }
                                }
                            }
                            return new ArrayList<>(map.values());
                        }

                    } catch (Exception e) {
                        logger.error("Error getting MedicationFlag for rxCui={}", rxCui, e);
                    }

                    return Collections.emptyList();
                }
            });
        }

        Map<String, RxClassSummary> map = new LinkedHashMap<>();

        try {
            for (Future<List<RxClassSummary>> future : executorService.invokeAll(callables)) {
                try {
                    List<RxClassSummary> result = future.get();
                    for (RxClassSummary summary : result) {
                        if ( ! map.containsKey(summary.getClassId()) ) {
                            map.put(summary.getClassId(), summary);
                        }
                    }

                } catch (ExecutionException e) {
                    logger.error("caught {} getting RxClass for rxCuis - {}", e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("caught {} getting RxClass for rxCuis - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }

        return new ArrayList<>(map.values());
    }

    private JsonNode getRxClassByRxCui(String rxCui) {
        return cache.get(rxCui, s -> {
            try {
                HttpResponse response = new HttpRequest().get("https://rxnav.nlm.nih.gov/REST/rxclass/class/byRxcui.json?rxcui=" + s + "&relaSource=ATCPROD");
                if (response.getResponseCode() >= 200 && response.getResponseCode() <= 300) {
                    logger.debug("got {} response for {} : {}", response.getResponseCode(), s, response.getResponseBody());
                    return new ObjectMapper().readTree(response.getResponseBody());
                } else {
                    logger.error("got {} response for {} : {}", response.getResponseCode(), s, response.getResponseBody());
                    return null;
                }
            } catch (Exception e) {
                logger.error("caught {} building raw RxClass data for {} - {}", e.getClass().getSimpleName(), s, e.getMessage(), e);
                return null;
            }
        });
    }

    private static final class RxClassSummary {
        private final String rxCui;
        private final String classId;
        private final String className;

        public RxClassSummary(String rxCui, String classId, String className) {
            this.rxCui = rxCui;
            this.classId = classId;
            this.className = className;
        }

        public String getRxCui() {
            return rxCui;
        }

        public String getClassId() {
            return classId;
        }

        public String getClassName() {
            return className;
        }
    }
}
