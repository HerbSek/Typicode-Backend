/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.resources;
import herbert.task.app.typicode.backend.DTO.EmailDto;
import herbert.task.app.typicode.backend.DTO.MessageDTO;
import herbert.task.app.typicode.backend.DTO.OTPDto;
import herbert.task.app.typicode.backend.DTO.UserDTO;
import herbert.task.app.typicode.backend.models.OTPModel;
import herbert.task.app.typicode.backend.models.UserModel;
import herbert.task.app.typicode.backend.services.PersistenceService;
import herbert.task.app.typicode.backend.utils.EmailUtil;
import herbert.task.app.typicode.backend.utils.OTPGenerator;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
/**
 *
 * @author HerbertSekpey
 */


//@Produces()
@Path("/sign")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class SignResource {
    
    
    @Inject
    PersistenceService ps;
    
    
    @POST
    @Path("/user/signin")
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
    @Path("/user/confirmOTP")
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
                message.setMessage("You have exceeded the max number of retries. Try signing in.");
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
        
        
        return Response.status(Response.Status.OK).entity(messageUser).build();
    }
    
    
    
    
}
