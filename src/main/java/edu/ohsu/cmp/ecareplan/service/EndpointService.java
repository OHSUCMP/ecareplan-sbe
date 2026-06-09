package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.repository.EndpointRepository;
import edu.ohsu.cmp.ecareplan.repository.UserEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EndpointService {
    private static final Logger logger = LoggerFactory.getLogger(EndpointService.class);

    @Value("${endpoint.patientLaunch.name}")
    private String patientEndpointName;

    @Value("${endpoint.careTeamLaunch.name}")
    private String careTeamEndpointName;

    @Value("${endpoint.sds.name}")
    private String sdsEndpointName;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private UserEndpointRepository userEndpointRepository;

    public Endpoint getPatientLaunchEndpoint() {
        return endpointRepository.findOneByName(patientEndpointName);
    }

    public Endpoint getCareTeamLaunchEndpoint() {
        return endpointRepository.findOneByName(careTeamEndpointName);
    }

    public Endpoint getSDSEndpoint() { return endpointRepository.findOneByName(sdsEndpointName); }

    public List<EndpointModel> getAllThirdPartyEndpoints() {
        List<EndpointModel> list = new ArrayList<>();
        for (Endpoint endpoint : endpointRepository.findAll(Sort.by("name").ascending())) {
            if (endpoint.getName().equals(patientEndpointName) ||
                    endpoint.getName().equals(careTeamEndpointName) ||
                    endpoint.getName().equals(sdsEndpointName)) {
                continue;
            }
            list.add(new EndpointModel(endpoint));
        }
        return list;
    }

    public UserEndpoint getUserEndpoint(Long userId, Long endpointId) {
        UserEndpoint ue = userEndpointRepository.findByUserIdAndEndpointId(userId, endpointId).orElse(null);
        if (ue == null) {
            ue = new UserEndpoint();
            ue.setUserId(userId);
            ue.setEndpoint(endpointRepository.findById(endpointId).orElseThrow());
            userEndpointRepository.save(ue);
        }
        return ue;
    }

    public void updateUserEndpointLastSync(Long userId, Long endpointId) {
        UserEndpoint ue = getUserEndpoint(userId, endpointId);
        ue.setLastSync(new Date());
        userEndpointRepository.save(ue);
    }
}
