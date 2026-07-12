package com.mugsun.boot.common.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时以配置密钥初始化 {@link Sm4Util}，供 {@link Sm4TypeHandler} 字段加解密使用。
 */
@Component
public class Sm4CryptoConfig {

	/** SM4 字段加密密钥（16 字节）；环境变量 MUGSUN_SM4_KEY 注入，勿硬编码生产密钥 */
	@Value("${mugsun.crypto.sm4-key:mugsun-sm4-key16}")
	private String sm4Key;

	@PostConstruct
	public void init() {
		Sm4Util.init(sm4Key);
	}
}
