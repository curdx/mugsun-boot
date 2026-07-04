package com.mugsun.boot.config;

import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化数据：首次启动创建超级管理员（admin / 123456）
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		long count = userMapper.selectCountByQuery(QueryWrapper.create().eq("username", "admin"));
		if (count == 0) {
			SysUser admin = new SysUser();
			admin.setUsername("admin");
			admin.setPassword(passwordEncoder.encode("123456"));
			admin.setNickname("超级管理员");
			admin.setStatus(1);
			userMapper.insert(admin);
		}
	}
}
