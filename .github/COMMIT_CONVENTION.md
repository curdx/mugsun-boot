# 提交信息规范

采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)，主题使用中文，不使用 emoji 与 gitmoji。

## 格式

```
<type>(<scope>): <主题>

<正文，可选>

<脚注，可选>
```

- `type` 与 `scope` 用英文小写，`scope` 可省略
- 主题不超过 50 字，使用陈述语气说明本次改动，句末不加句号
- 正文与脚注同主题之间各空一行

## type 取值

| type | 用途 |
| --- | --- |
| `feat` | 新增功能 |
| `fix` | 修复缺陷 |
| `perf` | 性能优化 |
| `refactor` | 重构，不改变外部行为 |
| `docs` | 文档 |
| `test` | 测试 |
| `build` | 构建脚本与依赖 |
| `ci` | 持续集成配置 |
| `chore` | 杂项，如版本号、配置调整 |
| `revert` | 回滚既有提交 |

## 主题写法

推荐：

```
fix(tenant): 切换路由后编辑弹窗未关闭
feat(user): 用户档案新增 real_name 与主管字段
docs(readme): 补充本地 SDK 构建步骤
perf(track): 事件队列改为批量写入 IndexedDB
```

应避免：

- 括号内堆砌清单，例如 `feat: 圈选插件（selector 生成器 + inspect 浮层 + 149 用例）`
- 空泛拔高的措辞，例如「里程碑」「全面升级」「重要突破」
- 三段式形容词并列，例如「无缝、直观、强大」
- 否定式排比，例如「不只是 X，更是 Y」
- 一次提交塞入多个不相关改动，应拆分为多次提交

## 正文

需要时再写，控制在 3 行内，说明改动原因、影响范围或迁移步骤。不复述 diff 已能体现的内容。

破坏性变更在 type 后加 `!`，并在脚注写明：

```
feat(api)!: 移除 /v1/user/list 接口

BREAKING CHANGE: 请改用 /v2/user/page，分页参数由 offset 改为 pageNo。
```

关联 issue 写在脚注：`Closes #123`。

## 工具

执行 `git config commit.template .gitmessage` 可启用本仓提交模板。
