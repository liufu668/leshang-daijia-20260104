package com.atguigu.daijia.service;

import org.springframework.stereotype.Service;

@Service
public interface CustomerInfoService {

    Long login(String code);
}
