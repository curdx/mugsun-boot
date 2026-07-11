package com.mugsun.boot.system.service;

import com.mugsun.boot.system.entity.SysSms;
import com.mugsun.boot.system.entity.SysSmsCode;
import com.mugsun.boot.system.mapper.SysSmsCodeMapper;
import com.mugsun.boot.system.mapper.SysSmsMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mugsun.boot.tenant.TenantContext;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 短信服务：验证码发送与校验。发送通道由 sys_sms 启用配置驱动（SMS4J SmsReadConfig），
 * 无启用配置或凭证占位时降级日志（可审计），支持前端切换渠道运行时生效。
 */
@Service
public class SmsService {

	private static final Logger log = LoggerFactory.getLogger(SmsService.class);

	private final SysSmsCodeMapper smsCodeMapper;
	private final SysSmsMapper smsMapper;
	private final Random random = new Random();

	/** 开发环境回显验证码（生产须 false） */
	@Value("${mugsun.sms.show-code:true}")
	private boolean showCode;

	public SmsService(SysSmsCodeMapper smsCodeMapper, SysSmsMapper smsMapper) {
		this.smsCodeMapper = smsCodeMapper;
		this.smsMapper = smsMapper;
	}

	/** 是否回显验证码（开发便于联调，生产关闭） */
	public boolean isShowCode() {
		return showCode;
	}

	/** 取该手机号最近一条未过期验证码（仅供开发回显，勿用于生产鉴权） */
	public String peekCode(String phone) {
		SysSmsCode rec = smsCodeMapper.selectOneByQuery(QueryWrapper.create()
			.eq("phone", phone).ge("expire_time", LocalDateTime.now()).orderBy("id", false));
		return rec == null ? null : rec.getCode();
	}

	/** 发送验证码：生成 6 位码落库，按启用短信配置下发，未就绪则降级日志 */
	public void sendCode(String phone) {
		String code = String.valueOf(100000 + random.nextInt(900000));
		SysSmsCode entity = new SysSmsCode();
		entity.setPhone(phone);
		entity.setCode(code);
		entity.setExpireTime(LocalDateTime.now().plusMinutes(5));
		smsCodeMapper.insertSelective(entity);

		// 验证码通道属平台级，登录前无租户上下文，故不加租户条件取启用配置
		SysSms sms = TenantContext.ignore(() ->
			smsMapper.selectOneByQuery(QueryWrapper.create().eq("status", 1).orderBy("id", false)));
		if (sms == null) {
			log.info("无启用短信配置，降级输出验证码 phone={} code={}", phone, code);
			return;
		}
		try {
			SmsFactory.createSmsBlend(readConfigOf(sms));
			SmsResponse resp = SmsFactory.getSmsBlend(sms.getSmsCode()).sendMessage(phone, code);
			if (resp == null || !resp.isSuccess()) {
				log.info("短信配置[{}/{}]下发未成功(凭证占位)，降级输出 phone={} code={}",
					sms.getSmsCode(), sms.getCategory(), phone, code);
			} else {
				log.info("短信配置[{}/{}]下发成功 phone={}", sms.getSmsCode(), sms.getCategory(), phone);
			}
		} catch (Exception e) {
			log.info("短信配置[{}/{}]通道异常，降级输出 phone={} code={}：{}",
				sms.getSmsCode(), sms.getCategory(), phone, code, e.getMessage());
		}
	}

	/** 以启用的 sys_sms 行构建 SMS4J 配置源（当前支持 alibaba/tencent，默认 alibaba） */
	private SmsReadConfig readConfigOf(SysSms sms) {
		BaseConfig config = "tencent".equalsIgnoreCase(sms.getCategory()) ? new TencentConfig() : new AlibabaConfig();
		config.setConfigId(sms.getSmsCode());
		config.setFactory(sms.getCategory());
		config.setAccessKeyId(sms.getAccessKey());
		config.setAccessKeySecret(sms.getSecretKey());
		config.setSignature(sms.getSignature());
		config.setTemplateId(sms.getTemplateId());
		List<BaseConfig> list = List.of(config);
		return new SmsReadConfig() {
			@Override
			public BaseConfig getSupplierConfig(String configId) {
				return sms.getSmsCode().equals(configId) ? config : null;
			}

			@Override
			public List<BaseConfig> getSupplierConfigList() {
				return list;
			}
		};
	}

	/** 校验验证码：命中未过期记录则通过并作废 */
	public boolean verifyCode(String phone, String code) {
		SysSmsCode rec = smsCodeMapper.selectOneByQuery(QueryWrapper.create()
			.eq("phone", phone).eq("code", code)
			.ge("expire_time", LocalDateTime.now())
			.orderBy("id", false));
		if (rec == null) {
			return false;
		}
		smsCodeMapper.deleteById(rec.getId());
		return true;
	}
}
