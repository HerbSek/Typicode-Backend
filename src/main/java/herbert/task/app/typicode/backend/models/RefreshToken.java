/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 * @author HerbertSekpey
 */


@Entity 
@Table(name = "REFRESH_TOKEN")
public class RefreshToken extends BaseModel{
 
    
    @Column(name = "TOKEN_HASH")
    private String tokenHash;

    
    @Column(name = "EXPIRES_AT")
    private OffsetDateTime expiresAt;

    
    @Column(name = "REVOKED")
    private boolean revoked;

    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "USER_ID")
    private UserModel user;      
     
    
    
    @PrePersist
    public void init2(){
        Config config = ConfigProvider.getConfig();
        long days = config.getValue("jwt.secret.refresh.ttl", Long.class);
        setTokenHash(UUID.randomUUID().toString());
        setExpiresAt(OffsetDateTime.now().plusDays(days));
        setRevoked(false);
    }
    
    
    
    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

   
    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }
    
    
    
    
}
