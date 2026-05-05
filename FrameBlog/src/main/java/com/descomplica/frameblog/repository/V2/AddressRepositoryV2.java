package com.descomplica.frameblog.repository.V2;

import com.descomplica.frameblog.models.V2.AddressV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepositoryV2 extends JpaRepository<AddressV2, Long> {

}
