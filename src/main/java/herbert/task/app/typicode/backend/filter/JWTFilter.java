/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import herbert.task.app.typicode.backend.annotation.Secured;
import herbert.task.app.typicode.backend.utils.JWTUtil;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 *
 * @author HerbertSekpey
 */
@Secured
@Provider
public class JWTFilter implements ContainerRequestFilter{
    
    @Override
    public void filter(ContainerRequestContext context){
        String header = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid Token").build());
            return;
        }
        
        
        try{
          JWTUtil jwtUtil = new JWTUtil();
          String hasBearer = "Bearer ";
          DecodedJWT checkJWT = jwtUtil.verify(header.substring(hasBearer.length()).trim());
          context.setProperty("userId", checkJWT.getSubject());
        }
        catch(JWTVerificationException e){
            e.printStackTrace();
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Token is invalid or expired").build());
        }
        
    }
    
}
