package com.devicemind.core.config;

import com.devicemind.common.utils.ContextUtils;
import com.devicemind.common.utils.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * traceId + 用户上下文过滤器。
 * <p>
 * 每个 HTTP 请求入口：
 * <ol>
 *   <li>优先取上游 {@link TraceContext#TRACE_ID_HEADER} 头，没有则生成新的 traceId，写入 MDC 并回写响应头</li>
 *   <li>写入硬编码 admin 用户上下文（personId=-1），省去登录环节</li>
 *   <li>请求结束后清理 MDC 和 ThreadLocal 上下文</li>
 * </ol>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = TraceContext.set(request.getHeader(TraceContext.TRACE_ID_HEADER));
            response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
            ContextUtils.setCurrentUserId(-1L);
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
            ContextUtils.clear();
        }
    }
}
