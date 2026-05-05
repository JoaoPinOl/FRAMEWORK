package com.descomplica.frameblog.clients;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TodoServiceClient {
    //Classe de consumo da Fake API

    public Object[] getAllTodos(){
        RestTemplate restTemplate = new RestTemplate();

        String fakeApiUrl =  "https://jsonplaceholder.typicode.com/todos";
        return restTemplate.getForObject(fakeApiUrl, Object[].class);
    }
}
