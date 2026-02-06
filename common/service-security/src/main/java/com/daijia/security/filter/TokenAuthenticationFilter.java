package com.daijia.security.filter;

import com.daijia.security.config.JwtTokenProvider;
import com.daijia.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.info("=== 拦截器收到请求 ===");
        log.info("URL: {}", requestURI);
        log.info("方法: {}", method);
        log.info("完整URL: {}?{}", requestURI, request.getQueryString());

        String token = request.getHeader("token");
        log.info("拦截器拦截并需要认证的token: {}", token);

        if(token != null && tokenService.validateToken(token)) {
            Long id = jwtTokenProvider.getIdFromToken(token);
            log.info("从前端token中解析出来的ID: ", id);
            if(id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(id,
                        null,
                        null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 刷新 Token 过期时间
                String newToken = tokenService.refreshToken(token);
                if(newToken != null && !newToken.isEmpty()){
                    // 将新 Token 添加到响应头中
                    response.setHeader("token", newToken);
                    response.setHeader("Access-Control-Expose-Headers", "token"); // 允许前端访问
                    log.info("Token已刷新并返回新Token到响应头, newToken: {}", newToken);
                }
            }
        }

        // 继续处理请求
        filterChain.doFilter(request, response);
    }
}
