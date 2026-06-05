package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import edu.ohsu.cmp.ecareplan.repository.DefaultQueryRepository;
import edu.ohsu.cmp.ecareplan.repository.EndpointQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService extends BaseService {
    @Autowired
    private DefaultQueryRepository defaultQueryRepository;

    @Autowired
    private EndpointQueryRepository endpointQueryRepository;

    public List<QueryModel> getDataSetQueriesForEndpoint(DataSetName dataSetName, Endpoint endpoint) {
        List<QueryModel> list = new ArrayList<>();

        for (EndpointQuery endpointQuery : endpointQueryRepository.findByEndpointIdAndDataSetName(endpoint.getId(), dataSetName)) {
            list.add(new QueryModel(endpointQuery));
        }

        if (list.isEmpty()) {
            for (DefaultQuery query : defaultQueryRepository.findByDataSetName(dataSetName)) {
                list.add(new QueryModel(query));
            }
        }

        return list;
    }
}
