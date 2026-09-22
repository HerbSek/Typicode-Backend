/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.models;

import jakarta.persistence.*;
import java.util.List;

/**
 *
 * @author HerbertSekpey
 */

@Entity
@Table(name = "BLOGS")
public class BlogModel extends BaseModel{
    
    @Column(name = "TITLE")
    private String title;
    
    @Column(name = "Description")
    @Lob
    private String description;
    
    @Column(name = "PARAGRAPH")
    @Lob
    private String paragraph;
    
    @Column(name = "HEADER_IMAGE")
    @Lob
    private String headerImage;
    
    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlogImagesModel> blogImages;
    
    @OneToOne
    @JoinColumn(name = "USER_ID")
    private UserModel user;
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "BLOG_TYPE")
    private BlogType blogtype;
    
    
    public enum BlogType{
     AI,
     MOBILE,
     WEB
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParagraph() {
        return paragraph;
    }

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }

    public String getHeaderImage() {
        return headerImage;
    }

    public void setHeaderImage(String headerImage) {
        this.headerImage = headerImage;
    }

    public List<BlogImagesModel> getBlogImages() {
        return blogImages;
    }

    public void setBlogImages(List<BlogImagesModel> blogImages) {
        this.blogImages = blogImages;
    }

    public BlogType getBlogtype() {
        return blogtype;
    }

    public void setBlogtype(BlogType blogtype) {
        this.blogtype = blogtype;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }
    
    
    
    
}
