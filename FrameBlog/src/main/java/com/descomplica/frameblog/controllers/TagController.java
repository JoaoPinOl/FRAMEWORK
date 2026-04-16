package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.Tag;
import com.descomplica.frameblog.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/tags")
public class TagController {
    @Autowired
    private TagService tagService;

    @PostMapping(path = "/saves")
    private @ResponseBody Tag save(@RequestBody Tag tags){
        return tagService.save(tags);
    }

    @GetMapping(path = "/getAll")
    private @ResponseBody List<Tag> getAll() {
        return tagService.getAll();
    }

    @GetMapping(path = "/get")
    private @ResponseBody Tag getTag(@RequestParam final Long id){
        return tagService.get(id);
    }

    @PostMapping(path = "/update")
    private @ResponseBody Tag update(@RequestParam final Long id, @RequestBody final Tag tag){
        return tagService.update(id, tag);
    }

    @DeleteMapping(path = "/delete")
    private void  delete(@RequestParam final Long id){
        tagService.delete(id);
    }
}
