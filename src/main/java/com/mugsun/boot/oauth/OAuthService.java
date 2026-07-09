package com.mugsun.boot.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.boot.oauth.entity.SysOauthClient;
import com.mugsun.boot.oauth.mapper.SysOauthClientMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 授权服务：客户端凭证 / 授权码 两种模式换取令牌，令牌与授权码入 Redis（TTL）。
 * 令牌为不透明字符串，元数据（clientId + scope）随令牌缓存，供 {@link OpenApiInterceptor} 校验。
 */
@Service
public class OAuthService {

	public static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
	public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";

	private static final String TOKEN_PREFIX = "mugsun:oauth:token:";
	private static final String CODE_PREFIX = "mugsun:oauth:code:";
	private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	/** 授权码有效期（秒） */
	private static final long CODE_TTL = 300;

	private final SysOauthClientMapper clientMapper;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public OAuthService(SysOauthClientMapper clientMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.clientMapper = clientMapper;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	/** 按 clientId 查启用客户端（公共流程无租户上下文，跳过隔离） */
	public SysOauthClient loadEnabledClient(String clientId) {
		SysOauthClient client = TenantManager.withoutTenantCondition(() ->
			clientMapper.selectOneByQuery(QueryWrapper.create().eq("client_id", clientId)));
		if (client == null) {
			throw new ServiceException("客户端不存在");
		}
		if (!Integer.valueOf(1).equals(client.getStatus())) {
			throw new ServiceException("客户端已停用");
		}
		return client;
	}

	/**
	 * 客户端凭证模式换取令牌。
	 *
	 * @param requestScope 请求范围（为空则授予客户端全部范围）
	 * @return [accessToken, grantedScope, expiresIn]
	 */
	public TokenResult clientCredentials(String clientId, String clientSecret, String requestScope) {
		SysOauthClient client = loadEnabledClient(clientId);
		if (!client.getClientSecret().equals(clientSecret)) {
			throw new ServiceException("客户端密钥错误");
		}
		assertGrantAllowed(client, GRANT_CLIENT_CREDENTIALS);
		String scope = resolveScope(client, requestScope);
		return issueToken(client, scope);
	}

	/** 授权码模式：登录用户授权后颁发一次性 code */
	public String authorize(long userId, String clientId, String requestScope, String redirectUri) {
		SysOauthClient client = loadEnabledClient(clientId);
		assertGrantAllowed(client, GRANT_AUTHORIZATION_CODE);
		if (client.getRedirectUri() != null && !client.getRedirectUri().isBlank()
			&& redirectUri != null && !redirectUri.isBlank()
			&& !client.getRedirectUri().equals(redirectUri)) {
			throw new ServiceException("回调地址不匹配");
		}
		String scope = resolveScope(client, requestScope);
		String code = randomStr(32);
		String meta = writeJson(Map.of("clientId", clientId, "scope", scope, "userId", String.valueOf(userId)));
		redisTemplate.opsForValue().set(CODE_PREFIX + code, meta, Duration.ofSeconds(CODE_TTL));
		return code;
	}

	/** 授权码换取令牌：校验 code 归属客户端，用后即焚 */
	public TokenResult authorizationCode(String clientId, String clientSecret, String code) {
		SysOauthClient client = loadEnabledClient(clientId);
		if (!client.getClientSecret().equals(clientSecret)) {
			throw new ServiceException("客户端密钥错误");
		}
		assertGrantAllowed(client, GRANT_AUTHORIZATION_CODE);
		if (code == null || code.isBlank()) {
			throw new ServiceException("授权码不能为空");
		}
		String key = CODE_PREFIX + code;
		String meta = redisTemplate.opsForValue().get(key);
		if (meta == null) {
			throw new ServiceException("授权码无效或已过期");
		}
		redisTemplate.delete(key);
		Map<String, String> data = readJson(meta);
		if (!clientId.equals(data.get("clientId"))) {
			throw new ServiceException("授权码与客户端不匹配");
		}
		return issueToken(client, data.get("scope"));
	}

	/** 校验令牌，返回元数据（clientId + scope 集合）；无效返回 null */
	public TokenInfo resolveToken(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		String meta = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
		if (meta == null) {
			return null;
		}
		Map<String, String> data = readJson(meta);
		return new TokenInfo(data.get("clientId"), toScopeSet(data.get("scope")));
	}

	private TokenResult issueToken(SysOauthClient client, String scope) {
		String token = "mo_" + randomStr(40);
		int validity = client.getAccessTokenValidity() == null ? 7200 : client.getAccessTokenValidity();
		String meta = writeJson(Map.of("clientId", client.getClientId(), "scope", scope == null ? "" : scope));
		redisTemplate.opsForValue().set(TOKEN_PREFIX + token, meta, Duration.ofSeconds(validity));
		return new TokenResult(token, scope, validity);
	}

	/** 授权类型校验 */
	private void assertGrantAllowed(SysOauthClient client, String grantType) {
		Set<String> allowed = toScopeSet(client.getGrantTypes());
		if (!allowed.contains(grantType)) {
			throw new ServiceException("客户端不支持该授权类型：" + grantType);
		}
	}

	/** 请求范围须为客户端可授权范围的子集；请求为空则授予全部 */
	private String resolveScope(SysOauthClient client, String requestScope) {
		Set<String> clientScopes = toScopeSet(client.getScopes());
		if (requestScope == null || requestScope.isBlank()) {
			return String.join(",", clientScopes);
		}
		Set<String> request = toScopeSet(requestScope);
		Set<String> invalid = request.stream().filter(s -> !clientScopes.contains(s)).collect(Collectors.toSet());
		if (!invalid.isEmpty()) {
			throw new ServiceException("超出客户端授权范围：" + String.join(",", invalid));
		}
		return String.join(",", request);
	}

	private Set<String> toScopeSet(String csv) {
		if (csv == null || csv.isBlank()) {
			return new LinkedHashSet<>();
		}
		return Arrays.stream(csv.split(","))
			.map(String::trim).filter(s -> !s.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String writeJson(Map<String, String> map) {
		try {
			return objectMapper.writeValueAsString(map);
		} catch (Exception e) {
			throw new ServiceException("令牌元数据序列化失败");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> readJson(String json) {
		try {
			return objectMapper.readValue(json, Map.class);
		} catch (Exception e) {
			throw new ServiceException("令牌元数据解析失败");
		}
	}

	private String randomStr(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
		}
		return sb.toString();
	}

	/** 换取令牌结果 */
	public record TokenResult(String accessToken, String scope, int expiresIn) {
	}

	/** 令牌元数据 */
	public record TokenInfo(String clientId, Set<String> scopes) {
	}
}
