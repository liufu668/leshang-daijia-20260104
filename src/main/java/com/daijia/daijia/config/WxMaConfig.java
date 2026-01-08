package com.example.daijia.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 根据 APPID 和APP_SECRET 获取 wxOpenId
 */
@Component
@RequiredArgsConstructor
public class WxMaConfig {

    private WeChatProperties weChatProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(weChatProperties.getAppId());
        config.setSecret(weChatProperties.getAppSecret());

        WxMaService wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(config);
        return wxMaService;
    }
}
