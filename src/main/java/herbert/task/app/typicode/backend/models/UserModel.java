/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;
import jakarta.persistence.*;
import java.util.UUID;

/**
 *
 * @author HerbertSekpey
 */
@Entity
@Table(name = "USER_LIST")
public class UserModel extends BaseModel{
    
    @Column(name ="REFERENCE_ID")
    private String referenceId;
    
    @Column(name ="EMAIL", unique=true, nullable=false)
    private String email;
    
    @Column(name = "EMAIL_VERIFIED")
    private boolean emailVerified;
    
    @Column(name ="AVATAR")
    private String avatar;
    
    
    @OneToOne(mappedBy="user", fetch=FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
    private OTPModel otp;
    
    @OneToOne(mappedBy="user", fetch=FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
    private RefreshToken tokenModel;
    
    
    @Enumerated(EnumType.STRING)
    @Column(name="ROLES")
    private UserRole userRole;
                 
    public enum UserRole{
        ADMIN,
        ENDUSER,
    };
    
    
    @PrePersist
    public void init2(){
        String myUUID = UUID.randomUUID().toString();
        setReferenceId("USER" + myUUID.substring(8,myUUID.length()-13));
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public OTPModel getOtp() {
        return otp;
    }

    public void setOtp(OTPModel otp) {
        this.otp = otp;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public RefreshToken getTokenModel() {
        return tokenModel;
    }

    public void setTokenModel(RefreshToken tokenModel) {
        this.tokenModel = tokenModel;
    }
    
    
    
}
