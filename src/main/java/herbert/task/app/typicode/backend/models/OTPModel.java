/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 *
 * @author HerbertSekpey
 */
@Entity
@Table(name = "OTP")
public class OTPModel extends BaseModel{
    
    @Column(name ="REFERENCE_ID")
    private String referenceId;
    
    @Column(name ="PASSWORD")
    private Integer password;
    
    @Column(name ="EXPIRY_TIME")
    private OffsetDateTime expiryTime;
    
    @Column(name = "MAX_RETRIES")
    private Integer maxRetries = 3;
    
    @Column(name = "RETRIES")
    private Integer retries;
    
    
    @JoinColumn(name = "USER_ID")
    @OneToOne(fetch = FetchType.LAZY)
    private UserModel user;

    
    @PrePersist
    public void init2(){
        setRetries(0);
        setExpiryTime(OffsetDateTime.now().plusMinutes(5));
        String myUUID = UUID.randomUUID().toString();
        setReferenceId("OTP" + myUUID.substring(8,myUUID.length()-13));
    }
    
    
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    
    public Integer getPassword() {
        return password;
    }

    public void setPassword(Integer password) {
        this.password = password;
    }

    public OffsetDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(OffsetDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }


    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }
    
    
    
}
