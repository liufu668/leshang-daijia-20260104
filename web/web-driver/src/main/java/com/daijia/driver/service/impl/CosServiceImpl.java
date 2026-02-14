package com.daijia.driver.service.impl;

import com.daijia.common.result.Result;
import com.daijia.driver.client.CosFeignClient;
import com.daijia.driver.service.CosService;
import com.daijia.model.vo.driver.CosUploadVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CosServiceImpl implements CosService {

    private final CosFeignClient cosFeignClient;

    //文件上传接口
    @Override
    public CosUploadVo uploadFile(MultipartFile file, String path) {
        //远程调用
        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.upload(file,path);
        CosUploadVo cosUploadVo = cosUploadVoResult.getData();
        return cosUploadVo;
    }
}
