package com.daijia.driver.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 根据 APPID 和APP_SECRET 获取 wxOpenId
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxMaConfig {

    private final WeChatProperties weChatProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(weChatProperties.getAppId());
        config.setSecret(weChatProperties.getAppSecret());

        log.info("配置微信小程序服务，AppId: {}", weChatProperties.getAppId());
        log.info("配置微信小程序服务，AppSecret: {}", weChatProperties.getAppSecret());

        WxMaService wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(config);
        return wxMaService;
    }
}
