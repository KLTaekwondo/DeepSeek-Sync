# DeepSeek-Sync

**Version**: 0.1.1
**License**: Apache 2.0

Bring DeepSeek into your IDE — seamless side panel integration with enhanced platform access.

将 DeepSeek 带入你的 IDE — 侧边栏无缝集成，增强平台访问。

---

## Features / 功能特性

- 🌐 **Embedded DeepSeek Web Chat** — Side tool window chat without leaving your IDE  
  **嵌入 DeepSeek 网页聊天** — 侧边栏直接对话，不离开 IDE

- 📊 **Platform Dashboard** — Quick access to `platform.deepseek.com` to view API Key status and remaining credits  
  **平台控制台** — 快速访问 `platform.deepseek.com`，查看 API Key 状态和剩余额度

- 🔄 **Page Refresh** — Easy reload of embedded web pages when needed  
  **页面刷新** — 需要时一键刷新嵌入式网页

- ⚡ **One-Click Access** — Open DeepSeek instantly from the IDE sidebar  
  **一键访问** — 从 IDE 侧边栏即刻打开 DeepSeek

- 🎯 **Stay in Flow** — Get coding answers faster without context switching  
  **保持心流** — 无需切换上下文，更快获取答案

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

1. After installation, locate the **DeepSeek-Sync icon** in the right sidebar (or click `View → Tool Windows → DeepSeek-Sync`)  
   安装后，在**右侧边栏**找到 **DeepSeek-Sync 图标**（或点击 `视图 → 工具窗口 → DeepSeek-Sync`）

2. Click to open the tool window / 点击打开工具窗口

3. Use the built-in tabs: / 使用内置标签页：
   - **Chat** — DeepSeek web chat directly in your IDE / **聊天** — IDE 内直接进行 DeepSeek 网页对话
   - **Dashboard** — Platform page to check API Key usage and credits / **控制台** — 查看 API Key 使用情况和剩余额度

4. Click the **Refresh** button anytime to reload the current page / 随时点击**刷新按钮**重新加载当前页面

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