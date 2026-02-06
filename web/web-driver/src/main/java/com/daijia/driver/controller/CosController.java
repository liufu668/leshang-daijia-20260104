package com.daijia.driver.controller;

import com.daijia.common.result.Result;
import com.daijia.driver.service.CosService;
import com.daijia.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value = "/cos")
@RequiredArgsConstructor
public class CosController {
    private final CosService cosService;

    //文件上传接口
    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file,
                                        @RequestParam(name = "path", defaultValue = "auth") String path) {
        CosUploadVo cosUploadVo = cosService.uploadFile(file,path);
        return Result.ok(cosUploadVo);
    }
}
