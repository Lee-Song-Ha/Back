package com.boot.swlugweb.v1.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws  Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        SecurityUserPasswordAuthenticationFilter customAuthFilter = new SecurityUserPasswordAuthenticationFilter(authenticationManager);
        customAuthFilter.setFilterProcessesUrl("/api/login");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                // 세션 관리 설정 통합
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())  // 세션 레지스트리 설정
                        .expiredUrl("/api/login")  // 세션 만료시 리다이렉트 URL
                )
                .authorizeHttpRequests(auth -> auth
                        //모든 API에 대한 권한 설정을 진행해야 함
                        // 블로그 관련 권한
                        .requestMatchers("/api/blog/save", "/api/blog/update", "/api/blog/delete", "/api/blog/upload-image").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                        .requestMatchers("/api/blog/detail", "/api/blog/tags", "/api/blog/adjacent").permitAll()

                        // 공지사항 관련 권한
                        .requestMatchers("/api/notice/save", "/api/notice/update", "/api/notice/delete","/api/notice/upload-image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notice/**").permitAll()
                        .requestMatchers("/api/notice/detail", "/api/notice/adjacent").permitAll()

                        // 로그인/회원가입 관련 권한
                        .requestMatchers("/api/login/**").permitAll()
                        .requestMatchers("/api/login/check").permitAll()
                        .requestMatchers("/api/login/logout").permitAll()
                        .requestMatchers("/api/signup/**").permitAll()

                        //mypage는 인증된 사용자만 접근 가능
                        .requestMatchers("/api/mypage").authenticated()

                        // 비밀번호 관련 권한
                        .requestMatchers("/api/password/request-reset").permitAll()
                        .requestMatchers("/api/password/reset").permitAll()
                        .requestMatchers("/api/password/verify").permitAll()
                        .requestMatchers("/api/password/verify-auth").permitAll()

                        //이메일 인증 번호 전송(회원가입시)
                        .requestMatchers("/api/email/**").permitAll()

                        //faq, intro, main, 개보처리방안, apply 권한
                        .requestMatchers("/api/faq","/api/main", "/api/intro", "/api/apply", "/api/privacy" ).permitAll()

                        .requestMatchers("/error").permitAll()

                )
                .addFilterAt(customAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // ✅ API 요청 허용
                        .allowedHeaders("*")
                        .allowedOrigins("http://localhost:3000") // ✅ 프론트엔드 주소 허용
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // ✅ 허용할 HTTP 메서드
                        .allowCredentials(true)
                        .exposedHeaders("Authorization");
            }
        };
    }

    // HTTP 세션 이벤트 리스너 설정 유지
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}