package com.daijia.controller;

import com.daijia.service.SysLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.daijia.common.result.Result;


@Slf4j
@RestController
@RequestMapping(value = "/sysLogin")
@RequiredArgsConstructor
public class SysLoginController {

    private final SysLoginService sysLoginService;

    @GetMapping("/getDriverIdByWxOpenId/{wxOpenId}")
    public Result<Long> getDriverIdByWxOpenId(@PathVariable String wxOpenId) {
        return Result.ok(sysLoginService.getDriverIdByWxOpenId(wxOpenId));
    }

    @GetMapping("/getCustomerIdByWxOpenId/{wxOpenId}")
    public Result<Long> getCustomerIdByWxOpenId(@PathVariable String wxOpenId) {
        return Result.ok(sysLoginService.getCustomerIdByWxOpenId(wxOpenId));
    }
}
