package com.betterbrainmemory.opentoday.gui.item.renderer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.betterbrainmemory.opentoday.R;
import com.betterbrainmemory.opentoday.app.items.item.SleepTimeItem;
import com.betterbrainmemory.opentoday.databinding.ItemSleepTimeBinding;
import com.betterbrainmemory.opentoday.gui.interfaces.ItemInterface;
import com.betterbrainmemory.opentoday.gui.item.ItemViewGenerator;
import com.betterbrainmemory.opentoday.gui.item.ItemViewGeneratorBehavior;
import com.betterbrainmemory.opentoday.gui.item.registry.ItemRenderer;
import com.betterbrainmemory.opentoday.gui.item.registry.NameResolver;
import com.betterbrainmemory.opentoday.util.Destroyer;
import com.betterbrainmemory.opentoday.util.time.ConvertMode;
import com.betterbrainmemory.opentoday.util.time.TimeUtil;

import org.jetbrains.annotations.NotNull;

public class SleepTimeItemRenderer implements NameResolver, ItemRenderer<SleepTimeItem> {
    @Override
    public String resolveName(Context context) {
        return context.getString(R.string.item_sleepTime);
    }

    @Override
    public String resolveDescription(Context context) {
        return context.getString(R.string.item_sleepTime_description);
    }

    @Override
    public View render(SleepTimeItem item, Activity context, LayoutInflater layoutInflater, ViewGroup parent, @NotNull ItemViewGeneratorBehavior behavior, @NotNull ItemInterface onItemClick, ItemViewGenerator itemViewGenerator, boolean previewMode, @NotNull Destroyer destroyer) {
        final ItemSleepTimeBinding binding = ItemSleepTimeBinding.inflate(layoutInflater, parent, false);

        TextItemRenderer.applyTextItemToTextView(context, item, binding.title, behavior, destroyer, previewMode);
        ItemViewGenerator.applyItemNotificationIndicator(context, item, binding.indicatorNotification, behavior, destroyer, previewMode);

        final String s = item.getSleepTextPattern()
                .replace("$(elapsed)", TimeUtil.convertToHumanTime(item.getElapsedToWakeupTime(), ConvertMode.HHMM))
                .replace("$(elapsedToStartSleep)", TimeUtil.convertToHumanTime(item.getElapsedTimeToStartSleep(), ConvertMode.HHMM))
                .replace("$(current)", TimeUtil.convertToHumanTime(TimeUtil.getDaySeconds(), ConvertMode.HHMM))
                .replace("$(wakeUpForRequired)", TimeUtil.convertToHumanTime(item.getWakeupAtCurrent(), ConvertMode.HHMM))
                .replace("$(wakeUpTime)", TimeUtil.convertToHumanTime(item.getWakeup(), ConvertMode.HHMM))
                .replace("$(requiredSleepTime)", TimeUtil.convertToHumanTime(item.getDuration(), ConvertMode.HHMM));

        binding.description.setText(ItemViewGenerator.colorize(s, item.getTextColor()));

        return binding.getRoot();
    }
}