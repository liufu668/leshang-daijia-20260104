package com.daijia.driver.controller;


import com.daijia.common.result.Result;
import com.daijia.driver.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 1,司机身份认证(上传身份证,驾驶证等)
 * 2,录入代驾车辆信息(上传车辆照片)
 * 3,上传代驾过程的录音文件
 */
@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        String url = fileService.upload(file);
        return Result.ok(url);
    }

}
