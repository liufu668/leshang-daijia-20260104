package com.daijia.driver.service;

import com.daijia.model.vo.order.TextAuditingVo;

public interface CiService {

    Boolean imageAuditing(String path);

    TextAuditingVo textAuditing(String content);
}
