package edu.ohsu.cmp.ecareplan.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.ecareplan.entity.MedicationFlag;
import edu.ohsu.cmp.ecareplan.http.HttpRequest;
import edu.ohsu.cmp.ecareplan.http.HttpResponse;
import edu.ohsu.cmp.ecareplan.model.dataset.MedicationModel;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class MedicationFlagService {
    private final Logger logger = LoggerFactory.getLogger(MedicationFlagService.class);
    private final Cache<String, MedicationFlag> cache;
    private final ExecutorService executorService;

    private static final String RXNORM_SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";
    private static final int POOL_SIZE = 5;

    public MedicationFlagService() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        this.executorService = Executors.newFixedThreadPool(POOL_SIZE);
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

        Map<String, MedicationFlag> map = new LinkedHashMap<>();

        for (String rxCui : rxCuis) {
            MedicationFlag mf = getMedicationFlag(rxCui);
            if (mf != null) {
                map.put(rxCui, mf);
            }
        }

        return new ArrayList<>(map.values());
    }

    public MedicationFlag getMedicationFlag(String rxCui) {
        return cache.get(rxCui, s -> {
            try {
                HttpResponse response = new HttpRequest().get("https://rxnav.nlm.nih.gov/REST/rxclass/class/byRxcui.json?rxcui=" + rxCui + "&relaSource=ATCPROD");
                if (response.getResponseCode() >= 200 && response.getResponseCode() <= 300) {
                    logger.info("got response for {} : {}", rxCui, response.getResponseBody());

                } else {
                    logger.error("got response code {} from rxnav for rxCui={}", response.getResponseCode(), rxCui);
                }

            } catch (Exception e) {
                logger.error("Error getting MedicationFlag for rxCui={}", rxCui, e);
            }

            return null;
        });
    }
}
