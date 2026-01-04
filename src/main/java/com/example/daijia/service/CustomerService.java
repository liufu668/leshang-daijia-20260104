package com.example.daijia.service;

import com.example.daijia.model.Customer;
import org.springframework.stereotype.Service;

@Service
public interface CustomerService {

    Customer loadUserByWxOpenId(String wxOpenId);
}
