package com.example.jwt.config.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 클라이언트 요청 시 JWT 인증을 하기 위해 설치하는 커스텀 필터로
// UsernamePasswordAuthenticationFiler 이전에 실행된다.
// 이전에 실행된다는 뜻은 JwtAuthenticationFilter를 통과하면
// UsernamePasswordAuthenticationFilter 이후의 필터는 통과한 것으로 본다는 뜻이다.
// 쉽게 말해서, Username + Password를 통한 인증을 Jwt를 통해 수행한다는 것이다.

// JWT 방식은 세션과 다르게 Filter 하나를 추가해야 합니다.
// 이제 사용자가 로그인을 했을 때, Request에 가지고 있는 Token을 해석해주는 로직이 필요합니다.
// 이 역할을 해주는것이 JwtAuthenticationFilter입니다.
// 세부 비즈니스 로직들은 TokenProvider에 적어둡니다. 일종의 service 클래스라고 생각하면 편합니다.
// 1. 사용자의 Request Header에 토큰을 가져옵니다.
// 2. 해당 토큰의 유효성 검사를 실시하고 유효하면
// 3. Authentication 인증 객체를 만들고
// 4. ContextHolder에 저장해줍니다.
// 5. 해당 Filter 과정이 끝나면 이제 시큐리티에 다음 Filter로 이동하게 됩니다.


@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean {

    public static final String HEADER_AUTHORIZATION ="Authorization" ;
    private final JwtProvider jwtProvider;


    // doFilter는 토큰의 인증정보를 SecurityContext에 저장하는 역할 수행
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        // 1. Requset Header에서 JWT 토큰 추출
        String jwt = resolveToken(httpServletRequest);
        String requestURI = httpServletRequest.getRequestURI();

        if(StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
            // 토큰이 유효할 경우 토큰에서 Authentication 객체를 가지고 와서 SecurityContext 에 저장
            Authentication authentication = jwtProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Security Context에 '{}' 인증 정보를 저장했습니다., uri : {}",
                    authentication.getName(), requestURI);
        } else {
            log.debug("유효한 JWT 토큰이 없습니다. uri : {}", requestURI);
        }
        chain.doFilter(request, response);
    }

    // Request Header 에서 토큰 정보를 꺼내오기 위한 메소드
    private String resolveToken(HttpServletRequest httpServletRequest) {
        String bearerToken = httpServletRequest.getHeader(HEADER_AUTHORIZATION);

        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        } else {
            return null;
        }
    }
}
