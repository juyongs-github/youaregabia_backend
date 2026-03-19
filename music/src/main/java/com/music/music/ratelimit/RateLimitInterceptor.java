package com.music.music.ratelimit;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.music.music.exception.RateLimitExceededException;
import com.music.music.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor{

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        System.out.println(">>> RateLimitInterceptor 진입: " + request.getRequestURI());

        String email = extractEmail();
        System.out.println(">>> email: " + email);

        // userRepository 조회 없이 email을 키로 바로 사용
        RateLimitType type = resolveType(request);
        System.out.println(">>> resolvedType: " + type);


        if (email == null || type == null) return true;

        boolean allowed = rateLimiter.isAllowed(email, type);
        System.out.println(">>> isAllowed: " + allowed);

        if (!allowed) {   // rateLimiter.isAllowed() 다시 호출하지 말고 변수 사용
        throw new RateLimitExceededException(type);
        }       

        return true;
    }

    private String extractEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return principal instanceof String s ? s : null;
    }

    private RateLimitType resolveType(HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();

    // POST 요청(생성/좋아요)에 대해서만 제한 적용
    if ("POST".equals(method)) {
        // 1. 댓글 작성: /community/share/{boardId}/replies
        if (uri.contains("/replies") && !uri.contains("/like")) {
            return RateLimitType.REPLY_CREATE;
        }
        
        // 2. 게시글 작성: /community/share/add
        if (uri.contains("/community/share/add")) {
            return RateLimitType.BOARD_CREATE;
        }

        // 3. 좋아요 (게시글 & 댓글 공통)
        if (uri.contains("/like")) {
            return RateLimitType.LIKE;
        }
    }
    
    return null;
}
}
