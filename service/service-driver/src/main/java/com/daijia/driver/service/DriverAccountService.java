package com.daijia.driver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.driver.DriverAccount;
import com.daijia.model.form.driver.TransferForm;

public interface DriverAccountService extends IService<DriverAccount> {

    Boolean transfer(TransferForm transferForm);
}
