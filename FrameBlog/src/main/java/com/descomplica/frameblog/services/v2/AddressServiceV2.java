package com.descomplica.frameblog.services.v2;

import com.descomplica.frameblog.models.V2.AddressV2;

import java.util.List;

public interface AddressServiceV2 {

        AddressV2 save(final AddressV2 address);

        List<AddressV2> getAll();

        AddressV2 get(Long id);

        AddressV2 update(Long id, AddressV2 address);

        void delete(Long id);
}
