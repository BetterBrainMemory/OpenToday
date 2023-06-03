package com.fazziclay.opentoday.app.items.callback;

import androidx.annotation.NonNull;

import com.fazziclay.opentoday.app.items.item.ItemManager;
import com.fazziclay.opentoday.app.items.tab.Tab;
import com.fazziclay.opentoday.util.callback.Callback;
import com.fazziclay.opentoday.util.callback.Status;

/**
 * @see ItemManager#getOnTabsChanged()
 * @see Callback
 */
public interface OnTabsChanged extends Callback {
    Status onTabsChanged(@NonNull final Tab[] tabs);
}
