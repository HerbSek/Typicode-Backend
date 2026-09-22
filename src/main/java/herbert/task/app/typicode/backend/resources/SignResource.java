/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.resources;
import herbert.task.app.typicode.backend.DTO.EmailDto;
import herbert.task.app.typicode.backend.DTO.JWTDto;
import herbert.task.app.typicode.backend.DTO.MessageDTO;
import herbert.task.app.typicode.backend.DTO.OTPDto;
import herbert.task.app.typicode.backend.DTO.UserDTO;
import herbert.task.app.typicode.backend.annotation.Secured;
import herbert.task.app.typicode.backend.models.OTPModel;
import herbert.task.app.typicode.backend.models.RefreshToken;
import herbert.task.app.typicode.backend.models.UserModel;
import herbert.task.app.typicode.backend.services.PersistenceService;
import herbert.task.app.typicode.backend.utils.EmailUtil;
import herbert.task.app.typicode.backend.utils.JWTUtil;
import herbert.task.app.typicode.backend.utils.OTPGenerator;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
/**
 *
 * @author HerbertSekpey
 */


//@Produces()
@Path("/user")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class SignResource {
    
    
    @Inject
    PersistenceService ps;
    
    private Long TTL;
    
    
    public SignResource(){
       Config config = ConfigProvider.getConfig();
       this.TTL = config.getValue("jwt.secret.refresh.ttl", Long.class);
    }
    
    
    @POST
    @Path("/signin")
    public Response sign(@Valid EmailDto email) throws MessagingException{
        EmailUtil emailUtil = new EmailUtil();
        if(!email.getEmail().contains("@")){
           MessageDTO message = new MessageDTO();
           message.setMessage("This is not a valid email address! Try again"); 
         return Response.status(Response.Status.CONFLICT).entity(message).build();  
        }
        
        UserModel check = ps.CheckUser(email.getEmail());
        if(check == null){
            UserModel newUser = new UserModel(); 
            newUser.setEmail(email.getEmail().trim().toLowerCase());
            OTPModel otp = new OTPModel();
            Integer genOTP = OTPGenerator.generate();
            while(ps.OTPExists(genOTP)== true){
                genOTP = OTPGenerator.generate();
            }
            otp.setPassword(genOTP);
            otp.setUser(newUser);
            newUser.setOtp(otp);
            ps.createUser(newUser);
            
            emailUtil.sendSmtpEmail(email.getEmail(), "Test Signup", "Welcome new user " + genOTP.toString()+ " This link is valid for 5 mins");
            System.out.println("User created");
            MessageDTO message = new MessageDTO();
            message.setMessage("User Created");
         return Response.status(Response.Status.CREATED).entity(message).build();
        }
        if(check.isEmailVerified() == false){
            check.setOtp(null);
            OTPModel otp1 = new OTPModel();
            Integer genOTP = OTPGenerator.generate();
            while(ps.OTPExists(genOTP)== true){
                genOTP = OTPGenerator.generate();
            }
            otp1.setPassword(genOTP);
            otp1.setUser(check);
            check.setOtp(otp1);
            ps.updateUser(check);
            emailUtil.sendSmtpEmail(email.getEmail(), "Test Signup", "Welcome previously unverified user " + genOTP.toString()+ " This link is valid for 5 mins");
            System.out.println("User trying verification Success");
            MessageDTO message = new MessageDTO();
            message.setMessage("User trying to verify. Success");
            return Response.status(Response.Status.ACCEPTED).entity(message).build();
        }
            check.setOtp(null);
            OTPModel otp1 = new OTPModel();
            Integer genOTP = OTPGenerator.generate();
            while(ps.OTPExists(genOTP)== true){
                genOTP = OTPGenerator.generate();
            }
            otp1.setPassword(genOTP);
            otp1.setUser(check);
            check.setOtp(otp1);
            ps.updateUser(check);
            emailUtil.sendSmtpEmail(email.getEmail(), "Test Sign In", "Welcome existing user " + genOTP.toString()+ " This link is valid for 5 mins");
            System.out.println("User signing in");
            MessageDTO message = new MessageDTO();
            message.setMessage("User signing in");
            return Response.status(Response.Status.OK).entity(message).build();
       
    }
   
    
    
    @POST
    @Path("/confirmOTP")
    public Response confirmOTP(@Valid OTPDto info){
     
        //
        UserModel user = ps.CheckUser(info.getEmail());
        if(user == null){
            MessageDTO message = new MessageDTO();
            message.setMessage("Invalid User");
            return Response.status(Response.Status.NOT_FOUND).entity(message).build();
        }
        if(user.getOtp() == null){
           MessageDTO message = new MessageDTO();
            message.setMessage("Retry signing in/ signing up !");
            return Response.status(Response.Status.NOT_FOUND).entity(message).build();  
        }
        System.out.println("From input: "+info.getPassword());
        System.out.println("From DB: "+ user.getOtp().getPassword());
       
        if(!info.getPassword().equals(user.getOtp().getPassword())){
            int count = user.getOtp().getRetries();
            System.out.println(count);
            if(count >= user.getOtp().getMaxRetries()){
                user.setOtp(null);
                ps.updateUser(user);
                MessageDTO message = new MessageDTO();
                message.setMessage("You have exceeded the max number of retries. Type in the correct email and try again! ");
                return Response.status(Response.Status.NOT_FOUND).entity(message).build(); 
            }
            count = count + 1;
            user.getOtp().setRetries(count);
            ps.updateUser(user);
            MessageDTO message = new MessageDTO();
            message.setMessage("Invalid OTP. Try again!");
            return Response.status(Response.Status.NOT_FOUND).entity(message).build();
        }
        
        if(user.getOtp().getExpiryTime().isBefore(OffsetDateTime.now())){
            MessageDTO message = new MessageDTO();
            message.setMessage("OTP has Expired. Please retry!");
            return Response.status(Response.Status.GONE).entity(message).build(); 
        }
        
        user.setOtp(null);
        user.setEmailVerified(true);
        user = ps.updateUser(user);
        
        //Later on use this to create the JWT Claims
        UserDTO messageUser = new UserDTO();
        messageUser.setEmail(user.getEmail());
        messageUser.setEmailVerified(user.isEmailVerified());
        messageUser.setAvatar(user.getAvatar());
        messageUser.setId(user.getId());
        messageUser.setReference(user.getReferenceId());
        messageUser.setRole(UserModel.UserRole.ENDUSER);
        
        JWTUtil tokenMessage = new JWTUtil();
        String token = "Bearer "+ tokenMessage.generateLoginToken(messageUser);
        String refreshToken;
        
        if(user.getTokenModel() != null){
               System.out.println("BEFORE: "+ user.getTokenModel().getTokenHash());
               user.getTokenModel().setDateUpdated(LocalDateTime.now()); 
               user.getTokenModel().setExpiresAt(OffsetDateTime.now().plusDays(TTL));
               user.getTokenModel().setTokenHash(UUID.randomUUID().toString());
               user.getTokenModel().setRevoked(false);
               UserModel updatedUser = ps.updateUser(user);
               user = updatedUser;
               refreshToken = user.getTokenModel().getTokenHash();
               System.out.println("AFTER: "+ refreshToken);
        }
        
        if(user.getTokenModel() == null){
                RefreshToken refresh = new RefreshToken();
                refresh.setUser(user);
                user.setTokenModel(refresh);
                user = ps.updateUser(user);
        }
       
        
        JWTDto jwtMessage = new JWTDto();
        jwtMessage.setMessage("JWT Token for login");
        jwtMessage.setToken(token);
        jwtMessage.setRefreshToken(user.getTokenModel().getTokenHash());
        
        
        return Response.status(Response.Status.OK).entity(jwtMessage).build();
    }
    
    
    @POST
    @Secured
    @Path("/logout")
    public Response logout(@Context ContainerRequestContext context){
        
        String userId = context.getProperty("userId").toString();
         MessageDTO message = new MessageDTO();
        UserModel user = ps.findUser(userId);
        if(user == null){
            message.setMessage("User not found.");
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }
        user.getTokenModel().setRevoked(true);
        ps.updateUser(user);
        message.setMessage("User logged out");
        message.setStatus("200");
        return Response.status(Response.Status.OK).entity(message).build();
    }
    
    
    
    
    @Secured
    @Path("/test-secure")
    @GET
    public Response testSecured(){
        return Response.ok("Secured!!!").build();
    }

    
    
    @Path("/refresh")
    @POST
    public Response refresh(JWTDto jwt){
        // search for refresh token and check if it has expired or it is revoked or it is null then return unauthorized so that user can login in again. 
        RefreshToken getToken = ps.findRefreshToken(jwt.getRefreshToken());
        if(getToken == null || getToken.isRevoked() == true || getToken.getExpiresAt().isBefore(OffsetDateTime.now())){
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token canot be found or it has expired or it has been revoked. Please login!").build();
        }
        
        UserModel user = getToken.getUser();
            user.getTokenModel().setDateUpdated(LocalDateTime.now()); 
            user.getTokenModel().setExpiresAt(OffsetDateTime.now().plusDays(TTL));
            user.getTokenModel().setTokenHash(UUID.randomUUID().toString());
            user.getTokenModel().setRevoked(false);
            
        UserDTO userInfo = new UserDTO();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setReference(user.getReferenceId());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(UserModel.UserRole.ENDUSER);
        UserModel myUser = ps.updateUser(user);
        user = myUser;
        // JWT 
        JWTUtil jwt1 = new JWTUtil();
        String newAccessToken = jwt1.generateLoginToken(userInfo);   
        String newRefreshToken = user.getTokenModel().getTokenHash();

        JWTDto data = new JWTDto();
        data.setMessage("Login in after rotation");
        data.setRefreshToken(newRefreshToken);
        data.setToken(newAccessToken);
       
       return Response.status(Response.Status.OK).entity(data).build();
        
    }
    
    
    
    
    
    
    
    
    public Long getTTL() {
        return TTL;
    }

    public void setTTL(Long TTL) {
        this.TTL = TTL;
    }

      
    
}
