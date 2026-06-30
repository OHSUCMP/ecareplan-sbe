package edu.ohsu.cmp.ecareplan.model;

public class RefreshTokenData {
    private String refreshToken;

    public RefreshTokenData() {}

    public RefreshTokenData(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
