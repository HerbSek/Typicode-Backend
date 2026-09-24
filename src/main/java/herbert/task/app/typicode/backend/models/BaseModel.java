/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.util.UUID;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author HerbertSekpey
 */

@MappedSuperclass
public abstract class BaseModel implements Serializable{
    
   @Id
   @Column(name = "ID")
   private String id;
   
   @Column(name ="DATE_CREATED")
   private LocalDateTime dateCreated;
   
   @Column(name ="DATE_UPDATED")
   private LocalDateTime dateUpdated;
   
   
   
  @PrePersist
  public void init(){
      setId(UUID.randomUUID().toString().replace("-", ""));
      setDateCreated(LocalDateTime.now());
      setDateUpdated(LocalDateTime.now());
  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
    
    
    
    
}
