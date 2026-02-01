"use strict";
var __defProp = Object.defineProperty;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
const common_vendor = require("../common/vendor.js");
const http_type = require("./type.js");
const utils_storage = require("../utils/storage.js");
const store_modules_user = require("../store/modules/user.js");

console.log('=== http/index.js 加载 ===');
console.log('utils_storage.getToken:', utils_storage.getToken);

const service = new common_vendor.Request();

service.setConfig((config) => {
  console.log('service.setConfig被调用，baseURL:', "http://localhost:8600/driver-api");
  config.timeout = http_type.ResultEnum.TIMEOUT;
  config.baseURL = "http://localhost:8600/driver-api";
  return config;
});

// =========== 修复1：添加请求拦截器 ===========
service.interceptors.request.use(
  (config) => {
    console.log('🚀🚀🚀 请求拦截器开始 🚀🚀🚀');
    console.log('请求URL:', config.url);
    console.log('请求方法:', config.method);
    
    // 保持原有的header配置
    config.header = __spreadValues({}, config.header);
    
    // 获取token
    const token = utils_storage.getToken();
    console.log('从storage获取token:', token ? '有值(' + token.length + '字符)' : 'null');
    
    if (token) {
      // 设置到请求头
      config.header.token = token;
      console.log('✅ token已添加到请求头');
      console.log('请求头完整信息:', config.header);
    } else {
      console.warn('⚠️ 没有获取到token，请求头中不会包含token');
    }
    
    console.log('🚀🚀🚀 请求拦截器结束 🚀🚀🚀');
    return config;
  },
  (config) => {
    console.error('请求拦截器错误:', config);
    common_vendor.index.showToast({
      title: "请求错误",
      icon: "error"
    });
    return Promise.reject(config);
  }
);
// =========== 修复1结束 ===========

service.interceptors.response.use(
  (response) => {
    var _a;
    console.log('📨📨📨 响应拦截器开始 📨📨📨');
    
    const { data } = response;
    
    // =========== 修复2：更新Token读取逻辑 ===========
    let newToken = null;
    
    // 尝试多种可能的token字段名
    if (response.header) {
      console.log('响应头:', response.header);
      
      // 小写token
      if (response.header.token) {
        newToken = response.header.token;
        console.log('✅ 从响应头小写token字段收到新Token');
      }
      // 大写Token
      else if (response.header.Token) {
        newToken = response.header.Token;
        console.log('✅ 从响应头大写Token字段收到新Token');
      }
      // new-token（兼容）
      else if (response.header['new-token']) {
        newToken = response.header['new-token'];
        console.log('✅ 从响应头new-token字段收到新Token（兼容模式）');
      }
    }
    
    // 如果收到新Token，更新本地存储
    if (newToken) {
      console.log('🔄 更新本地Token，长度:', newToken.length);
      console.log('Token预览:', newToken.substring(0, Math.min(30, newToken.length)) + '...');
      
      // 保存到本地存储
      const saveResult = utils_storage.setToken(newToken);
      console.log('保存token结果:', saveResult ? '成功' : '失败');
      
      // 验证保存
      const savedToken = utils_storage.getToken();
      console.log('验证保存的token:', savedToken ? '存在' : '不存在');
      
      // 更新用户store中的token（如果有）
      if (store_modules_user.useUserStore) {
        const userStore = store_modules_user.useUserStore();
        if (userStore && userStore.setToken) {
          userStore.setToken(newToken);
          console.log('✅ 已更新user store中的token');
        }
      }
    } else {
      console.log('📭 响应中没有新Token');
    }
    // =========== 修复2结束 ===========
    
    // 处理过期token
    if (http_type.ResultEnum.EXPIRE.includes(data.code)) {
      console.log('🔴 Token已过期');
      (_a = store_modules_user.useUserStore()) == null ? void 0 : _a.$reset();
      common_vendor.index.showModal({
        title: "提示",
        content: "登录过期，请重新登录",
        success: function(res) {
          if (res.confirm) {
            common_vendor.index.clearStorageSync();
            common_vendor.index.redirectTo({
              url: "/pages/login/login"
            });
          } else if (res.cancel) {
            console.log("用户不想登陆");
          }
        }
      });
      return Promise.reject(data);
    }
    
    // 处理错误响应
    if (data.code && data.code !== http_type.ResultEnum.SUCCESS) {
      console.log('❌ 业务错误:', data.code, data.message);
      common_vendor.index.showToast({
        title: data.message || http_type.ResultEnum.ERRMESSAGE,
        icon: "error"
      });
      return Promise.reject(data);
    }
    
    console.log('📨📨📨 响应拦截器结束，返回数据 📨📨📨');
    return data;
  },
  (response) => {
    console.log('🔥🔥🔥 响应错误拦截器 🔥🔥🔥');
    console.log('错误响应:', response);
    
    // =========== 修复3：即使请求失败也要检查Token ===========
    if (response && response.header) {
      let newToken = null;
      
      if (response.header.token) {
        newToken = response.header.token;
      } else if (response.header.Token) {
        newToken = response.header.Token;
      }
      
      if (newToken) {
        console.log('⚠️ 请求失败但收到新Token，更新');
        utils_storage.setToken(newToken);
      }
    }
    // =========== 修复3结束 ===========
    
    const status = response == null ? void 0 : response.statusCode;
    let message = "";
    
    switch (status) {
      case 401:
        message = "token 失效，请重新登录";
        break;
      case 403:
        message = "拒绝访问";
        break;
      case 404:
        message = "请求地址错误";
        break;
      case 500:
        message = "服务器故障";
        break;
      default:
        message = "网络连接故障";
    }
    
    console.log('错误状态码:', status, '错误信息:', message);
    
    common_vendor.index.showToast({
      title: message,
      icon: "error"
    });
    
    return Promise.reject(response);
  }
);

// =========== 修复4：手动包装http方法，确保token被添加 ===========
const http = {
  get(url, params, config) {
    console.log('📤 调用http.get');
    
    // 确保config存在
    config = config || {};
    config.header = config.header || {};
    
    // 手动添加token（双重保险）
    const token = utils_storage.getToken();
    if (token) {
      config.header.token = token;
      console.log('🛡️ http.get手动添加token');
    }
    
    return service.get(url, __spreadValues({ params }, config));
  },
  post(url, data, config) {
    console.log('📤 调用http.post');
    
    config = config || {};
    config.header = config.header || {};
    
    // 手动添加token
    const token = utils_storage.getToken();
    if (token) {
      config.header.token = token;
      console.log('🛡️ http.post手动添加token');
    }
    
    return service.post(url, data, config);
  },
  put(url, data, config) {
    console.log('📤 调用http.put');
    
    config = config || {};
    config.header = config.header || {};
    
    // 手动添加token
    const token = utils_storage.getToken();
    if (token) {
      config.header.token = token;
      console.log('🛡️ http.put手动添加token');
    }
    
    return service.put(url, data, config);
  },
  delete(url, data, config) {
    console.log('📤 调用http.delete');
    
    config = config || {};
    config.header = config.header || {};
    
    // 手动添加token
    const token = utils_storage.getToken();
    if (token) {
      config.header.token = token;
      console.log('🛡️ http.delete手动添加token');
    }
    
    return service.delete(url, data, config);
  }
};

const http$1 = http;
exports.http = http$1;

console.log('=== http/index.js 加载完成 ===');