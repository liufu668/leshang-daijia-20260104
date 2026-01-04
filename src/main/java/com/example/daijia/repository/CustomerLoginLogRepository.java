package com.example.daijia.repository;

import com.example.daijia.model.CustomerLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerLoginLogRepository extends JpaRepository<CustomerLoginLog, Long> {
}
