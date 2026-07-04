package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/system/user")
@SaCheckLogin
public class SysUserController {

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public SysUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/page")
	@SaCheckPermission("sys:user:list")
	public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		Page<SysUser> page = userMapper.paginate(pageNum, pageSize, QueryWrapper.create().orderBy("id", false));
		// 密码脱敏
		page.getRecords().forEach(u -> u.setPassword(null));
		return R.data(page);
	}

	@GetMapping("/detail")
	public R<SysUser> detail(@RequestParam Long id) {
		SysUser user = userMapper.selectOneById(id);
		if (user != null) {
			user.setPassword(null);
		}
		return R.data(user);
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysUser user) {
		if (user.getId() == null) {
			String raw = (user.getPassword() == null || user.getPassword().isBlank()) ? "123456" : user.getPassword();
			user.setPassword(passwordEncoder.encode(raw));
			userMapper.insert(user);
		} else {
			if (user.getPassword() != null && !user.getPassword().isBlank()) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
			}
			userMapper.update(user);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		userMapper.deleteById(id);
		return R.success("删除成功");
	}
}
