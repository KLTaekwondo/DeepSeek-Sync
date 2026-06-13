# DeepSeek-Sync 开发守则

## 之前犯过的错误，不要重复

### plugin.xml 相关
- **action 文本**不要用 `<resource-bundle>` 做本地化 — IntelliJ 2025.3 不支持，改用 `update()` 里 `MyMessageBundle.message()` 动态设置
- **快捷键**必须写在 `<action>` 标签**内部**，用 `</action>` 闭合（不是 `/>`），否则注册失败
- `<group>` 的 `text` 属性在插件启动时显示，兜底用英文，`update()` 会覆盖
- `anchor` 格式：`anchor="after"` + `relative-to-action="actionId"`（两属性分开，不是 `anchor="after actionId"`）
- 删除 action 后记得清理 `relative-to-action` 引用，不然 IDE 报错
- `<version>` 改了还不够，必须同步改 `gradle.properties` 里的 `version`，构建时会覆盖

### CHANGELOG
- 一段英文 + 一段中文（跟已有格式对齐），不要写两段中文或两段英文
- 修复的放 `### Fixed` / `### 修复`，新增的放 `### Added` / `### 新增`，分清楚
- 版本号更新后记得加 changelog 条目

### Message Bundle / 字典
- `.properties` 文件默认 ISO 8859-1 编码，非 Latin 字符必须用 `\uXXXX` 转义或在 IDE 中编辑（IDE 自动处理）
- 创建新语言文件后必须**校验所有 key 是否齐全** — 用脚本对比基准文件的 key 列表
- 新增 key 必须同步更新所有已有语言文件，否则用户切语言会看到 key 名字
- 翻译前先问用户要不要翻译，不要自己机翻完就提交

### Git / 提交流程
- 先想清楚再动手，不要反复横跳来回 revert
- 不要在修复里夹带新功能，分拆提交
- 提交前检查一下改了什么：`git diff --stat`
- 推之前等用户确认，不要擅自推送

### 沟通
- 用户说的"字典"指 `.properties` 翻译文件，不是多语言 IDE 支持
- 理解不清楚的先问，不要自己猜然后跑偏
- 不要同时改太多东西，一次专注一个问题
- 用户指出错误后立即承认，不要辩解
