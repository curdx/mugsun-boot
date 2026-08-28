# Git 提交信息约定

本仓提交信息按 [Humanizer-zh](https://github.com/op7418/Humanizer-zh) 思路写：**说清改了什么，少堆形容词和清单式口号**。

## 格式

```
<gitmoji> <主题（50 字内）>

<可选正文：为何改、注意点>
```

gitmoji 沿用现有习惯：`:sparkles:` 新功能、`:bug:` 修复、`:zap:` 优化、`:memo:` 文档、`:wrench:` 配置。

## 主题怎么写

**可以：**

- `:bug: 租户页切路由后编辑弹窗没关`
- `:sparkles: 用户档案增加 real_name 与主管字段`
- `:memo: README 补本地 SDK 构建步骤`

**避免：**

- 过长括号清单：`圈选插件（G104）：selector 唯一性生成器 + inspect 浮层 + …（vitest 149 用例）`
- 空泛拔高：`作为 XX 的证明`、`关键转折点`、`不断演变的格局`
- 三段式形容词：`无缝、直观、强大`
- 否定式排比：`不仅仅是 X，而是 Y`

## 正文

- 需要时再写正文，1～3 行即可
- 写 breaking change、迁移步骤、关联 issue
- 不要复述 diff 里已经 obvious 的内容

## 改历史提交

已经 push 的提交若要改信息，用 `git rebase -i` 或联系维护者，**不要**在 shared 分支上 silent force-push。

本地 skill：`humanizer-zh`（工作区 `.agents/skills/humanizer-zh`）可用来润色 README、PR 描述和提交说明。
