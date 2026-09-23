/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.services;

import herbert.task.app.typicode.backend.models.BlogModel;
import herbert.task.app.typicode.backend.models.OTPModel;
import herbert.task.app.typicode.backend.models.RefreshToken;
import herbert.task.app.typicode.backend.models.UserModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

/**
 *
 * @author HerbertSekpey
 */
@ApplicationScoped
@Transactional
public class PersistenceService {
    
    @PersistenceContext
    EntityManager em;
    
   
    public void createUser(UserModel entity){
      em.persist(entity);
    }
    
    public boolean OTPExists(Integer OTP){
        Long otp = em.createQuery("SELECT COUNT(O) FROM OTPModel O WHERE O.password = :OTP", Long.class)
                .setParameter("OTP", OTP)
                .getSingleResult();
        return otp > 0;
    }
    
    public boolean OTPAndEmailExistsAndExpired(Integer OTP, String email){
        Long otp = em.createQuery("SELECT COUNT(O) FROM OTPModel O WHERE O.password = :OTP AND O.user.email = :email AND O.expiryTime < :now", Long.class)
                .setParameter("OTP", OTP)
                .setParameter("email", email)
                .setParameter("now", OffsetDateTime.now())
                .getSingleResult();
        return otp > 0;
    }
    
    public UserModel CheckUserAndOtp(Integer OTP, String email){
        try{
        UserModel myUser = em.createQuery("SELECT O FROM UserModel O WHERE O.email = :email",UserModel.class)
                             .setParameter("email", email)
                             .getSingleResult();
        
        if(myUser == null){
            return null;
        } 
        
        return myUser;
        }
    
        catch(Exception e){
          e.printStackTrace();
        return null;
        }
    }
        
        
        public UserModel CheckUser(String email){
        try{
        UserModel myUser = em.createQuery("SELECT O FROM UserModel O WHERE O.email = :email",UserModel.class)
                             .setParameter("email", email)
                             .getSingleResult();
        return myUser;
        }
    
        catch(Exception e){
          e.printStackTrace();
        return null;
        }
    }
        
        public UserModel updateUser(UserModel user){
         return em.merge(user);
        }
        
        public UserModel findUser(String id){
            return em.find(UserModel.class, id);
        }
    
        public void deleteUser(UserModel user){
            em.remove(user);
        }
    
        
        public OTPModel updateOTP(OTPModel otp){
            OTPModel otp1 = em.merge(otp);
            return otp1;
        }
        
        public void deleteOTP(OTPModel otp){
            em.remove(otp);
            em.flush();
        }
    
        
        public RefreshToken findRefreshToken(String token){
            try{
            RefreshToken myToken = em.createQuery("SELECT O from RefreshToken O WHERE O.tokenHash = :token",RefreshToken.class)
                     .setParameter("token", token)
                     .getSingleResult();
             return myToken;
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }
        
    
        
        ////////////////////////////BLOG////////////////////////////////////
    /// @param blog/
       
        
      public void createBlog(BlogModel blog){
          em.persist(blog);
      }
      
      public BlogModel findBlog(String id){
          return em.find(BlogModel.class, id);
      }  
        
      public BlogModel updateBlog(BlogModel updateBlog){
          return em.merge(updateBlog);
      }  
        
      public List<BlogModel> findBlogsByUserId(String id){
          List<BlogModel> blogs = em.createQuery("SELECT B from BlogModel B WHERE B.user.id = :id",BlogModel.class)
                                    .setParameter("id", id)
                                    .getResultList();
          return blogs;
      }
        
        
        
}
