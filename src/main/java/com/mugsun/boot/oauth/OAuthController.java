package com.mugsun.boot.oauth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 令牌端点（开放平台，公共访问）：
 * <ul>
 *   <li>POST /oauth2/token —— 客户端凭证 / 授权码 换取令牌</li>
 *   <li>POST /oauth2/authorize —— 登录用户授权，颁发一次性授权码</li>
 * </ul>
 */
@RestController
@RequestMapping("/oauth2")
public class OAuthController {

	private final OAuthService oauthService;

	public OAuthController(OAuthService oauthService) {
		this.oauthService = oauthService;
	}

	/** 换取访问令牌，按 grantType 分发 */
	@PostMapping("/token")
	public R<Map<String, Object>> token(@RequestBody TokenRequest req) {
		String grantType = req.grantType();
		OAuthService.TokenResult result;
		if (OAuthService.GRANT_CLIENT_CREDENTIALS.equals(grantType)) {
			result = oauthService.clientCredentials(req.clientId(), req.clientSecret(), req.scope());
		} else if (OAuthService.GRANT_AUTHORIZATION_CODE.equals(grantType)) {
			result = oauthService.authorizationCode(req.clientId(), req.clientSecret(), req.code());
		} else {
			throw new ServiceException("不支持的授权类型：" + grantType);
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("accessToken", result.accessToken());
		resp.put("tokenType", "Bearer");
		resp.put("expiresIn", result.expiresIn());
		resp.put("scope", result.scope());
		return R.data(resp);
	}

	/** 登录用户对客户端授权，颁发一次性授权码 */
	@PostMapping("/authorize")
	@SaCheckLogin
	public R<Map<String, Object>> authorize(@RequestBody AuthorizeRequest req) {
		String code = oauthService.authorize(StpUtil.getLoginIdAsLong(), req.clientId(), req.scope(), req.redirectUri());
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("code", code);
		resp.put("redirectUri", req.redirectUri());
		return R.data(resp);
	}

	/** 换取令牌参数 */
	public record TokenRequest(String grantType, String clientId, String clientSecret, String scope,
							   String code, String redirectUri) {
	}

	/** 授权参数 */
	public record AuthorizeRequest(String clientId, String scope, String redirectUri) {
	}
}
