/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;

import jakarta.persistence.*;
import jakarta.persistence.Table;

/**
 *
 * @author HerbertSekpey
 */

@Entity
@Table(name = "BLOG_IMAGES")
public class BlogImagesModel extends BaseModel{
    
    @Column(name = "IMAGE")
    private String image;
    
//    @ManyToOne
//    @JoinColumn(name = "USER_ID")
//    private UserModel user;
    
    @ManyToOne
    @JoinColumn(name = "BLOG_ID")
    private BlogModel blog;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BlogModel getBlog() {
        return blog;
    }

    public void setBlog(BlogModel blog) {
        this.blog = blog;
    }

    
    
}
