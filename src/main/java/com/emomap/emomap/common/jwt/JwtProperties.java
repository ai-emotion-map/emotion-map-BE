package com.emomap.emomap.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperties {
    private String secretKey;
    private long expirationTime;         // access 만료(ms)
    private long refreshExpirationTime;  // refresh 만료(ms)

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public long getExpirationTime() { return expirationTime; }
    public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
    public long getRefreshExpirationTime() { return refreshExpirationTime; }
    public void setRefreshExpirationTime(long refreshExpirationTime) { this.refreshExpirationTime = refreshExpirationTime; }
}
