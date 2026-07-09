package com.mugsun.boot.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysUserOauth;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysUserOauthMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社交登录：以第三方回调得到的 (source, openId) 完成绑定/登录。
 * 已绑定→直接登录；未绑定→自助建号并绑定后登录。第三方 OAuth 授权握手（JustAuth 微信/支付宝/QQ）
 * 需真实应用凭证，属凭证投递范畴，本服务承载回调之后的绑定/登录逻辑。
 */
@Service
public class SocialService {

	private static final String DEFAULT_TENANT = "000000";

	private final SysUserOauthMapper oauthMapper;
	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public SocialService(SysUserOauthMapper oauthMapper, SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.oauthMapper = oauthMapper;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	/** 按 source+openId 登录：已绑定则登录，未绑定则自助建号绑定后登录，返回 token */
	@Transactional(rollbackFor = Exception.class)
	public String loginByOpenId(String source, String openId, String unionId) {
		SysUser user = TenantManager.withoutTenantCondition(() -> {
			SysUserOauth bind = oauthMapper.selectOneByQuery(QueryWrapper.create()
				.eq("source", source).eq("open_id", openId));
			if (bind != null) {
				return userMapper.selectOneById(bind.getUserId());
			}
			SysUser created = createSocialUser(source, openId);
			userMapper.insert(created);
			SysUserOauth oauth = new SysUserOauth();
			oauth.setUserId(created.getId());
			oauth.setSource(source);
			oauth.setOpenId(openId);
			oauth.setUnionId(unionId);
			oauthMapper.insert(oauth);
			return created;
		});
		if (user == null) {
			throw new ServiceException("绑定用户不存在");
		}
		StpUtil.login(user.getId());
		StpUtil.getSession().set("tenantId", user.getTenantId());
		return StpUtil.getTokenValue();
	}

	/** 当前登录用户绑定一个社交账号（幂等；已被他人绑定则拒绝） */
	public void bind(Long userId, String source, String openId, String unionId) {
		SysUserOauth exist = TenantManager.withoutTenantCondition(() -> oauthMapper.selectOneByQuery(
			QueryWrapper.create().eq("source", source).eq("open_id", openId)));
		if (exist != null) {
			if (exist.getUserId().equals(userId)) {
				return;
			}
			throw new ServiceException("该社交账号已绑定其他用户");
		}
		SysUserOauth oauth = new SysUserOauth();
		oauth.setUserId(userId);
		oauth.setSource(source);
		oauth.setOpenId(openId);
		oauth.setUnionId(unionId);
		TenantManager.withoutTenantCondition(() -> oauthMapper.insert(oauth));
	}

	/** 未绑定时自助建号：用户名 source_openid 前缀，随机密码，默认租户 */
	private SysUser createSocialUser(String source, String openId) {
		String suffix = openId.length() > 40 ? openId.substring(0, 40) : openId;
		SysUser user = new SysUser();
		user.setUsername(source + "_" + suffix);
		user.setNickname(source + "用户" + suffix);
		user.setPassword(passwordEncoder.encode(IdUtil.fastSimpleUUID()));
		user.setStatus(1);
		user.setTenantId(DEFAULT_TENANT);
		return user;
	}
}
