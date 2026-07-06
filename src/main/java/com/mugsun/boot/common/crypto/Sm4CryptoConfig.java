package com.mugsun.boot.common.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时以配置密钥初始化 {@link Sm4Util}，供 {@link Sm4TypeHandler} 字段加解密使用。
 */
@Component
public class Sm4CryptoConfig {

	@Value("${mugsun.crypto.sm4-key:mugsun-sm4-key16}")
	private String sm4Key;

	@PostConstruct
	public void init() {
		Sm4Util.init(sm4Key);
	}
}
