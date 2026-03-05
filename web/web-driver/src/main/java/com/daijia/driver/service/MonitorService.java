package com.daijia.driver.service;

import com.daijia.model.form.order.OrderMonitorForm;
import org.springframework.web.multipart.MultipartFile;

public interface MonitorService {

    Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm);
}
