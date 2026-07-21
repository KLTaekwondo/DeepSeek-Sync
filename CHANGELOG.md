<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Deepseek-sync Changelog
## [0.3.1]

### v0.3.1 FIXED
- Fixed NoClassDefFoundError: com/intellij/ui/jcef/JBCefApp caused by missing explicit JCEF module dependency

### v0.3.1 修复
- 修复因 JCEF 模块依赖缺失导致的 NoClassDefFoundError: com/intellij/ui/jcef/JBCefApp 问题

## [0.3.0]

- ***🧘 此插件功能有缘更新，更多只剩下维护，核心功能已基本实现。感谢你的支持！***
- ***🧘 Feature updates on a rolling basis; otherwise, maintenance only — core features are complete. Thanks for your support!***

### v0.3.0 NEWS
- ⚙️ Customizable AI prompt templates (5 slots) via Settings panel
- 🧠 PSI context variables: `{class}`, `{method}`, `{package}`, `{imports}` in templates
- 🔄 PSI variables added to default templates — AI sees file, class, method context automatically
- 🖼️ Tool window title bar icon buttons (chat/platform/custom/refresh) — no more text tabs
- ⚡ Settings action in right-click "Send to DeepSeek" popup menu
- 🏷️ Convention-based action localization via `<resource-bundle>`
- 🌍 Settings panel UI strings localized in all 7 languages
- 📝 Maintenance notice at the bottom of settings page

### v0.3.0 FIXES
- 🔁 Auto-switch to chat tab when sending code from any tab

### v0.3.0 新增
- ⚙️ 自定义 AI 提示词模板（5 个槽位），支持设置面板编辑
- 🧠 PSI 上下文变量：`{class}` `{method}` `{package}` `{imports}` 自动注入
- 🔄 默认模板已包含 PSI 上下文，AI 自动获取文件/类/方法信息
- 🖼️ 标题栏图标按钮切换页面（聊天/控制台/自定义/刷新），去掉文本标签页
- ⚡ 右键菜单新增「设置」入口
- 🏷️ 基于 `<resource-bundle>` 约定的 Action 本地化
- 🌍 设置面板全部文字支持 7 语言本地化
- 📝 设置页面底部添加维护期告示

### v0.3.0 修复
- 🔁 发送代码时自动跳转到聊天标签页

## [0.2.2]

### Added
- 🌍 Resource bundles for 日本語・한국어・Français・Deutsch・Русский

### Fixed
- 🔗 URL not saved on in-page navigation (restore last visited page on restart)
- 🌐 Action text not localized — `<resource-bundle>` unsupported in 2025.3, switched to programmatic bundle lookup in `update()`

### 新增
- 🌍 新增多语言资源包：日语・韩语・法语・德语・俄语

### 修复
- 🔗 页面内导航不保存 URL，重启后恢复最后浏览地址
- 🌐 🐞 字典不生效的 Bug — 2025.3 不支持 `<resource-bundle>`，改为 `update()` 动态读取 bundle

## [0.2.1]

### Added
- 🌐 Custom browser tab with URL persistence & navigation (back/forward)
- ⭐ Bookmarks (max 15) with collapsible panel
- ↗ Open in system browser (for video sites)
- 🔀 Fixed tab links → auto open in custom tab
- 🎯 Popup windows intercepted, loaded in-plugin
- ⌨️ Global shortcut to toggle sidebar (Ctrl+Shift+S)
- 🔄 URL bar syncs with page navigation
- 🎨 Hover animation effects on toolbar buttons
- 🌓 Dark/Light theme adaptive icons
- 💬⚡🌐 Tab emoji icons for visual distinction
- 🌍 i18n: Chinese & English localization
- 🔄 One-click refresh for web pages
- 🎯 Smart auto-focus on chat input
- 🖱️ Right-click "Send to DeepSeek" to send selected code
- 📋 Clipboard fallback when JS injection fails
- 🤖 AI context menu (Explain/Improve/Review/Test/Comment)
- ⌨️ Keyboard shortcuts: Ctrl+Shift+D/O/P/Q/T
- 🎨 Unified DeepSeek icons for all actions

### 新增
- 🌐 自定义浏览器标签页，支持前进/后退导航
- ⭐ 收藏夹（最多 15 条），可折叠面板
- ↗ 系统浏览器打开（视频网站兜底）
- 🔀 固定标签页点链接 → 自动转到自定义页打开
- 🎯 弹窗拦截，在插件内打开
- ⌨️ 全局快捷键切换侧栏（Ctrl+Shift+S）
- 🔄 地址栏随页面导航自动同步
- 🎨 工具栏按钮悬停动效
- 🌓 深色/亮色主题自适应图标
- 💬⚡🌐 Tab 页 Emoji 图标区分
- 🌍 中英双语 i18n 本地化
- 🔄 一键刷新网页
- 🎯 聊天输入框自动聚焦
- 🖱️ 右键「发送到 DeepSeek」选中代码
- 📋 JS 注入失败时自动使用剪贴板兜底
- 🤖 新增 AI 代码操作二级菜单（解释/改进/检查/测试/注释）
- ⌨️ 专属快捷键，快速调用各种 AI 能力
- 🖱️ 选中代码右键直达

## [0.1.2]

### Added
- 🖥️ Full JetBrains IDE cross-platform support

### 新增
- 🖥️ 全面支持所有 JetBrains IDE

## [0.1.1]

### Added
- ⚡ DeepSeek Platform tab in the side panel
- 🔄 Refresh button for web pages

### 新增
- ⚡ 支持查看 DeepSeek Platform 控制台
- 🔄 新增刷新浏览器按钮

## [0.1.0] - Initial Release

### Added
- 💬 DeepSeek Web Chat embedded in IDE side panel
- 🪟 Tool window "DeepSeek-Sync/深度同步" for easy access

### 新增
- 💬 在 IDE 侧栏中嵌入 DeepSeek 网页聊天
- 🪟「DeepSeek-Sync/深度同步」工具窗口，一键打开
