package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;

public record DataSetBuilderRequestConfiguration(UserEndpoint userEndpoint,
                                                 FHIRCredentials credentials,
                                                 String endpointPatientId) {
}
