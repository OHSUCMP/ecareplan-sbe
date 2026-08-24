package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.MedicationFlag;
import edu.ohsu.cmp.ecareplan.model.dataset.MedicationModel;
import edu.ohsu.cmp.ecareplan.repository.MedicationFlagRepository;
import edu.ohsu.cmp.ecareplan.service.rxclass.RxClassService;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MedicationFlagService {
    private final Logger logger = LoggerFactory.getLogger(MedicationFlagService.class);

    private static final String RXNORM_SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";

    @Autowired
    private RxClassService rxClassService;

    @Autowired
    private MedicationFlagRepository repository;

    @Scheduled(cron = "0 0 2 1 * *")
    public void refreshRxClassMembers() {
        logger.info("refreshing RxClass Members -");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -14);
        Date twoWeeksAgo = cal.getTime();   // don't update any more often than once every two weeks.  this should only
                                            // apply if the application is restarted.  under normal circumstances,
                                            // this function will get called monthly by the scheduler

        for (MedicationFlag medicationFlag : repository.findAll()) {
            try {
                if (medicationFlag.getLastRefreshed() == null || medicationFlag.getLastRefreshed().before(twoWeeksAgo)) {
                    rxClassService.refresh(medicationFlag.getRxClass());
                    medicationFlag.setLastRefreshed(new Date());
                    repository.save(medicationFlag);
                }

            } catch (Exception e) {
                logger.error("caught {} refreshing RxClass members for rxClass {} - {}", e.getClass().getName(), medicationFlag.getRxClass(), e.getMessage(), e);
            }
        }
        logger.info("done refreshing RxClass Members.");
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
            flagMap.put(flag.getRxClass(), flag);
        }

        Map<String, MedicationFlag> map = new LinkedHashMap<>();

        for (String rxClass : rxClassService.getRxClassSet(rxCuis)) {
            if (flagMap.containsKey(rxClass)) {
                map.put(rxClass, flagMap.get(rxClass));
            }
        }

        return new ArrayList<>(map.values());
    }
}
