package com.daijia.model.vo.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CosUploadVo {

    /**
     * COS存储路径（持久化到数据库）
     * 示例：/driver/auth/abc123.jpg
     */
    @Schema(description = "上传路径")
    private String url;

    /**
     * 临时访问URL（仅用于前端回显）
     * 示例：https://driver-xxx.cos.ap-guangzhou.myqcloud.com/...?sign=xxx
     */
    @Schema(description = "回显地址")
    private String showUrl;
}
