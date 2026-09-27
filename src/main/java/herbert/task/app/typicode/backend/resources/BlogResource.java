/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package herbert.task.app.typicode.backend.resources;

import herbert.task.app.typicode.backend.DTO.BlogDTO;
import herbert.task.app.typicode.backend.DTO.BlogResponseDTO;
import herbert.task.app.typicode.backend.annotation.Secured;
import herbert.task.app.typicode.backend.models.BlogImagesModel;
import herbert.task.app.typicode.backend.models.BlogModel;
import herbert.task.app.typicode.backend.models.BlogModel.BlogType;
import herbert.task.app.typicode.backend.models.UserModel;
import herbert.task.app.typicode.backend.services.PersistenceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author HerbertSekpey
 */
@Path("/blog")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class BlogResource {

    @Inject
    PersistenceService ps;


    @Secured
    @POST
    @Path("/create")
    public Response createBlog(@Context ContainerRequestContext cxt, BlogDTO blogData){
      String userId = cxt.getProperty("userId").toString();
      UserModel searchUser = ps.findUser(userId);

      // Next, search and see if any title like that exist.
       boolean titleExists = ps.findBlogTitle(blogData.getTitle());
       if(titleExists == true){
           return Response.status(Response.Status.CONFLICT).entity("Title already exists !!!. Try a new title.").build();
       }

      if(searchUser != null){
        BlogModel blogModel = new BlogModel();
        blogModel.setTitle(blogData.getTitle());
        blogModel.setBlogtype(BlogType.valueOf(blogData.getBlogtype().toUpperCase()));
        blogModel.setHeaderImage(blogData.getHeaderImage());
        blogModel.setParagraph(blogData.getParagraph());

        List<BlogImagesModel> imageArray = new ArrayList<>();

        if(blogData.getBlogImages() != null){
            for (String image : blogData.getBlogImages()){
                BlogImagesModel storeImage = new BlogImagesModel();
                storeImage.setBlog(blogModel);
                storeImage.setImage(image);
                imageArray.add(storeImage);
            }
        }
        blogModel.setBlogImages(imageArray);
        blogModel.setDescription(blogData.getDescription());
        blogModel.setUser(searchUser);
        if(searchUser.getBlog() == null){
            searchUser.setBlog(new ArrayList<>());
        }
        searchUser.getBlog().add(blogModel);
        ps.updateUser(searchUser);


        return Response.status(Response.Status.CREATED).entity("Blog Created").build();
      }
     return Response.status(Response.Status.NOT_FOUND).entity("User Not found").build();
    }


    @GET
    @Path("/all")
    public Response getAllBlogs(@QueryParam("type") String type,
                                @QueryParam("search") String search,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("20") int size){
        try{
            List<BlogModel> blogs;

            if(search != null && !search.trim().isEmpty()){
                blogs = ps.searchBlogs(search.trim());
            } else if(type != null && !type.trim().isEmpty()){
                blogs = ps.findBlogsByType(BlogType.valueOf(type.trim().toUpperCase()));
            } else if(size > 0){
                int offset = Math.max(page, 0) * size;
                blogs = ps.findAllBlogsPaged(offset, size);
            } else {
                blogs = ps.findAllBlogs();
            }

            List<BlogResponseDTO> data = blogs.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return Response.status(Response.Status.OK).entity(data).build();
        }
        catch(IllegalArgumentException e){
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid blogtype. Use AI, MOBILE or WEB.").build();
        }
    }


    @GET
    @Path("/{id}")
    public Response getBlogById(@PathParam("id") String id){
        BlogModel blog = ps.findBlog(id);
        if(blog == null){
            return Response.status(Response.Status.NOT_FOUND).entity("Blog Not found").build();
        }
        return Response.status(Response.Status.OK).entity(toResponse(blog)).build();
    }


    @Secured
    @GET
    @Path("/my-blogs")
    public Response getMyBlogs(@Context ContainerRequestContext cxt){
        String userId = cxt.getProperty("userId").toString();
        UserModel searchUser = ps.findUser(userId);
        if(searchUser == null){
            return Response.status(Response.Status.NOT_FOUND).entity("User Not found").build();
        }
        List<BlogModel> blogs = ps.findBlogsByUserId(userId);
        List<BlogResponseDTO> data = blogs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return Response.status(Response.Status.OK).entity(data).build();
    }


    @Secured
    @PUT
    @Path("/{id}")
    public Response updateBlog(@Context ContainerRequestContext cxt,
                               @PathParam("id") String id,
                               BlogDTO blogData){
        String userId = cxt.getProperty("userId").toString();
        UserModel searchUser = ps.findUser(userId);
        if(searchUser == null){
            return Response.status(Response.Status.NOT_FOUND).entity("User Not found").build();
        }
        BlogModel blog = ps.findBlog(id);
        if(blog == null){
            return Response.status(Response.Status.NOT_FOUND).entity("Blog Not found").build();
        }
        if(blog.getUser() == null || !blog.getUser().getId().equals(userId)){
            return Response.status(Response.Status.FORBIDDEN).entity("You are not the author of this blog.").build();
        }
        if(blogData.getTitle() != null && !blogData.getTitle().equals(blog.getTitle())){
            boolean titleExists = ps.findBlogTitleExcludingId(blogData.getTitle(), id);
            if(titleExists == true){
                return Response.status(Response.Status.CONFLICT).entity("Title already exists !!!. Try a new title.").build();
            }
            blog.setTitle(blogData.getTitle());
        }
        if(blogData.getDescription() != null){
            blog.setDescription(blogData.getDescription());
        }
        if(blogData.getParagraph() != null){
            blog.setParagraph(blogData.getParagraph());
        }
        if(blogData.getHeaderImage() != null){
            blog.setHeaderImage(blogData.getHeaderImage());
        }
        if(blogData.getBlogtype() != null){
            try{
                blog.setBlogtype(BlogType.valueOf(blogData.getBlogtype().toUpperCase()));
            }
            catch(IllegalArgumentException e){
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid blogtype. Use AI, MOBILE or WEB.").build();
            }
        }
        if(blogData.getBlogImages() != null){
            blog.getBlogImages().clear();
            for (String image : blogData.getBlogImages()){
                BlogImagesModel storeImage = new BlogImagesModel();
                storeImage.setBlog(blog);
                storeImage.setImage(image);
                blog.getBlogImages().add(storeImage);
            }
        }
        BlogModel updated = ps.updateBlog(blog);
        return Response.status(Response.Status.OK).entity(toResponse(updated)).build();
    }


    @Secured
    @DELETE
    @Path("/{id}")
    public Response deleteBlog(@Context ContainerRequestContext cxt,
                               @PathParam("id") String id){
        String userId = cxt.getProperty("userId").toString();
        UserModel searchUser = ps.findUser(userId);
        if(searchUser == null){
            return Response.status(Response.Status.NOT_FOUND).entity("User Not found").build();
        }
        BlogModel blog = ps.findBlog(id);
        if(blog == null){
            return Response.status(Response.Status.NOT_FOUND).entity("Blog Not found").build();
        }
        boolean isAuthor = blog.getUser() != null && blog.getUser().getId().equals(userId);
        boolean isAdmin = searchUser.getUserRole() == UserModel.UserRole.ADMIN;
        if(!isAuthor && !isAdmin){
            return Response.status(Response.Status.FORBIDDEN).entity("You are not allowed to delete this blog.").build();
        }
        ps.deleteBlog(blog);
        return Response.status(Response.Status.OK).entity("Blog Deleted").build();
    }


    private BlogResponseDTO toResponse(BlogModel blog){
        BlogResponseDTO dto = new BlogResponseDTO();
        dto.setId(blog.getId());
        dto.setTitle(blog.getTitle());
        dto.setDescription(blog.getDescription());
        dto.setParagraph(blog.getParagraph());
        dto.setHeaderImage(blog.getHeaderImage());
        if(blog.getBlogtype() != null){
            dto.setBlogtype(blog.getBlogtype().name());
        }
        if(blog.getBlogImages() != null){
            List<String> images = blog.getBlogImages().stream()
                    .map(BlogImagesModel::getImage)
                    .collect(Collectors.toList());
            dto.setBlogImages(images);
        }
        if(blog.getUser() != null){
            dto.setAuthorId(blog.getUser().getId());
            dto.setAuthorEmail(blog.getUser().getEmail());
            dto.setAuthorReference(blog.getUser().getReferenceId());
        }
        dto.setDateCreated(blog.getDateCreated());
        dto.setDateUpdated(blog.getDateUpdated());
        return dto;
    }



}
