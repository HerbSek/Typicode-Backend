/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.DTO;

import jakarta.validation.constraints.NotNull;

/**
 *
 * @author HerbertSekpey
 */
public class OTPDto {
    
    @NotNull
    private String email;
    
    @NotNull
    private Integer password;

    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    
    public Integer getPassword() {
        return password;
    }

    public void setPassword(Integer password) {
        this.password = password;
    }
    
    
    
}
