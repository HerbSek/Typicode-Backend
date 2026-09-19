/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.utils;

import java.security.SecureRandom;

/**
 *
 * @author HerbertSekpey
 */
public class OTPGenerator {
    
    
    public static int generate(){
        SecureRandom random = new SecureRandom();
        int genNum =100000 + random.nextInt(1, 900000);
        return genNum;
    }
    
}
