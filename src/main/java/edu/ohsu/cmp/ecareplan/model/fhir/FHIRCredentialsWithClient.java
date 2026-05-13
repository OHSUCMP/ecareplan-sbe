package edu.ohsu.cmp.ecareplan.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.CapabilityStatement;

public class FHIRCredentialsWithClient {
    private FhirCredentials credentials;
    private IGenericClient client;
    private CapabilityStatement metadata = null;

    public FHIRCredentialsWithClient(FhirCredentials credentials, IGenericClient client) {
        this.credentials = credentials;
        this.client = client;
    }

    public FhirCredentials getCredentials() {
        return credentials;
    }

    public IGenericClient getClient() {
        return client;
    }

    public CapabilityStatement getMetadata() {
        if (metadata == null) {
            metadata = client.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();
        }
        return metadata;
    }
}
