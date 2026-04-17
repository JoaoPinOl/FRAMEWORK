package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping(path = "/saves")
    private @ResponseBody Post save(@RequestBody final Post post){
        return postService.save(post);
    }

    @GetMapping(path = "/getAll")
    private @ResponseBody List<Post> getAll(){
        return postService.getAll();
    }

    @GetMapping(path = "/get")
    private @ResponseBody Post get(@RequestParam final Long id){
        return postService.get(id);
    }

    @PostMapping(path = "/update")
    private @ResponseBody Post update(@RequestParam final Long id, @RequestBody final Post post){
        return postService.update(id, post);
    }

    @DeleteMapping(path = "/delete")
    private void delete(@RequestParam final Long id){
        postService.delete(id);
    }
}
