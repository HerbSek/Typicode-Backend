/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package herbert.task.app.typicode.backend.utils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import herbert.task.app.typicode.backend.DTO.UserDTO;
import jakarta.ejb.Stateless;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;


/**
 *
 * @author HerbertSekpey
 */


@Stateless
public class JWTUtil {
    
    
    private String ISSUER;
    private int timer;
    private final Duration TTL;
    private String secret;   
    private Algorithm algorithm;
    private JWTVerifier verifier;
    
    
    
    public JWTUtil() {
        
        Config config = ConfigProvider.getConfig();
        this.ISSUER = config.getValue("jwt.secret.issuer", String.class);
        Integer ttl = config.getValue("jwt.secret.access.ttl", Integer.class);
        this.TTL = Duration.ofMinutes(ttl);
        this.secret = config.getValue("jwt.secret.key", String.class);
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build(); 
        
    }
       
    
    public String generateLoginToken(UserDTO user){       
        Instant now = Instant.now();        
        return JWT.create()
                  .withIssuer(ISSUER)
                  .withSubject(user.getId())
                  .withClaim("email", user.getEmail())
                  .withClaim("reference", String.valueOf(user.getReference()))
                  .withClaim("avatar", user.getAvatar())
                  .withClaim("role", String.valueOf(user.getRole()))
                  .withIssuedAt(Date.from(now))
                  .withExpiresAt(Date.from(now.plus(TTL)))
                  .sign(algorithm);
    }
    
    
    public DecodedJWT verify(String token) throws JWTVerificationException {
      return verifier.verify(token);
    }
    
    
    
}
