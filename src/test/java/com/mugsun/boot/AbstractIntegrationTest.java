package com.mugsun.boot;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试基座：Testcontainers 单例容器（PostgreSQL 16 / Redis 7，JVM 级复用）+ 动态配置覆写。
 * <p>数据全部落在容器随机端口，绝不触达本机 5432/6379 的开发环境。
 * <p>复刻前端真实登录链路：取验证码（开发回显）→ 取 SM2 公钥 → sm-crypto 同构加密（C1C3C2、无 04 前缀）→ 登录换 token。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

	/** 平台超管租户编号 */
	protected static final String PLATFORM_TENANT = "000000";
	/** 平台超管账号（DataInitializer 首次启动播种） */
	protected static final String ADMIN_USERNAME = "admin";
	protected static final String ADMIN_PASSWORD = "123456";
	/** 默认登录客户端（V40 播种：验证码开） */
	protected static final String CLIENT_WEB = "web";
	/** 租户请求头（伪造越权判定依据） */
	protected static final String TENANT_HEADER = "X-Tenant-Id";
	/** Sa-Token 令牌请求头（裸 token，无前缀） */
	protected static final String TOKEN_HEADER = "Authorization";

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
		DockerImageName.parse("postgres:16-alpine"))
		.withDatabaseName("mugsun")
		.withUsername("mugsun")
		.withPassword("mugsun");

	private static final GenericContainer<?> REDIS = new GenericContainer<>(
		DockerImageName.parse("redis:7-alpine"))
		.withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add("mybatis-flex.datasource.primary.url", POSTGRES::getJdbcUrl);
		registry.add("mybatis-flex.datasource.primary.username", POSTGRES::getUsername);
		registry.add("mybatis-flex.datasource.primary.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("jetcache.remote.default.uri",
			() -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379) + "/3");
		// 测试环境不连 PowerJob Server
		registry.add("powerjob.worker.enabled", () -> "false");
		// 文件存储落 target 下临时目录，不污染 /tmp
		registry.add("dromara.x-file-storage.local-plus[0].storage-path", () -> "target/it-files/");
		// Redis 容器就绪，推送扇出保持 redis 模式
		registry.add("mugsun.websocket.sender-type", () -> "redis");
		// application.yml 的 SMTP 为占位符，邮件健康检查必然 DOWN 拖垮聚合 503；测试环境关闭该指标
		registry.add("management.health.mail.enabled", () -> "false");
	}

	@LocalServerPort
	protected int port;

	@Autowired
	protected TestRestTemplate rest;

	protected final ObjectMapper om = new ObjectMapper();

	@BeforeAll
	static void containersRunning() {
		assertThat(POSTGRES.isRunning()).as("PostgreSQL 容器已启动").isTrue();
		assertThat(REDIS.isRunning()).as("Redis 容器已启动").isTrue();
	}

	/** 解析 R 信封响应体 */
	protected JsonNode readBody(ResponseEntity<String> response) {
		try {
			return om.readTree(response.getBody());
		} catch (Exception e) {
			throw new IllegalStateException("响应体非 JSON：" + response.getBody(), e);
		}
	}

	/** 带 token 的请求头（裸 token，与前端一致） */
	protected HttpHeaders authHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (token != null) {
			headers.set(TOKEN_HEADER, token);
		}
		return headers;
	}

	protected ResponseEntity<String> get(String url, String token) {
		return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
	}

	protected ResponseEntity<String> post(String url, Object body, String token) {
		return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);
	}

	/** 带伪造租户头的 GET（租户守卫测试用） */
	protected ResponseEntity<String> getWithTenantHeader(String url, String token, String tenantHeader) {
		HttpHeaders headers = authHeaders(token);
		headers.set(TENANT_HEADER, tenantHeader);
		return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	/**
	 * 完整登录链路（与前端一致）：验证码 → SM2 公钥 → 加密密码 → 登录。
	 * 断言各步 R.code=200，返回 token。
	 */
	protected String login(String tenantId, String username, String rawPassword) {
		// 1) 图形验证码（开发模式回显明文）
		ResponseEntity<String> captchaResp = get("/auth/captcha", null);
		assertThat(captchaResp.getStatusCode().value()).as("验证码接口 HTTP 状态").isEqualTo(200);
		JsonNode captcha = readBody(captchaResp);
		assertThat(captcha.path("code").asInt()).as("验证码接口 R.code").isEqualTo(200);
		String captchaUuid = captcha.path("data").path("captchaUuid").asText();
		String captchaCode = captcha.path("data").path("captchaCode").asText();
		assertThat(captchaUuid).isNotBlank();
		assertThat(captchaCode).as("开发模式应回显验证码明文").isNotBlank();

		// 2) SM2 传输公钥（国密开关开启时前端公钥加密密码）
		ResponseEntity<String> keyResp = get("/auth/sm2-public-key", null);
		JsonNode keyData = readBody(keyResp).path("data");
		String password = rawPassword;
		if (keyData.path("gmEnabled").asBoolean()) {
			password = sm2Encrypt(rawPassword, keyData.path("publicKey").asText());
		}

		// 3) 登录
		Map<String, Object> body = new HashMap<>();
		body.put("tenantId", tenantId);
		body.put("username", username);
		body.put("password", password);
		body.put("captchaUuid", captchaUuid);
		body.put("captchaCode", captchaCode);
		body.put("clientId", CLIENT_WEB);
		ResponseEntity<String> loginResp = post("/auth/login", body, null);
		assertThat(loginResp.getStatusCode().value()).as("登录接口 HTTP 状态").isEqualTo(200);
		JsonNode login = readBody(loginResp);
		assertThat(login.path("code").asInt()).as("登录应成功：" + login.path("msg").asText()).isEqualTo(200);
		String token = login.path("data").path("token").asText();
		assertThat(token).isNotBlank();
		return token;
	}

	/** SM2 公钥加密（对齐前端 sm-crypto：C1C3C2，密文不带 04 前缀） */
	protected String sm2Encrypt(String plain, String publicKeyHex) {
		SM2 sm2 = SmUtil.sm2(null, publicKeyHex);
		sm2.setMode(SM2Engine.Mode.C1C3C2);
		String hex = sm2.encryptHex(plain, KeyType.PublicKey);
		return hex.startsWith("04") ? hex.substring(2) : hex;
	}

	/** 以平台超管身份登录 */
	protected String loginAdmin() {
		return login(PLATFORM_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD);
	}
}
