package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.entity.SysNotice;
import com.mugsun.boot.system.entity.SysNoticeRead;
import com.mugsun.boot.system.entity.SysNoticeScope;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.boot.system.mapper.SysNoticeMapper;
import com.mugsun.boot.system.mapper.SysNoticeReadMapper;
import com.mugsun.boot.system.mapper.SysNoticeScopeMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知公告管理：管理端 CRUD + 可见范围/阅读记录；用户端「我的通知」按可见范围过滤 + 已读留痕。
 */
@RestController
@RequestMapping("/system/notice")
@SaCheckLogin
public class SysNoticeController {

	private static final String PERM_MANAGE = "sys:notice:manage";

	private final SysNoticeMapper noticeMapper;
	private final SysNoticeScopeMapper scopeMapper;
	private final SysNoticeReadMapper readMapper;
	private final SysUserMapper userMapper;
	private final SysDeptMapper deptMapper;

	public SysNoticeController(SysNoticeMapper noticeMapper, SysNoticeScopeMapper scopeMapper,
							   SysNoticeReadMapper readMapper, SysUserMapper userMapper, SysDeptMapper deptMapper) {
		this.noticeMapper = noticeMapper;
		this.scopeMapper = scopeMapper;
		this.readMapper = readMapper;
		this.userMapper = userMapper;
		this.deptMapper = deptMapper;
	}

	// ============ 管理端 ============

	/** 分页：置顶优先，再按时间倒序；支持按分类过滤 */
	@GetMapping("/page")
	@SaCheckPermission(PERM_MANAGE)
	public R<Page<SysNotice>> page(@RequestParam(defaultValue = "1") long pageNum,
								   @RequestParam(defaultValue = "10") long pageSize,
								   @RequestParam(required = false) String category) {
		QueryWrapper query = QueryWrapper.create().orderBy("is_top", false).orderBy("id", false);
		if (category != null && !category.isBlank()) {
			query.eq("category", category);
		}
		return R.data(noticeMapper.paginate(pageNum, pageSize, query));
	}

	/** 置顶公告列表 */
	@GetMapping("/top")
	public R<List<SysNotice>> top() {
		return R.data(noticeMapper.selectListByQuery(
			QueryWrapper.create().eq("is_top", 1).orderBy("id", false)));
	}

	/** 详情：附可见范围明细（回显穿梭框） */
	@GetMapping("/detail")
	public R<SysNotice> detail(@RequestParam Long id) {
		SysNotice notice = noticeMapper.selectOneById(id);
		if (notice != null) {
			notice.setScopeList(scopeMapper
				.selectListByQuery(QueryWrapper.create().eq("notice_id", id))
				.stream()
				.map(s -> new SysNotice.NoticeScopeItem(s.getScopeType(), s.getScopeId()))
				.toList());
		}
		return R.data(notice);
	}

	/** 新增/更新：主体 + 可见范围（先删旧再插新） */
	@PostMapping("/submit")
	@SaCheckPermission(PERM_MANAGE)
	@Transactional(rollbackFor = Exception.class)
	public R<Void> submit(@RequestBody SysNotice notice) {
		if (notice.getAllVisible() == null) {
			notice.setAllVisible(1);
		}
		if (notice.getId() == null) {
			noticeMapper.insertSelective(notice);
		} else {
			noticeMapper.update(notice);
		}
		Long noticeId = notice.getId();
		scopeMapper.deleteByQuery(QueryWrapper.create().eq("notice_id", noticeId));
		if (notice.getAllVisible() == 0 && notice.getScopeList() != null) {
			for (SysNotice.NoticeScopeItem item : notice.getScopeList()) {
				SysNoticeScope scope = new SysNoticeScope();
				scope.setNoticeId(noticeId);
				scope.setScopeType(item.scopeType());
				scope.setScopeId(item.scopeId());
				scopeMapper.insertSelective(scope);
			}
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	@SaCheckPermission(PERM_MANAGE)
	@Transactional(rollbackFor = Exception.class)
	public R<Void> remove(@RequestBody List<Long> ids) {
		noticeMapper.deleteBatchByIds(ids);
		for (Long id : ids) {
			scopeMapper.deleteByQuery(QueryWrapper.create().eq("notice_id", id));
		}
		return R.success("删除成功");
	}

	/** 阅读记录分页（谁读了 + 次数 + 首末时间），装配昵称/部门 */
	@GetMapping("/read/page")
	@SaCheckPermission(PERM_MANAGE)
	public R<Page<SysNoticeRead>> readPage(@RequestParam Long noticeId,
										   @RequestParam(defaultValue = "1") long pageNum,
										   @RequestParam(defaultValue = "10") long pageSize) {
		Page<SysNoticeRead> page = readMapper.paginate(pageNum, pageSize,
			QueryWrapper.create().eq("notice_id", noticeId).orderBy("last_time", false));
		fillReaders(page.getRecords());
		return R.data(page);
	}

	// ============ 用户端「我的通知」 ============

	/** 我可见的通知分页：all_visible=1 或命中可见范围；管理员见全部；置顶优先 */
	@GetMapping("/my/page")
	public R<Page<SysNotice>> myPage(@RequestParam(defaultValue = "1") long pageNum,
									 @RequestParam(defaultValue = "10") long pageSize,
									 @RequestParam(required = false) String category) {
		QueryWrapper query = QueryWrapper.create();
		if (category != null && !category.isBlank()) {
			query.eq("category", category);
		}
		if (!StpUtil.hasPermission(PERM_MANAGE)) {
			Long userId = StpUtil.getLoginIdAsLong();
			Long deptId = deptIdOf(userId);
			query.and("(all_visible = 1 or id in (select notice_id from sys_notice_scope "
				+ "where is_deleted = 0 and ((scope_type = 1 and scope_id = ?) or (scope_type = 2 and scope_id = ?))))",
				userId, deptId == null ? -1L : deptId);
		}
		query.orderBy("is_top", false).orderBy("id", false);
		Page<SysNotice> page = noticeMapper.paginate(pageNum, pageSize, query);
		fillReadFlag(page.getRecords());
		return R.data(page);
	}

	/** 标记已读（幂等 upsert）：首读 view_uv+1，每读 view_pv+1、read_count+1 */
	@PostMapping("/read/{noticeId}")
	@Transactional(rollbackFor = Exception.class)
	public R<Void> read(@PathVariable Long noticeId) {
		Long userId = StpUtil.getLoginIdAsLong();
		// 原子 upsert，RETURNING xmax=0 精确区分「首次插入」与「已存在再读」，据此累计 UV
		Row row = Db.selectOneBySql(
			"insert into sys_notice_read (id, notice_id, user_id, read_count, first_time, last_time, create_time, update_time, is_deleted) "
				+ "values (?, ?, ?, 1, now(), now(), now(), now(), 0) "
				+ "on conflict (notice_id, user_id) where is_deleted = 0 "
				+ "do update set read_count = sys_notice_read.read_count + 1, last_time = now(), update_time = now() "
				+ "returning (xmax = 0) as first_read",
			IdUtil.getSnowflakeNextId(), noticeId, userId);
		boolean firstRead = row != null && Boolean.TRUE.equals(row.getBoolean("first_read"));
		Db.updateBySql("update sys_notice set view_pv = view_pv + 1" + (firstRead ? ", view_uv = view_uv + 1" : "")
			+ " where id = ?", noticeId);
		return R.success("已读");
	}

	/** 我的未读通知数（可见范围内、未读过的） */
	@GetMapping("/my/unread-count")
	public R<Long> myUnreadCount() {
		Long userId = StpUtil.getLoginIdAsLong();
		Long deptId = deptIdOf(userId);
		QueryWrapper query = QueryWrapper.create();
		if (!StpUtil.hasPermission(PERM_MANAGE)) {
			query.and("(all_visible = 1 or id in (select notice_id from sys_notice_scope "
				+ "where is_deleted = 0 and ((scope_type = 1 and scope_id = ?) or (scope_type = 2 and scope_id = ?))))",
				userId, deptId == null ? -1L : deptId);
		}
		query.and("id not in (select notice_id from sys_notice_read where is_deleted = 0 and user_id = ?)", userId);
		return R.data(noticeMapper.selectCountByQuery(query));
	}

	// ============ 内部工具 ============

	/** 当前用户部门（优先取已激活的数据权限上下文，兜底查库） */
	private Long deptIdOf(Long userId) {
		DataScopeContext.Ctx ctx = DataScopeContext.current();
		if (ctx != null && ctx.deptId() != null) {
			return ctx.deptId();
		}
		SysUser user = userMapper.selectOneById(userId);
		return user == null ? null : user.getDeptId();
	}

	/** 装配当前用户对通知列表的已读标记 */
	private void fillReadFlag(List<SysNotice> notices) {
		if (notices.isEmpty()) {
			return;
		}
		Long userId = StpUtil.getLoginIdAsLong();
		List<Long> ids = notices.stream().map(SysNotice::getId).toList();
		List<Long> readIds = readMapper.selectListByQuery(
				QueryWrapper.create().eq("user_id", userId).in("notice_id", ids))
			.stream().map(SysNoticeRead::getNoticeId).toList();
		notices.forEach(n -> n.setReadFlag(readIds.contains(n.getId())));
	}

	/** 批量装配阅读人昵称/部门（避免 N+1） */
	private void fillReaders(List<SysNoticeRead> records) {
		if (records.isEmpty()) {
			return;
		}
		List<Long> userIds = records.stream().map(SysNoticeRead::getUserId).distinct().toList();
		Map<Long, SysUser> userMap = userMapper.selectListByIds(userIds).stream()
			.collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
		List<Long> deptIds = userMap.values().stream().map(SysUser::getDeptId)
			.filter(java.util.Objects::nonNull).distinct().toList();
		Map<Long, String> deptMap = deptIds.isEmpty() ? Map.of()
			: deptMapper.selectListByIds(deptIds).stream()
			.collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
		records.forEach(r -> {
			SysUser u = userMap.get(r.getUserId());
			if (u != null) {
				r.setNickname(u.getNickname() == null ? u.getUsername() : u.getNickname());
				r.setDeptName(u.getDeptId() == null ? null : deptMap.get(u.getDeptId()));
			}
		});
	}
}
