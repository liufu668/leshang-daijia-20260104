package com.daijia.model.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateWxPhoneVo {

    @Schema(description = "客户ID")
    private Long customerId;

    private String code;
}
