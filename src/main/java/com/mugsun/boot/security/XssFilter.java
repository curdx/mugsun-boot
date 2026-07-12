package com.mugsun.boot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 全局 XSS 净化过滤器：净化 query/form 参数并缓存请求体。多部件（文件上传）与静态资源跳过。
 * <p>JSON 请求体字段的净化由 {@link XssCleaner} 的 Jackson 反序列化器完成（本过滤器只负责参数与 body 缓存）。
 */
@Component
@Order(-100)
public class XssFilter extends OncePerRequestFilter {

	/** 跳过净化的路径前缀（文件上传/下载、静态资源、监控） */
	private static final List<String> EXCLUDES = List.of(
		"/system/oss", "/system/file", "/actuator", "/warm-flow", "/v3/api-docs", "/doc.html", "/swagger");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String contentType = request.getContentType();
		boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
		if (multipart || isExcluded(request.getRequestURI())) {
			chain.doFilter(request, response);
			return;
		}
		chain.doFilter(new XssRequestWrapper(request), response);
	}

	private boolean isExcluded(String uri) {
		if (uri == null) {
			return false;
		}
		for (String prefix : EXCLUDES) {
			if (uri.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
