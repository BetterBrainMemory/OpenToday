package com.fazziclay.opentoday.app.items.tab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fazziclay.opentoday.app.data.Cherry;
import com.fazziclay.opentoday.app.items.ItemsStorage;
import com.fazziclay.opentoday.app.items.Unique;
import com.fazziclay.opentoday.util.annotation.RequireSave;
import com.fazziclay.opentoday.util.annotation.SaveKey;

import java.util.UUID;

public abstract class Tab implements ItemsStorage, Unique {
    protected static class TabCodec extends AbstractTabCodec {
        private static final String KEY_ID = "id";
        private static final String KEY_NAME = "name";

        @NonNull
        @Override
        public Cherry exportTab(@NonNull Tab tab) {
            return new Cherry()
                    .put(KEY_NAME, tab.name)
                    .put(KEY_ID, tab.id == null ? null : tab.id.toString());
        }

        @NonNull
        @Override
        public Tab importTab(@NonNull Cherry cherry, @Nullable Tab tab) {
            if (cherry.has(KEY_ID)) tab.id = UUID.fromString(cherry.getString(KEY_ID));
            tab.name = cherry.optString(KEY_NAME, "");
            return tab;
        }
    }

    @RequireSave @SaveKey(key = "id") private UUID id = null;
    @RequireSave @SaveKey(key = "name") private String name = "";
    private TabController controller;

    public Tab(String name) {
        this.name = name;
    }

    public void setController(TabController controller) {
        this.controller = controller;
    }

    public void validateId() {
        if (id == null && controller != null) id = controller.generateId();
    }

    public void attach(TabController controller) {
        this.controller = controller;
        this.id = controller.generateId();
    }

    public void detach() {
        this.controller = null;
        this.id = null;
    }

    protected Tab() {

    }

    @Override
    public UUID getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setName(String text) {
        this.name = text;
        if (controller != null) controller.nameChanged(this);
    }

    @Override
    public void save() {
        if (controller != null) controller.save(this);
    }
}
