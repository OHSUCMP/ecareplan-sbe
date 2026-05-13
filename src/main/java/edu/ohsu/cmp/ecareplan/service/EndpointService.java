package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.repository.EndpointRepository;
import edu.ohsu.cmp.ecareplan.repository.UserEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EndpointService {
    private static final Logger logger = LoggerFactory.getLogger(EndpointService.class);

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private UserEndpointRepository userEndpointRepository;

    public List<EndpointModel> getAllEndpoints() {
        List<EndpointModel> list = new ArrayList<>();
        for (Endpoint endpoint : endpointRepository.findAll(Sort.by("name").ascending())) {
            list.add(new EndpointModel(endpoint));
        }
        return list;
    }

    public List<EndpointModel> getEndpointsForUser(Long userId) {
        List<EndpointModel> list = new ArrayList<>();
        for (UserEndpoint ue : userEndpointRepository.findByUserId(userId)) {
            list.add(new EndpointModel(ue.getEndpoint()));
        }
        return list;
    }

    public void addEndpointForUser(Long userId, Long endpointId) {
        if (userEndpointRepository.findByUserIdAndEndpointId(userId, endpointId).isPresent()) {
            logger.warn("UserEndpoint already exists for user {} and endpoint {}", userId, endpointId);
            return;
        }

        UserEndpoint ue = new UserEndpoint();
        ue.setUserId(userId);
        ue.setEndpoint(endpointRepository.findById(endpointId).orElseThrow());
        userEndpointRepository.save(ue);
    }

    public void updateUserEndpointLastSync(Long userId, Long endpointId) {
        UserEndpoint ue = userEndpointRepository.findByUserIdAndEndpointId(userId, endpointId)
                .orElseThrow(() -> new IllegalArgumentException("UserEndpoint not found"));

        ue.setLastSync(new Date());
        userEndpointRepository.save(ue);
    }
}
