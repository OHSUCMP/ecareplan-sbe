package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;

public class EndpointModel {
    private Long id;
    private String name;
    private String iss;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private EndpointProviderType providerType;

    public EndpointModel(Endpoint endpoint) {
        this.id = endpoint.getId();
        this.name = endpoint.getName();
        this.iss = endpoint.getIss();
        this.clientId = endpoint.getClientId();
        this.clientSecret = endpoint.getClientSecret();
        this.redirectUri = endpoint.getRedirectUri();
        this.scope = endpoint.getScope();
        this.providerType = endpoint.getProviderType();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIss() {
        return iss;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public EndpointProviderType getProviderType() {
        return providerType;
    }
}
