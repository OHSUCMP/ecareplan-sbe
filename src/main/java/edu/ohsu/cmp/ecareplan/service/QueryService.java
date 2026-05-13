package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.repository.DefaultQueryRepository;
import edu.ohsu.cmp.ecareplan.repository.EndpointQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QueryService extends BaseService {
    @Autowired
    private DefaultQueryRepository defaultQueryRepository;

    @Autowired
    private EndpointQueryRepository endpointQueryRepository;

    public Collection<QueryModel> getQueriesForEndpoint(Long endpointId) {
        Map<String, QueryModel> map = new LinkedHashMap<>();
        for (DefaultQuery query : defaultQueryRepository.findAll()) {
            QueryModel qm = new QueryModel(query);
            map.put(qm.getDataSetName(), qm);
        }

        // endpoint queries will overwrite defaults if any are specified
        for (EndpointQuery endpointQuery : endpointQueryRepository.findByEndpointId(endpointId)) {
            QueryModel qm = new QueryModel(endpointQuery);
            map.put(qm.getDataSetName(), qm);
        }
        return map.values();
    }
}
