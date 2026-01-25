package com.daijia.security.config;

import com.daijia.security.filter.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor // Lombok:为所有final字段生成构造函数
@EnableWebSecurity
public class WebSecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 权限认证
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    /**
     * Spring Security 过滤器链配置
     * 定义应用程序的安全规则和认证流程
     *
     * @param http HttpSecurity对象，用于配置安全策略
     * @return 配置好的SecurityFilterChain
     * @throws Exception 配置过程中可能抛出的异常
     */
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 需要跨域支持
                 .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 禁用CSRF（跨站请求伪造）保护
                // 对于REST API或无状态应用，通常需要禁用CSRF
                // 因为CSRF主要针对基于浏览器的会话
                .csrf(csrf -> csrf.disable())

                // 会话管理配置 - 设置为无状态（STATELESS）
                // 意味着不使用HTTP Session来存储安全上下文
                // 适用于JWT等基于令牌的认证
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 授权配置：定义哪些请求需要认证，哪些可以公开访问
                .authorizeHttpRequests(auth -> auth
                        // 放行（允许匿名访问）的请求路径
                        // 通常包括：登录、注册、公开API等不需要认证的接口
                        .requestMatchers("/customer-api/customer/**").permitAll()

                        // 其他所有请求都需要认证
                        // 此规则必须放在最后，因为它会匹配所有未在前面匹配到的请求
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 构建并返回配置好的安全过滤器链
        return http.build();
    }
    /**
     * CORS配置
     * @return
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //  允许所有来源,生产环境请限制具体域名
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带凭证(如cookies)
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 应用到所有路径
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 给密码加密
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
