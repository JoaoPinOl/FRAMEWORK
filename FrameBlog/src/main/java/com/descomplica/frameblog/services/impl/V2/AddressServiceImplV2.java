package com.descomplica.frameblog.services.impl.V2;

import com.descomplica.frameblog.models.V2.AddressV2;
import com.descomplica.frameblog.models.V2.UserV2;
import com.descomplica.frameblog.repository.V2.AddressRepositoryV2;
import com.descomplica.frameblog.repository.V2.UserRepositoryV2;
import com.descomplica.frameblog.services.v2.AddressServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AddressServiceImplV2 implements AddressServiceV2 {

    @Autowired
    private AddressRepositoryV2 addressRepositoryV2;

    @Autowired
    private UserRepositoryV2 userRepositoryV2;

    @Override
    public AddressV2 save(final AddressV2 address) {
        UserV2 userV2 = userRepositoryV2.findById(address.getUserV2().getUserId()).orElseThrow(() -> new RuntimeException("User not found"));

        return addressRepositoryV2.save(address);
    }

    @Override
    public List<AddressV2> getAll() {
        return addressRepositoryV2.findAll();
    }

    @Override
    public AddressV2 get(Long id) {
        return addressRepositoryV2.findById(id).orElseThrow(() -> new RuntimeException("Address not found"));
    }

    @Override
    public AddressV2 update(Long id, AddressV2 address) {
        AddressV2 addressUpdate = addressRepositoryV2.findById(id).orElseThrow(() -> new RuntimeException("Address not found"));

        if(Objects.nonNull(addressUpdate)){
            addressUpdate.setStreet(address.getStreet());
            addressUpdate.setCity(address.getCity());
            addressUpdate.setState(address.getState());
            addressUpdate.setZipCode(address.getZipCode());
            return addressRepositoryV2.save(addressUpdate);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        AddressV2 addressDelete = addressRepositoryV2.findById(id).orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepositoryV2.deleteById(id);
    }
}
