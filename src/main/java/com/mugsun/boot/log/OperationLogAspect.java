package com.mugsun.boot.log;

import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.system.entity.SysOperLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志切面：在请求线程收集上下文，交由独立 Bean 异步落库，避免阻塞主流程。
 */
@Aspect
@Component
public class OperationLogAspect {

	private final OperationLogService logService;

	public OperationLogAspect(OperationLogService logService) {
		this.logService = logService;
	}

	@Around("@annotation(operationLog)")
	public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
		long start = System.currentTimeMillis();
		SysOperLog log = new SysOperLog();
		log.setTitle(operationLog.value());
		log.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
		log.setParams(Arrays.toString(point.getArgs()));
		fillRequest(log);
		fillOperator(log);
		int status = 1;
		String error = null;
		try {
			return point.proceed();
		} catch (Throwable e) {
			status = 0;
			error = e.getMessage();
			throw e;
		} finally {
			log.setStatus(status);
			log.setErrorMsg(error);
			log.setDuration(System.currentTimeMillis() - start);
			logService.saveAsync(log);
		}
	}

	private void fillRequest(SysOperLog log) {
		try {
			ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attr != null) {
				HttpServletRequest req = attr.getRequest();
				log.setRequestMethod(req.getMethod());
				log.setRequestUri(req.getRequestURI());
				log.setIp(req.getRemoteAddr());
			}
		} catch (Exception ignore) {
		}
	}

	private void fillOperator(SysOperLog log) {
		try {
			Object loginId = StpUtil.getLoginIdDefaultNull();
			if (loginId != null) {
				log.setOperator(loginId.toString());
			}
		} catch (Exception ignore) {
		}
	}
}
