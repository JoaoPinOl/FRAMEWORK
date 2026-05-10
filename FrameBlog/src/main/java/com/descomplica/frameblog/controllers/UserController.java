package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.models.V2.UserV2;
import com.descomplica.frameblog.services.UserService;
import com.descomplica.frameblog.services.v2.UserServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = "/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceV2 userServiceV2;

    @PostMapping("/saves")
    public @ResponseBody User  saveUser(@RequestBody User user){
        return userService.save(user);
    }

    //Traz todas as entidades do banco
    //@Cacheable("users")
    @GetMapping(path = "/getAll")
    public @ResponseBody List<User> getAll(){
        return userService.getAll();
    }

    //Versionamento por parâmetro de URI
    //Versionamento por parâmetro de header
    // Versionamento por parâmetro de URI
    // e via parâmetro no cabeçalho
    @GetMapping(path = "/get")
    public @ResponseBody ResponseEntity<Object> get(@RequestParam final Long id, @RequestParam final String uriVersion,
                                                     @RequestHeader(name = "Accept-Version") final String acceptVersion) {

        if (uriVersion.equals("v2") || acceptVersion.equals("v2")){
            return ResponseEntity.ok(userServiceV2.get(id));
        }
        return ResponseEntity.ok(userService.get(id));
    }

//    @GetMapping(path = "/get")
//    private @ResponseBody User get(@RequestParam final Long id){
//        return userService.get(id);
//    }

    @PostMapping(path = "/update")
    public @ResponseBody User update(@RequestParam final Long id, @RequestBody final User user){
        return userService.update(id, user);
    }

    @DeleteMapping(path = "/delete")
    public ResponseEntity<?>  delete(@RequestParam final Long id){
        userService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/")
    public @ResponseBody String authentication(){
        return "Hello World";
    }
}
