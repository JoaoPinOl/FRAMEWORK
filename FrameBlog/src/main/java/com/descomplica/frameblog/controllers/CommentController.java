package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.Comment;
import com.descomplica.frameblog.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping(path = "/saves")
    private @ResponseBody Comment save(@RequestBody final Comment comment){
        return commentService.save(comment);
    }
}
