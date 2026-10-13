package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;

public class EndpointModel {
    private final Long id;
    private final String name;
    private final String iss;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final String pkceMode;
    private final EndpointProviderType providerType;

    public EndpointModel(Endpoint endpoint) {
        this.id = endpoint.getId();
        this.name = endpoint.getName();
        this.iss = endpoint.getIss();
        this.clientId = endpoint.getClientId();
        this.clientSecret = endpoint.getClientSecret();
        this.redirectUri = endpoint.getRedirectUri();
        this.scope = endpoint.getScope();
        this.pkceMode = endpoint.getPkceMode();
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

    public String getPkceMode() {
        return pkceMode;
    }

    public EndpointProviderType getProviderType() {
        return providerType;
    }
}
