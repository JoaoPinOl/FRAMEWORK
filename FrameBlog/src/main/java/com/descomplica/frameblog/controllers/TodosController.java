package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.clients.TodoServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TodosController {

    @Autowired
    TodoServiceClient  todoServiceClient;

    @GetMapping(path = "/todos")
    public @ResponseBody List<Object> getAllTodos(){
        return List.of(todoServiceClient.getAllTodos());
    }
}
