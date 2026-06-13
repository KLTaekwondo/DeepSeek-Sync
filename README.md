# DeepSeek-Sync

**Version**: 0.3.0
**License**: Apache 2.0

Bring DeepSeek into your IDE — seamless side panel integration with AI code actions and customizable prompts.

将 DeepSeek 带入你的 IDE — 侧边栏无缝集成，支持 AI 代码操作与可自定义的提示词模板。

---

## Features / 功能特性

- 🌐 **Embedded DeepSeek Web Chat** — Side tool window chat without leaving your IDE  
  **嵌入 DeepSeek 网页聊天** — 侧边栏直接对话，不离开 IDE

- 📊 **Platform Dashboard** — Quick access to `platform.deepseek.com` to view API Key status and remaining credits  
  **平台控制台** — 快速访问 `platform.deepseek.com`，查看 API Key 状态和剩余额度

- 🌍 **Custom URL Tab** — Browse any website with navigation controls, back/forward, and bookmarks  
  **自定义网页标签** — 浏览任意网站，支持前进后退导航与书签

- ⚡ **Code Actions** — Select code in the editor, right-click → `Send to DeepSeek Sync` → Explain / Improve / Review / Test / Comment  
  **代码操作** — 在编辑器选中代码，右键 → `发送到 DeepSeek Sync` → 解释 / 改进 / 审查 / 测试 / 注释

- 🧠 **PSI Context** — Templates automatically inject `{class}`, `{method}`, `{package}`, `{imports}` from your code context  
  **PSI 上下文** — 模板自动注入 `{class}`、`{method}`、`{package}`、`{imports}` 等代码上下文

- 🎨 **Customizable Prompts** — Fully customizable labels and templates via `Settings → Tools → DeepSeek-Sync`  
  **自定义提示词** — 通过 `设置 → 工具 → DeepSeek-Sync` 完全自定义标签和模板

- 🐛 **Terminal Error Explanation** — Right-click errors in the terminal and get AI-powered analysis  
  **终端错误解释** — 在终端中右键错误，获取 AI 分析

- 🌍 **Multi-Language UI** — Supports 7 languages: English, Chinese, Japanese, Korean, German, French, Russian  
  **多语言界面** — 支持 7 种语言：英语、简体中文、日语、韩语、德语、法语、俄语

- 🔄 **Page Refresh** — Easy reload of embedded web pages when needed  
  **页面刷新** — 需要时一键刷新嵌入式网页

- 🚀 **Multi-IDE Support** — Compatible with the full JetBrains ecosystem (IntelliJ IDEA, PyCharm, WebStorm, GoLand, CLion, Rider, PhpStorm, RubyMine, and more)  
  **全 IDE 支持** — 兼容整个 JetBrains 生态系统（IntelliJ IDEA、PyCharm、WebStorm、GoLand、CLion、Rider、PhpStorm、RubyMine 等）

---

## Installation / 安装

**From JetBrains Marketplace (recommended) / 从 JetBrains 市场安装（推荐）：**

1. Go to `Settings → Plugins → Marketplace` / 进入 `设置 → 插件 → 市场`
2. Search for `DeepSeek-Sync` / 搜索 `DeepSeek-Sync`
3. Click Install / 点击安装
4. Restart your IDE / 重启 IDE

**Manual installation / 手动安装：**

1. Download the `.jar` file from [Releases](https://github.com/KLTaekwondo/DeepSeek-Sync/releases) / 从 Releases 下载 `.jar` 文件
2. Go to `Settings → Plugins → ⚙️ → Install Plugin from Disk...` / 进入 `设置 → 插件 → ⚙️ → 从磁盘安装插件...`
3. Select the downloaded `.jar` file / 选择下载的 `.jar` 文件
4. Restart your IDE / 重启 IDE

---

## Usage / 使用说明

1. After installation, locate the **DeepSeek-Sync icon** in the right sidebar, below the file tree buttons (or click `View → Tool Windows → DeepSeek-Sync`)  
   安装后，在**右侧边栏**找到 **DeepSeek-Sync 图标**，位置在文件树按钮下方（或点击 `视图 → 工具窗口 → DeepSeek-Sync`）

2. Click to open the tool window / 点击打开工具窗口

3. Use the icon buttons in the tool window title bar to switch between pages / 使用工具窗口标题栏图标按钮切换页面：
   - **Chat** — DeepSeek web chat directly in your IDE / **聊天** — IDE 内直接进行 DeepSeek 网页对话
   - **Platform** — Platform page to check API Key usage and credits / **控制台** — 查看 API Key 使用情况和剩余额度
   - **Custom** — Browse any URL with back/forward navigation / **自定义** — 浏览任意网页，支持前进后退

4. **Code Actions** — Select code in any file, right-click → `Send to DeepSeek Sync` → choose an action  
   **代码操作** — 在任意文件中选中代码，右键 → `发送到 DeepSeek Sync` → 选择操作

5. **Customize Prompts** — Go to `Settings → Tools → DeepSeek-Sync` to edit labels and templates  
   **自定义提示词** — 进入 `设置 → 工具 → DeepSeek-Sync` 编辑标签和模板

6. Click the **Refresh** button anytime to reload the current page / 随时点击**刷新按钮**重新加载当前页面

---

## Prompt Templates / 提示词模板

The plugin ships with 5 default code-action slots. Each has a configurable label and template using these variables:

| Variable | Description |
|----------|-------------|
| `{selection}` | The selected code / 选中的代码 |
| `{file}` | Current file name / 当前文件名 |
| `{class}` | Enclosing class name / 所在类名 |
| `{method}` | Enclosing method name / 所在方法名 |
| `{package}` | Package name / 包名 |
| `{imports}` | File imports / 文件导入语句 |

Default template format: `{file} | {class}.{method} | {package}\n{imports}\n\n{prompt}\n\n{selection}`

---

## Development / 开发构建

Built with [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).

基于 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template) 构建。

```bash
# Clone the project / 克隆项目
git clone https://github.com/KLTaekwondo/DeepSeek-Sync.git

# Build the plugin / 构建插件
./gradlew buildPlugin

# Run sandbox IDE for testing / 运行沙盒 IDE 进行测试
./gradlew runIde
```

---

## Status / 维护状态

Core features are complete. This plugin is in **maintenance mode** — only bug fixes going forward.  
核心功能已完成，插件进入**维护模式** — 后续仅修复 bug，不再添加新功能。
