package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.model.EndpointProviderType;
import edu.ohsu.cmp.ecareplan.transform.GenericResourceTransformer;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseDataSetBuilderService extends BaseService {
    @Autowired
    private ResourceCategorizationService resourceCategorizationService;

    public ResourceTransformer getResourceTransformer(EndpointProviderType endpointProviderType) {
        // todo : this needs to return an appropriate transformer based on the endpoint provider type
        //        for now, just return GenericResourceTransformer

        return new GenericResourceTransformer(resourceCategorizationService);
    }
}
