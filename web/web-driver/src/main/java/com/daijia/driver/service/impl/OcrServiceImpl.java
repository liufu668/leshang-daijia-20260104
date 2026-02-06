package com.daijia.driver.service.impl;

import com.daijia.common.result.Result;
import com.daijia.driver.client.OcrFeignClient;
import com.daijia.driver.service.OcrService;
import com.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.daijia.model.vo.driver.IdCardOcrVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrServiceImpl implements OcrService {

    private final OcrFeignClient ocrFeignClient;

    //身份证识别
    @Override
    public IdCardOcrVo idCardOcr(MultipartFile file) {
        Result<IdCardOcrVo> ocrVoResult = ocrFeignClient.idCardOcr(file);
        IdCardOcrVo idCardOcrVo = ocrVoResult.getData();
        return idCardOcrVo;
    }

    //驾驶证识别
    @Override
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) {
        Result<DriverLicenseOcrVo> driverLicenseOcrVoResult = ocrFeignClient.driverLicenseOcr(file);
        DriverLicenseOcrVo driverLicenseOcrVo = driverLicenseOcrVoResult.getData();
        return driverLicenseOcrVo;
    }
}

