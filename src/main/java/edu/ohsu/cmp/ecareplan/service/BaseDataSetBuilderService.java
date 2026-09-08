package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import edu.ohsu.cmp.ecareplan.model.EndpointProviderType;
import edu.ohsu.cmp.ecareplan.transform.GenericResourceTransformer;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseDataSetBuilderService extends BaseService {
    // fallbacks so a context can never land on HAPI's defaults (10s socket timeout, 20 connections)
    public static final int DEFAULT_SOCKET_TIMEOUT = 5 * 60 * 1000;
    public static final int DEFAULT_POOL_MAX_TOTAL = 200;
    public static final int DEFAULT_POOL_MAX_PER_ROUTE = 100;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 60 * 1000;

    @Value("${socket.timeout:" + DEFAULT_SOCKET_TIMEOUT + "}")
    private int socketTimeout;

    @Value("${fhir.pool.maxTotal:" + DEFAULT_POOL_MAX_TOTAL + "}")
    private int poolMaxTotal;

    @Value("${fhir.pool.maxPerRoute:" + DEFAULT_POOL_MAX_PER_ROUTE + "}")
    private int poolMaxPerRoute;

    @Value("${fhir.pool.connectionRequestTimeout:" + DEFAULT_CONNECTION_REQUEST_TIMEOUT + "}")
    private int connectionRequestTimeout;

    @Autowired
    private ResourceCategorizationService resourceCategorizationService;

    protected FhirContext fhirContext;

    protected abstract ServerValidationModeEnum getServerValidationMode();

    @PostConstruct
    private void initFhirContext() {
        // initialize fhirContext in @PostConstruct to ensure that injected @Values are populated
        FhirContext ctx = FhirContext.forR4();
        ctx.getRestfulClientFactory().setServerValidationMode(getServerValidationMode());
        configureClientFactory(ctx, socketTimeout, poolMaxTotal, poolMaxPerRoute, connectionRequestTimeout);
        fhirContext = ctx;
    }

    public ResourceTransformer getResourceTransformer(EndpointProviderType endpointProviderType) {
        // todo : this needs to return an appropriate transformer based on the endpoint provider type
        //        for now, just return GenericResourceTransformer

        return new GenericResourceTransformer(resourceCategorizationService);
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    private void configureClientFactory(FhirContext ctx, int socketTimeout, int poolMaxTotal,
                                        int poolMaxPerRoute, int connectionRequestTimeout) {
        var factory = ctx.getRestfulClientFactory();
        factory.setSocketTimeout(socketTimeout);
        factory.setPoolMaxTotal(poolMaxTotal);
        factory.setPoolMaxPerRoute(poolMaxPerRoute);

        // an exhausted pool otherwise fails the lease after 10s, and callers treat that as a dead
        // server rather than backpressure.  waiting is cheap on a virtual thread, so wait instead
        factory.setConnectionRequestTimeout(connectionRequestTimeout);
    }
}
