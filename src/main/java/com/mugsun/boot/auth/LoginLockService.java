package com.mugsun.boot.auth;

import com.mugsun.boot.security.SecurityPolicyService;
import com.mugsun.core.tool.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录锁定：按用户名维度累计失败次数，达阈值后锁定一段时间（内存态，防暴力破解与账号枚举）。
 * 阈值/时长取自安全策略参数（后台可改，即时生效）。
 */
@Service
public class LoginLockService {

	private final SecurityPolicyService securityPolicyService;

	private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

	public LoginLockService(SecurityPolicyService securityPolicyService) {
		this.securityPolicyService = securityPolicyService;
	}

	private static final class Attempt {
		int count;
		LocalDateTime lockUntil;
	}

	/** 校验未锁定，已锁定则抛异常；锁定过期自动清除 */
	public synchronized void assertNotLocked(String username) {
		Attempt a = attempts.get(username);
		if (a != null && a.lockUntil != null) {
			if (a.lockUntil.isAfter(LocalDateTime.now())) {
				long minutes = Math.max(1, Duration.between(LocalDateTime.now(), a.lockUntil).toMinutes() + 1);
				throw new ServiceException("账号已锁定，请 " + minutes + " 分钟后再试");
			}
			attempts.remove(username);
		}
	}

	/** 记录一次失败，达阈值则锁定 */
	public synchronized void recordFail(String username) {
		Attempt a = attempts.computeIfAbsent(username, k -> new Attempt());
		a.count++;
		if (a.count >= securityPolicyService.getLoginFailMax()) {
			a.lockUntil = LocalDateTime.now().plusMinutes(securityPolicyService.getLockMinutes());
		}
	}

	/** 登录成功后清除计数 */
	public synchronized void clear(String username) {
		attempts.remove(username);
	}
}
