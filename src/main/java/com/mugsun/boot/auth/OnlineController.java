package com.mugsun.boot.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mugsun.boot.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线会话管理：枚举当前所有在线终端（多端登录各占一行），支持强制下线
 * （按 token 踢单端 / 按账号踢全部端）。会话落 Redis 后重启不失效，列表在重启后仍反映真实在线状态。
 */
@RestController
@RequestMapping("/system/online")
@SaCheckLogin
public class OnlineController {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SysUserMapper userMapper;

	public OnlineController(SysUserMapper userMapper) {
		this.userMapper = userMapper;
	}

	/** 在线会话列表：遍历所有账号会话，展开每个终端为一行 */
	@GetMapping("/list")
	public R<List<Map<String, Object>>> list() {
		List<Map<String, Object>> rows = new ArrayList<>();
		List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
		for (String sid : sessionIds) {
			SaSession session = StpUtil.getSessionBySessionId(sid);
			if (session == null) {
				continue;
			}
			Object loginId = session.getLoginId();
			SysUser user = resolveUser(loginId);
			for (SaTerminalInfo terminal : session.terminalListCopy()) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("loginId", loginId == null ? null : loginId.toString());
				row.put("username", user == null ? "-" : user.getUsername());
				row.put("nickname", user == null ? "-"
					: (user.getNickname() == null ? user.getUsername() : user.getNickname()));
				row.put("tokenValue", terminal.getTokenValue());
				row.put("tokenMask", mask(terminal.getTokenValue()));
				row.put("deviceType", terminal.getDeviceType());
				row.put("loginTime", format(terminal.getCreateTime()));
				rows.add(row);
			}
		}
		return R.data(rows);
	}

	/** 强制下线：传 tokenValue 踢单端；传 loginId 踢该账号全部端 */
	@PostMapping("/kickout")
	public R<Void> kickout(@RequestBody Map<String, String> body) {
		String token = body.get("tokenValue");
		String loginId = body.get("loginId");
		if (token != null && !token.isBlank()) {
			StpUtil.kickoutByTokenValue(token);
		} else if (loginId != null && !loginId.isBlank()) {
			StpUtil.kickout(loginId);
		} else {
			throw new ServiceException("缺少 tokenValue 或 loginId");
		}
		return R.success("已强制下线");
	}

	private SysUser resolveUser(Object loginId) {
		if (loginId == null) {
			return null;
		}
		try {
			Long id = Long.valueOf(loginId.toString());
			return TenantContext.ignore(() -> userMapper.selectOneById(id));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String mask(String token) {
		if (token == null || token.length() <= 8) {
			return token;
		}
		return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
	}

	private String format(long epochMilli) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()).format(FMT);
	}
}
