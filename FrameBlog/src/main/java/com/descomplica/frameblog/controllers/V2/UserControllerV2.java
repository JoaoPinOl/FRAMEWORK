package com.descomplica.frameblog.controllers.V2;

import com.descomplica.frameblog.models.V2.UserV2;
import com.descomplica.frameblog.services.v2.UserServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/v2/users")
public class UserControllerV2 {

    @Autowired
    private UserServiceV2 userServiceV2;

    @PostMapping("/saves")
    private @ResponseBody UserV2 saveUser(@RequestBody UserV2 userV2){
        return userServiceV2.save(userV2);
    }

    //Traz todas as entidades do banco
    @GetMapping(path = "/getAll")
    private @ResponseBody List<UserV2> getAll(){
        return userServiceV2.getAll();
    }

    @GetMapping(path = "/get")
    private @ResponseBody UserV2 get(@RequestParam final Long id){
        return userServiceV2.get(id);
    }

    @PostMapping(path = "/update")
    private @ResponseBody UserV2 update(@RequestParam final Long id, @RequestBody final UserV2 userV2){
        return userServiceV2.update(id, userV2);
    }

    @DeleteMapping(path = "/delete")
    private ResponseEntity<?>  delete(@RequestParam final Long id){
        userServiceV2.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/")
    public @ResponseBody String authentication(){
        return "Hello World";
    }
}
