package com.mall.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.R;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** 免登录白名单：认证接口、接口文档、错误页 */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/admin/login",
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    };

    /**
     * 游客可访问的商品浏览接口。
     */
    private static final String[] PUBLIC_GET_URLS = {
            "/api/product/categories/tree",
            "/api/product/spus",
            "/api/product/spus/*"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // SSE 流式响应结束时容器会发起 ASYNC 派发，此时 JWT 过滤器不参与（OncePerRequestFilter 默认跳过 ASYNC），
                // 若不放行会导致已提交的响应被重新判定为 403 并中断连接（前端表现为 ERR_ABORTED）。
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sms/**").permitAll()
                // 第三方支付回调：由渠道签名校验保证安全，不能要求用户 JWT
                .requestMatchers(HttpMethod.POST, "/api/pay/callback").permitAll()
                // 商家端接口需要登录（由 @RequirePermission 进一步控制权限）
                .requestMatchers("/api/shop/**").authenticated()
                // 平台端接口需要登录（由 @RequirePermission 进一步控制权限）
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(objectMapper.writeValueAsString(
                            R.fail(ErrorCode.Auth.UNAUTHORIZED.getCode(), ErrorCode.Auth.UNAUTHORIZED.getMsg())));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(objectMapper.writeValueAsString(
                            R.fail(ErrorCode.Auth.FORBIDDEN.getCode(), ErrorCode.Auth.FORBIDDEN.getMsg())));
                })
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
