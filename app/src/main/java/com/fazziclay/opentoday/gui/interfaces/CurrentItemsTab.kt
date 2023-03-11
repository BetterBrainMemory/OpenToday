package com.fazziclay.opentoday.gui.interfaces

import com.fazziclay.opentoday.app.items.tab.Tab
import java.util.*

interface CurrentItemsTab {
    fun getCurrentTabId(): UUID
    fun getCurrentTab(): Tab
    fun setCurrentTab(id: UUID?)
}