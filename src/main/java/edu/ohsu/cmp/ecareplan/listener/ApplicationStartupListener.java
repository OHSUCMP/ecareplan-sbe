package edu.ohsu.cmp.ecareplan.listener;

import edu.ohsu.cmp.ecareplan.service.MedicationFlagService;
import edu.ohsu.cmp.ecareplan.service.ResourceCategorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private ResourceCategorizationService resourceCategorizationService;

    @Autowired
    private MedicationFlagService medicationFlagService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        resourceCategorizationService.refreshValueSets();
        medicationFlagService.refreshRxClassMembers();
    }
}
