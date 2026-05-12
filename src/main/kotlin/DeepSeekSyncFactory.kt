package com.kldo

import androidx.compose.runtime.LaunchedEffect
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab

class DeepSeekSyncFactory: ToolWindowFactory {
    override fun  shouldBeAvailable(project: Project) = true
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.RIGHT ,null)
        toolWindow.addComposeTab("DeepSeek Web Chat", focusOnClickInside = true) {
            LaunchedEffect(Unit) {
                // initial data loading
            }
            DeepSeekWebPreview()
        }

    }
}


