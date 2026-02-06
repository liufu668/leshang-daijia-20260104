package com.daijia.controller;

import com.daijia.common.result.Result;
import com.daijia.model.vo.driver.CosUploadVo;
import com.daijia.service.CosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value="/cos")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosController {

    private final CosService cosService;

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file,
                                      @RequestParam("path") String path) {
        CosUploadVo cosUploadVo = cosService.upload(file,path);
        return Result.ok(cosUploadVo);
    }

}
