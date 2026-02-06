package com.daijia.driver.service;

import com.daijia.model.vo.driver.CosUploadVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {
    //文件上传接口
    CosUploadVo uploadFile(MultipartFile file, String path);
}
