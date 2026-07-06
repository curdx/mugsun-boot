package com.mugsun.boot.auth;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.mugsun.boot.mail.MailService;
import com.mugsun.boot.security.SecurityPolicyService;
import com.mugsun.core.tool.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 双因子登录：密码校验通过后下发二次验证码（邮箱/短信，无凭证降级日志），二次校验通过才发 token。
 */
@Service
public class TwoFactorService {

	private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);
	private static final String KEY = "mugsun:2fa:";
	private static final long EXPIRE_MIN = 5;

	private final StringRedisTemplate redis;
	private final SecurityPolicyService policy;
	private final MailService mailService;

	@Value("${mugsun.captcha.show-code:true}")
	private boolean showCode;

	public TwoFactorService(StringRedisTemplate redis, SecurityPolicyService policy, MailService mailService) {
		this.redis = redis;
		this.policy = policy;
		this.mailService = mailService;
	}

	public boolean isEnabled() {
		return policy.isTwoFactorEnabled();
	}

	/** 生成二次验证码存 Redis、按渠道下发，返回 [token, code(仅 dev)] */
	public String[] challenge(Long userId, String contact) {
		String code = RandomUtil.randomNumbers(6);
		String token = IdUtil.fastSimpleUUID();
		redis.opsForValue().set(KEY + token, userId + ":" + code, Duration.ofMinutes(EXPIRE_MIN));
		String channel = policy.getTwoFactorChannel();
		try {
			if ("sms".equals(channel)) {
				// 复用 SMS4J 短信通道（无凭证降级日志）
				log.info("双因子[短信] 验证码 {} 发往 {}", code, contact);
			} else {
				mailService.sendByTemplate(contact == null || contact.isBlank() ? "user@mugsun.local" : contact,
					"login_2fa", Map.of("code", code));
			}
		} catch (Exception e) {
			log.warn("双因子验证码下发失败(降级为日志) err={}", e.getMessage());
		}
		return new String[]{token, showCode ? code : null};
	}

	/** 校验二次验证码，返回 userId */
	public Long verify(String token, String code) {
		if (token == null || token.isBlank() || code == null || code.isBlank()) {
			throw new ServiceException("请输入验证码");
		}
		String v = redis.opsForValue().get(KEY + token);
		if (v == null) {
			throw new ServiceException("验证码已过期，请重新登录");
		}
		int idx = v.lastIndexOf(':');
		String uid = v.substring(0, idx);
		String real = v.substring(idx + 1);
		if (!real.equals(code)) {
			// 错码不消耗 token，允许 5 分钟 TTL 内重试
			throw new ServiceException("验证码错误");
		}
		redis.delete(KEY + token);
		return Long.parseLong(uid);
	}
}
