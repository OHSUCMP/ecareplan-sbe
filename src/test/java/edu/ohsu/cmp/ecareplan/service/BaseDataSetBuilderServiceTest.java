package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class BaseDataSetBuilderServiceTest {

    @Test
    void endpointServiceValidatesServersAndSdsServiceDoesNot() {
        assertEquals(ServerValidationModeEnum.ONCE, validationModeOf(initialized(new EndpointService())));
        assertEquals(ServerValidationModeEnum.NEVER, validationModeOf(initialized(new SDSService())));
    }

    @Test
    void configuredSettingsReachEachServicesClientFactory() {
        for (BaseDataSetBuilderService service : List.of(new EndpointService(), new SDSService())) {
            IRestfulClientFactory factory = initialized(service).getFhirContext().getRestfulClientFactory();

            assertEquals(1234, factory.getSocketTimeout());
            assertEquals(321, factory.getPoolMaxTotal());
            assertEquals(123, factory.getPoolMaxPerRoute());
            assertEquals(4321, factory.getConnectionRequestTimeout());
        }
    }

    /**
     * Regression test for settings being read before Spring injected them.  Building the context in
     * the constructor left every value at 0, which HAPI stores without complaint and Apache then
     * rejects when it lazily builds the pool - so the first FHIR request of the process died with
     * "Max value may not be negative or zero".  getNativeHttpClient() is what triggers that build,
     * and it performs no network I/O, so it reproduces the failure directly.
     */
    @Test
    void nativeHttpClientCanBeBuiltFromEachServicesContext() {
        for (BaseDataSetBuilderService service : List.of(new EndpointService(), new SDSService())) {
            IRestfulClientFactory factory = initialized(service).getFhirContext().getRestfulClientFactory();

            assertTrue(factory.getPoolMaxTotal() > 0, "pool max total must be positive");
            assertTrue(factory.getPoolMaxPerRoute() > 0, "pool max per route must be positive");
            assertTrue(factory.getSocketTimeout() > 0, "a socket timeout of 0 means never time out");
            assertTrue(factory.getConnectionRequestTimeout() > 0, "a request timeout of 0 means wait forever");

            assertDoesNotThrow(() -> ((ApacheRestfulClientFactory) factory).getNativeHttpClient());
        }
    }

    /** Stands in for Spring: inject the @Value fields, then run the @PostConstruct initializer. */
    private static <T extends BaseDataSetBuilderService> T initialized(T service) {
        ReflectionTestUtils.setField(service, "socketTimeout", 1234);
        ReflectionTestUtils.setField(service, "poolMaxTotal", 321);
        ReflectionTestUtils.setField(service, "poolMaxPerRoute", 123);
        ReflectionTestUtils.setField(service, "connectionRequestTimeout", 4321);
        ReflectionTestUtils.invokeMethod(service, "initFhirContext");
        return service;
    }

    private static ServerValidationModeEnum validationModeOf(BaseDataSetBuilderService service) {
        return service.getFhirContext().getRestfulClientFactory().getServerValidationMode();
    }
}
