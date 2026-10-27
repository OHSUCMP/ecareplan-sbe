package edu.ohsu.cmp.ecareplan.model.fhir;

public record FHIRCredentials(String clientId, String serverURL, String bearerToken, String patientId, String userId) {

    @Override
    public String toString() {
        return "FHIRCredentials{[REDACTED]}";
    }
}
