package com.market.module.user.controller;

import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.user.entity.Address;
import com.market.module.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.listByUserId(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Address> getById(@PathVariable Long id) {
        return Result.success(addressService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Address address) {
        address.setUserId(SecurityUtil.getCurrentUserId());
        addressService.save(address);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Address address) {
        address.setId(id);
        addressService.update(address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id, SecurityUtil.getCurrentUserId());
        return Result.success();
    }
}
