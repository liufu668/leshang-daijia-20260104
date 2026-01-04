package com.example.daijia.service.impl;

import com.example.daijia.model.Customer;
import com.example.daijia.repository.CustomerRepository;
import com.example.daijia.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    CustomerRepository customerRepository;

    // 根据openid到数据库查找用户
    @Override
    public Customer loadUserByWxOpenId(String wxOpenId){
        Customer customer = customerRepository.findByWxOpenId(wxOpenId);
        if(customer == null){
            // 创建新用户
            customer = new Customer();
            customer.setWxOpenId(wxOpenId);
            customer.setCreateTime(LocalDateTime.now());
            customer.setUpdateTime(LocalDateTime.now());
            customer.setGender("1");
            customer.setStatus(1);
            customer.setIsDeleted(0);

            // 保存用户数据到数据库
            customerRepository.save(customer);
        }
        return customer;
    }
}
