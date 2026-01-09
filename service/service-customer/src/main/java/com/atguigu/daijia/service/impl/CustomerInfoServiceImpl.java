package com.atguigu.daijia.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.daijia.common.GuiguException;
import com.daijia.daijia.common.ResultCodeEnum;
import com.daijia.daijia.config.WeChatProperties;
import com.daijia.daijia.mapper.CustomerInfoMapper;
import com.daijia.daijia.mapper.CustomerLoginLogMapper;
import com.daijia.daijia.model.CustomerInfo;
import com.daijia.daijia.model.CustomerLoginLog;
import com.daijia.daijia.service.CustomerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerInfoServiceImpl extends Service<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    private final CustomerLoginLogMapper customerLoginLogMapper;
    private final WeChatProperties weChatProperties;
    private final WxMaService wxMaService;


    // 根据openid到数据库查找用户
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Long login(String code){

        String openId = null;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
            log.info("[小程序授权] openId={}", openId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.FAIL);
            //throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }

        CustomerInfo customerInfo = this.getOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getWxOpenId, openId));
        // 用户不存在,就创建新用户
        if(null == customerInfo){
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.getWxOpenId(openId);
            this.save(customerInfo);
        }

        // 登录日志
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);
        return customerInfo.getId();

        //// 装载请求参数,与微信服务器接口通信,获取openid,session_key
        //Map<String, String> paramsMap = new HashMap<>();
        //paramsMap.put("appid", weChatProperties.getAppId());
        //paramsMap.put("secret", weChatProperties.getAppSecret());
        //paramsMap.put("js_code", code);
        //paramsMap.put("grant_type", "authorization_code");
        //
        //// 发送HTTP GET请求,通过RestTemplate调用微信API
        //RestTemplate restTemplate = new RestTemplate();
        //// 拿到HTTP请求的response的所有内容
        //ResponseEntity<String> responseEntity = restTemplate.getForEntity(AUTH_URL, String.class, paramsMap);
        //// 拿到body的内容,里面有openid和session_key
        //String jsonResult = responseEntity.getBody();
        //// 使用jackson来完成类型转换
        //ObjectMapper objectMapper = new ObjectMapper();
        //// 获取map类型是Map<String, String>
        //MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);
        //// 将json格式的字符串转换为Map
        //Map<String, String> resultMap = null;
        //try {
        //    resultMap = objectMapper.readValue(jsonResult, mapType);
        //} catch (IOException e) {
        //    log.error("验证openId的http返回的body内容json转Map转换异常 jsonResult={}", jsonResult);
        //    throw new IOException("内容json转Map转换异常");
        //}
        //
        //String openId = resultMap.get("openid");
        //
        //// 根据openid检测用户是否存在,如果不存在,就创建用户
        //CustomerInfo customerInfo = customerService.loadUserByWxOpenId(openId);
        //
        //// 记录登录日志到 customer_login_log 表
        //CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        //customerLoginLog.setCreateTime(LocalDateTime.now());
        //customerLoginLog.setUpdateTime(LocalDateTime.now());
        //customerLoginLog.setIsDeleted(1);
        //customerLoginLog.setCustomerId(customerInfo.getNickname());
        //customerLoginLogRepository.save(customerLoginLog);
        //
        //// 生成 Token, 返回给前端
        //String token = tokenService.createToken(customerInfo);
        //CustomerInfo customerInfo = customerRepository.findByWxOpenId(wxOpenId);
        //if(customerInfo == null){
        //    // 创建新用户
        //    customerInfo = new CustomerInfo();
        //    customerInfo.setWxOpenId(wxOpenId);
        //    customerInfo.setCreateTime(LocalDateTime.now());
        //    customerInfo.setUpdateTime(LocalDateTime.now());
        //    customerInfo.setGender("1");
        //    customerInfo.setStatus(1);
        //    customerInfo.setIsDeleted(0);
        //
        //    // 保存用户数据到数据库
        //    customerRepository.save(customerInfo);
        //}
        //return customerInfo;
    }
}
