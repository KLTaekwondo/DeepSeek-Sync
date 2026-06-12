package com.kldo

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

/**
 * 「DeepSeek AI」二级菜单的 ActionGroup。
 * 只负责弹出子菜单，可见性和启用状态由各子项自行控制。
 */
class DeepSeekAIActionGroup : DefaultActionGroup(null, true), DumbAware
