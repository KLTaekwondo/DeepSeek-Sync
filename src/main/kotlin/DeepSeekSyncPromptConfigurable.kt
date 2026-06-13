package com.kldo

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DeepSeekSyncPromptConfigurable : Configurable {

    private val fields = mutableListOf<SlotFields>()
    private var slotList: JBList<String>? = null
    private val listModel = DefaultListModel<String>()
    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)
    private var selectedIndex = 0

    private class SlotFields(val index: Int) {
        val labelField = JBTextField()
        val templateArea = JTextArea(8, 35).apply {
            lineWrap = true
            wrapStyleWord = true
        }
    }

    override fun createComponent(): JComponent {
        val slots = DeepSeekSyncPromptSettings.getSlots()
        fields.clear()
        listModel.clear()
        cardPanel.removeAll()

        slots.forEachIndexed { i, slot ->
            val sf = SlotFields(i).also { fields.add(it) }
            sf.labelField.text = slot.label
            sf.templateArea.text = slot.template
            listModel.addElement(slot.label.ifBlank { "Prompt #${i + 1}" })
            cardPanel.add(buildSlotCard(i, sf), "slot_$i")
        }

        // 左侧列表
        slotList = JBList(listModel).apply {
            fixedCellHeight = 32
            preferredSize = Dimension(170, 0)
            selectedIndex = 0
            addListSelectionListener { e ->
                if (e.valueIsAdjusting) return@addListSelectionListener
                val newIdx = this@apply.selectedIndex
                if (newIdx < 0 || newIdx == this@DeepSeekSyncPromptConfigurable.selectedIndex)
                    return@addListSelectionListener
                this@DeepSeekSyncPromptConfigurable.selectedIndex = newIdx
                cardLayout.show(cardPanel, "slot_$newIdx")
            }
        }

        cardLayout.show(cardPanel, "slot_0")

        val splitter = Splitter(false, 0.23f).apply {
            firstComponent = JBScrollPane(slotList)
            secondComponent = cardPanel
        }

        return splitter
    }

    private fun buildSlotCard(index: Int, sf: SlotFields): JComponent {
        val panel = JPanel(GridBagLayout())
        val c = GridBagConstraints()
        var row = 0

        c.gridx = 0; c.gridy = row; c.gridwidth = 2
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0
        c.insets = Insets(8, 8, 0, 8)
        panel.add(JLabel("<html><b>" +
                MyMessageBundle.message("settings.prompt.number", index + 1) +
                "</b></html>"), c)

        row++
        c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE
        c.weightx = 0.0; c.insets = Insets(8, 8, 2, 5)
        panel.add(JLabel(MyMessageBundle.message("settings.label")), c)

        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0; c.insets = Insets(8, 0, 2, 8)
        panel.add(sf.labelField, c)

        row++
        c.gridx = 0; c.gridy = row; c.fill = GridBagConstraints.NONE
        c.weightx = 0.0; c.anchor = GridBagConstraints.NORTHWEST
        c.insets = Insets(4, 8, 2, 5)
        panel.add(JLabel(MyMessageBundle.message("settings.template")), c)

        c.gridx = 1; c.fill = GridBagConstraints.BOTH
        c.weightx = 1.0; c.weighty = 1.0; c.insets = Insets(4, 0, 2, 8)
        panel.add(JScrollPane(sf.templateArea), c)

        row++
        c.gridx = 0; c.gridy = row; c.gridwidth = 2
        c.fill = GridBagConstraints.BOTH; c.weightx = 1.0; c.weighty = 0.0
        c.insets = Insets(8, 8, 2, 8)
        val varModel = object : DefaultTableModel(
            arrayOf<Any>(
                MyMessageBundle.message("settings.variables.column.name"),
                MyMessageBundle.message("settings.variables.column.description")
            ), 0) {
            override fun isCellEditable(row: Int, col: Int) = false
        }
        listOf(
            arrayOf<Any>("{selection}", MyMessageBundle.message("settings.variable.desc.selection")),
            arrayOf<Any>("{file}", MyMessageBundle.message("settings.variable.desc.file")),
            arrayOf<Any>("{class}", MyMessageBundle.message("settings.variable.desc.class")),
            arrayOf<Any>("{method}", MyMessageBundle.message("settings.variable.desc.method")),
            arrayOf<Any>("{package}", MyMessageBundle.message("settings.variable.desc.package")),
            arrayOf<Any>("{imports}", MyMessageBundle.message("settings.variable.desc.imports")),
        ).forEach { varModel.addRow(it) }
        val varTable = JBTable(varModel).apply {
            setShowGrid(false)
            tableHeader = null
            rowHeight = 36
            columnModel.getColumn(0).apply { preferredWidth = 100; maxWidth = 120 }
            columnModel.getColumn(1).apply { preferredWidth = 220 }
        }
        panel.add(JBScrollPane(varTable).also {
            it.border = BorderFactory.createTitledBorder(
                MyMessageBundle.message("settings.variables.title"))
        }, c)

        row++
        c.gridy = row; c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.WEST; c.insets = Insets(4, 8, 2, 8)
        panel.add(JButton(MyMessageBundle.message("settings.reset")).also { btn ->
            btn.addActionListener { resetToDefaults() }
        }, c)

        row++
        c.gridy = row; c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(2, 8, 8, 8)
        panel.add(JLabel(MyMessageBundle.message("settings.maintenance.notice")).also {
            it.foreground = UIManager.getColor("Label.disabledForeground")
                ?: java.awt.Color.GRAY
            it.font = it.font.deriveFont(it.font.size * 0.9f)
        }, c)

        row++
        c.gridy = row; c.weighty = 1.0; c.fill = GridBagConstraints.VERTICAL
        panel.add(Box.createVerticalGlue(), c)

        return JBScrollPane(panel)
    }

    override fun isModified(): Boolean {
        val current = DeepSeekSyncPromptSettings.getSlots()
        return fields.withIndex().any { (i, f) ->
            val slot = current.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text != slot.label || f.templateArea.text != slot.template
        }
    }

    override fun apply() {
        DeepSeekSyncPromptSettings.setSlots(
            fields.map { PromptSlot(it.labelField.text, it.templateArea.text) }
        )
        fields.forEachIndexed { i, f ->
            if (i < listModel.size)
                listModel.setElementAt(f.labelField.text.ifBlank { "Prompt #${i + 1}" }, i)
        }
    }

    override fun reset() {
        val current = DeepSeekSyncPromptSettings.getSlots()
        fields.forEachIndexed { i, f ->
            val slot = current.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text = slot.label
            f.templateArea.text = slot.template
            if (i < listModel.size)
                listModel.setElementAt(slot.label.ifBlank { "Prompt #${i + 1}" }, i)
        }
    }

    override fun getDisplayName(): String = "DeepSeek-Sync"

    private fun resetToDefaults() {
        DeepSeekSyncPromptSettings.resetDefaults()
        val defaults = DeepSeekSyncPromptSettings.getSlots()
        fields.forEachIndexed { i, f ->
            val slot = defaults.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text = slot.label
            f.templateArea.text = slot.template
            if (i < listModel.size)
                listModel.setElementAt(slot.label.ifBlank { "Prompt #${i + 1}" }, i)
        }
    }
}
