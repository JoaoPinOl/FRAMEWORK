package com.descomplica.frameblog.controllers.V2;

import com.descomplica.frameblog.models.V2.AddressV2;
import com.descomplica.frameblog.services.v2.AddressServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/v2/addresses")
public class AddressControllerV2 {

    @Autowired
    private AddressServiceV2 addressServiceV2;

     //Endpoints de CRUD
    @PostMapping(path = "/save")
    private @ResponseBody  AddressV2 save(@RequestBody AddressV2 addressV2){
        return addressServiceV2.save(addressV2);
    }

     //Traz todas as entidades do banco
    @GetMapping(path = "/getAll")
    private @ResponseBody List<AddressV2> getAll(){
        return addressServiceV2.getAll();
    }

    //Trazer uma entidade específica do banco
    @GetMapping(path = "/get")
    private @ResponseBody AddressV2 get(@RequestParam final Long id){
        return addressServiceV2.get(id);
    }

    //Atualizar o registro de uma entidade específica do banco
    @PostMapping(path = "/update")
    private @ResponseBody AddressV2 update(@RequestParam final Long id, @RequestBody final AddressV2 addressV2){
        return addressServiceV2.update(id, addressV2);
    }

    //Deletar o registro de uma entidade específica do banco
    @DeleteMapping(path = "/delete")
    private void delete(@RequestParam final Long id){
        addressServiceV2.delete(id);
    }
}
