package com.market.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.market.module.user.entity.Address;
import com.market.module.user.mapper.AddressMapper;
import com.market.module.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
    }

    @Override
    public Address getById(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    @Transactional
    public void save(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultByUserId(address.getUserId());
        }
        addressMapper.insert(address);
    }

    @Override
    @Transactional
    public void update(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultByUserId(address.getUserId());
        }
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long id) {
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        clearDefaultByUserId(userId);
        Address address = new Address();
        address.setId(id);
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    private void clearDefaultByUserId(Long userId) {
        Address update = new Address();
        update.setIsDefault(0);
        addressMapper.update(update, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
    }
}
