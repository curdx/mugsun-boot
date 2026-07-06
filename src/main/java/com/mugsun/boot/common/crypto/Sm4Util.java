package com.mugsun.boot.common.crypto;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * SM4 国密对称加解密工具（字段加密用）。密钥由 {@link Sm4CryptoConfig} 启动时注入。
 */
public final class Sm4Util {

	private static volatile SM4 sm4;

	private Sm4Util() {
	}

	/** 由 Spring 启动时以配置密钥初始化（SM4 要求 16 字节密钥，不足补齐、超出截断） */
	static void init(String key) {
		byte[] raw = key.getBytes(StandardCharsets.UTF_8);
		sm4 = SmUtil.sm4(Arrays.copyOf(raw, 16));
	}

	/** 加密为 Base64 密文；null 原样返回 */
	public static String encrypt(String plain) {
		return plain == null ? null : sm4.encryptBase64(plain);
	}

	/** 解密 Base64 密文；null/空原样返回；非密文（历史明文）容错原样返回 */
	public static String decrypt(String cipher) {
		if (cipher == null || cipher.isEmpty()) {
			return cipher;
		}
		try {
			return sm4.decryptStr(cipher, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return cipher;
		}
	}
}
