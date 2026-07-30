package com.mugsun.boot;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证链路回归：真实 HTTP 走验证码 + SM2 加密登录全链路。
 */
class AuthApiTest extends AbstractIntegrationTest {

	@Test
	void wrongPasswordIsRejected() {
		// 取验证码后按真实链路提交错误密码（SM2 密文可正常解密，落 BCrypt 比对失败分支）
		JsonNode captcha = readBody(get("/auth/captcha", null));
		String publicKey = readBody(get("/auth/sm2-public-key", null)).path("data").path("publicKey").asText();
		Map<String, Object> body = new HashMap<>();
		body.put("tenantId", PLATFORM_TENANT);
		body.put("username", ADMIN_USERNAME);
		body.put("password", sm2Encrypt("wrong-password-1", publicKey));
		body.put("captchaUuid", captcha.path("data").path("captchaUuid").asText());
		body.put("captchaCode", captcha.path("data").path("captchaCode").asText());
		body.put("clientId", CLIENT_WEB);

		ResponseEntity<String> response = post("/auth/login", body, null);
		// 业务异常走 R 信封：HTTP 200 + code=400（FAILURE），不暴露账号是否存在
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		JsonNode r = readBody(response);
		assertThat(r.path("code").asInt()).isEqualTo(400);
		assertThat(r.path("success").asBoolean()).isFalse();
		assertThat(r.path("msg").asText()).isEqualTo("账号或密码错误");
		assertThat(r.path("data").isMissingNode() || r.path("data").isNull()).isTrue();
	}

	@Test
	void correctLoginReturnsToken() {
		String token = loginAdmin();
		assertThat(token).isNotBlank();
	}

	@Test
	void infoReturnsRolesAndButtons() {
		String token = loginAdmin();
		ResponseEntity<String> response = get("/auth/info", token);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		JsonNode r = readBody(response);
		assertThat(r.path("code").asInt()).isEqualTo(200);
		JsonNode data = r.path("data");
		assertThat(data.path("userName").asText()).isEqualTo(ADMIN_USERNAME);
		// 平台超管：内置 admin 角色 + 前端门控伪角色 R_SUPER/R_ADMIN
		java.util.List<String> roles = new java.util.ArrayList<>();
		data.path("roles").forEach(n -> roles.add(n.asText()));
		assertThat(roles).contains("admin", "R_SUPER", "R_ADMIN");
		// 超管通配权限
		java.util.List<String> buttons = new java.util.ArrayList<>();
		data.path("buttons").forEach(n -> buttons.add(n.asText()));
		assertThat(buttons).contains("*");
	}

	@Test
	void protectedApiWithoutTokenReturns401() {
		ResponseEntity<String> response = get("/auth/info", null);
		assertThat(response.getStatusCode().value()).isEqualTo(401);
		JsonNode r = readBody(response);
		assertThat(r.path("code").asInt()).isEqualTo(401);
		assertThat(r.path("success").asBoolean()).isFalse();
	}
}
