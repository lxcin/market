package com.market.module.user.service;

import com.market.module.user.entity.Address;

import java.util.List;

public interface AddressService {

    List<Address> listByUserId(Long userId);

    Address getById(Long id);

    void save(Address address);

    void update(Address address);

    void delete(Long id);

    void setDefault(Long id, Long userId);
}
