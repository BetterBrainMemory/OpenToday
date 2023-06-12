package com.fazziclay.opentoday.gui.fragment;

import static com.fazziclay.opentoday.util.InlineUtil.viewClick;
import static com.fazziclay.opentoday.util.InlineUtil.viewVisible;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fazziclay.opentoday.Debug;
import com.fazziclay.opentoday.R;
import com.fazziclay.opentoday.app.App;
import com.fazziclay.opentoday.app.items.ItemsRoot;
import com.fazziclay.opentoday.app.items.item.LongTextItem;
import com.fazziclay.opentoday.app.items.item.TextItem;
import com.fazziclay.opentoday.databinding.FragmentItemTextEditorBinding;
import com.fazziclay.opentoday.gui.ActivitySettings;
import com.fazziclay.opentoday.gui.ColorPicker;
import com.fazziclay.opentoday.gui.UI;
import com.fazziclay.opentoday.util.ColorUtil;
import com.fazziclay.opentoday.util.MinTextWatcher;
import com.fazziclay.opentoday.util.ResUtil;

import java.util.UUID;

public class ItemTextEditorFragment extends Fragment {
    public static final int EDITABLE_TYPE_TEXT = 0;
    public static final int EDITABLE_TYPE_LONG_TEXT = 1;
    public static final int EDITABLE_TYPE_AUTO = 2;

    private static final String KEY_ID = "ItemTextEditorFragment_itemId";
    private static final String KEY_EDITABLE_TYPE = "ItemTextEditorFragment_editableType";
    private static final String TAG = "ItemTextEditorFragment";
    private int systemStart;
    private int systemEnd;
    private String system;

    public static ItemTextEditorFragment create(UUID id) {
        return create(id, EDITABLE_TYPE_AUTO);
    }

    public static ItemTextEditorFragment create(UUID id, int editableType) {
        ItemTextEditorFragment f = new ItemTextEditorFragment();

        Bundle args = new Bundle();
        args.putString(KEY_ID, id.toString());
        args.putInt(KEY_EDITABLE_TYPE, editableType);

        f.setArguments(args);

        return f;
    }

    private FragmentItemTextEditorBinding binding;
    private MenuItem previewMenuItem = null;
    private TextItem item;
    private boolean isLongText;
    private boolean showPreview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ItemsRoot itemsRoot = App.get(requireContext()).getItemsRoot();

        binding = FragmentItemTextEditorBinding.inflate(getLayoutInflater());
        UUID id = UUID.fromString(getArguments().getString(KEY_ID));
        int editableType = getArguments().getInt(KEY_EDITABLE_TYPE);
        item = (TextItem) itemsRoot.getItemById(id);
        if (item == null) throw new RuntimeException("Item not found in ItemsRoot by provided UUID");
        if (editableType == EDITABLE_TYPE_AUTO || editableType == EDITABLE_TYPE_LONG_TEXT) {
            isLongText = true;
        } else if (editableType == EDITABLE_TYPE_TEXT) {
            isLongText = false;
        }
        if (!(item instanceof LongTextItem)) isLongText = false;
        setupView();
        UI.getUIRoot(this).pushActivitySettings(a -> {
            a.setNotificationsVisible(false);
            a.setClockVisible(false);
            // TODO: 09.06.2023 make translatable
            a.setToolbarSettings(
                    ActivitySettings.ToolbarSettings.createBack(isLongText ? "Edit long text" : "Edit text", () -> UI.rootBack(this))
                            .setMenu(R.menu.menu_item_text_editor, menu -> {
                                MenuItem preview = previewMenuItem = menu.findItem(R.id.previewFormatting);
                                preview.setChecked(showPreview);
                                preview.setOnMenuItemClickListener(menuItem -> {
                                    menuItem.setChecked(!showPreview);
                                    setShowPreview(!showPreview);
                                    return true;
                                });

                                MenuItem formattingHelp = menu.findItem(R.id.helpFormatting);
                                formattingHelp.setOnMenuItemClickListener(menuItem -> {
                                    openFormattingHelp();
                                    return true;
                                });
                            })
            );
        });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.getUIRoot(this).popActivitySettings();
    }

    private void openFormattingHelp() {
        // TODO: 09.06.2023 create
    }

    private String getEditableText() {
        if (isLongText && item instanceof LongTextItem l) {
            return l.getLongText();
        }
        return item.getText();
    }

    private int getEditableTextColor() {
        if (isLongText && item instanceof LongTextItem l) {
            if (!l.isCustomLongTextColor()) return ResUtil.getAttrColor(requireContext(), R.attr.item_text_textColor);
            return l.getLongTextColor();
        }
        if (!item.isCustomTextColor()) return ResUtil.getAttrColor(requireContext(), R.attr.item_text_textColor);
        return item.getTextColor();
    }

    private void setEditableText(String s) {
        if (isLongText && item instanceof LongTextItem l) {
            l.setLongText(s);
        } else {
            item.setText(s);
        }
        item.visibleChanged();
        item.save();
    }

    private void setupView() {
        viewClick(binding.apply, () -> {
            setEditableText(binding.editText.getText().toString());
            UI.rootBack(this);
        });
        binding.editText.setText(getEditableText());
        MinTextWatcher.after(binding.editText, this::updatePreview);
        updateCurrentSystem(0);
        binding.editText.setOnSelectionChangedListener((view, start, end) -> {
            if (start == end) updateCurrentSystem(start);
            updatePreview();
        });
        setShowPreview(item.isParagraphColorize() && getEditableText().contains("$"));
        if (item.isViewCustomBackgroundColor()) binding.formattingPreview.setBackgroundColor(item.getViewBackgroundColor());

        viewClick(binding.addSystem, () -> putText(binding.editText.getSelectionStart(), "$[]"));
        viewClick(binding.deleteSystem, this::clearCurrentSystem);

        // TODO: 12.06.2023 make translatable
        viewClick(binding.foregroundColor, () -> new ColorPicker(requireContext(), getSystemColorValue("-"))
                .setting(true, true, true)
                .setNeutralDialogButton("Reset", () -> resetSystem("-"))
                .showDialog("Text color", "No", "Yes", (color) -> setSystemValue("-", ColorUtil.colorToHex(color))));

        viewClick(binding.backgroundSystem, () -> new ColorPicker(requireContext(), getSystemColorValue("="))
                .setting(true, true, true)
                .setNeutralDialogButton("Reset", () -> resetSystem("="))
                .showDialog("Background", "No", "Yes", (color) -> setSystemValue("=", ColorUtil.colorToHex(color))));
    }

    private void updateCurrentSystem(int start) {
        String s = binding.editText.getText().toString();
        systemStart = -1;
        systemEnd = -1;

        int i = start-1;
        while (i >= 0) {
            if (s.charAt(i) == ']') break;
            if (s.charAt(i) == '[') {
                if (i-1 >= 0 && s.charAt(i-1) == '$') {
                    systemStart = i;
                    break;
                }
            }
            i--;
        }

        i = start;
        while (i < s.length()) {
            if (s.charAt(i) == '$') break;
            if (s.charAt(i) == ']') {
                systemEnd = i;
                break;
            }
            i++;
        }

        if (systemStart >= 0 && systemEnd >= 0) {
            system = s.substring(systemStart, systemEnd);
            if (!system.isEmpty()) system = system.substring(1);
        } else {
            system = null;
        }

        Debug.itemtexteditor = String.format("SysSTART=%s; SysEND=%s s=%s", systemStart, systemEnd, system);

        viewVisible(binding.addSystem, !isSystem(), View.GONE);
        viewVisible(binding.deleteSystem, isSystem(), View.GONE);

        binding.foregroundColor.setBackgroundTintList(ColorStateList.valueOf(getSystemColorValue("-")));
        viewVisible(binding.foregroundColor, isSystem(), View.GONE);
        binding.backgroundSystem.setBackgroundTintList(ColorStateList.valueOf(getSystemColorValue("=")));
        viewVisible(binding.backgroundSystem, isSystem(), View.GONE);
    }

    private int getSystemColorValue(String chas) {
        if (system == null) return Color.TRANSPARENT;
        for (String s : system.split(";")) {
            if (s.startsWith(chas) && s.length() > 1) {
                String colorValue = s.substring(1);
                try {
                    return Color.parseColor(colorValue);
                } catch (Exception e) {
                    return 0xFFFF00FF;
                }
            }
        }
        return Color.TRANSPARENT;
    }

    private void setSystemValue(String c, String val) {
        if (system == null || c.isEmpty()) return;
        editSystem(ColorUtil.sysSet(system, c.charAt(0), val));
    }

    private void resetSystem(String c) {
        if (system == null || c.isEmpty()) return;
        editSystem(ColorUtil.sysReset(system, c.charAt(0)));
    }

    private void editSystem(String s) {
        if (system == null) return;
        String first = binding.editText.getText().toString().substring(0, systemStart);
        String end = binding.editText.getText().toString().substring(systemEnd);


        int pos = systemStart + 1;
        binding.editText.setText(first + "[" +s + end);
        binding.editText.setSelection(pos);
    }

    private void putText(int pos, String text) {
        String first = binding.editText.getText().toString().substring(0, pos);
        String end = binding.editText.getText().toString().substring(pos);


        binding.editText.setText(first + text + end);
        binding.editText.setSelection(pos + text.length()-1);
    }

    private void clearCurrentSystem() {
        if (system == null) return;
        String first = binding.editText.getText().toString().substring(0, systemStart-1);
        String end = binding.editText.getText().toString().substring(systemEnd+1);


        int pos = systemStart - 1;
        binding.editText.setText(first + end);
        binding.editText.setSelection(pos);
    }

    private void updatePreview() {
        String s = binding.editText.getText().toString();
        binding.editText.setTextSize(s.length()>100 ? 15 : 20);

        if (!showPreview) return;
        int i = binding.editText.getSelectionStart();
        int line = getLinePosition(s, i);
        binding.formattingPreview.setText(ColorUtil.colorize(lineArea(s, line), getEditableTextColor(), Color.TRANSPARENT, Typeface.NORMAL));
    }

    private void setShowPreview(boolean b) {
        this.showPreview = b;
        viewVisible(binding.formattingPreview, b, View.GONE);
        updatePreview();
        if (previewMenuItem != null) previewMenuItem.setChecked(b);
    }

    private boolean isSystemXExist(String anChar) {
        if (system == null) return false;
        for (String s : system.split(";")) {
            if (s.startsWith(anChar)) return true;
        }
        return false;
    }

    private int getLinePosition(String s, int charPos) {
        if (charPos > s.length()) throw new IndexOutOfBoundsException("charPos index out bounds provided string");
        if (charPos < 0) throw new IllegalArgumentException("charPos can't be negative");
        String[] lines = s.split("\n");
        int length = 0;
        int i = 0;
        for (String line : lines) {
            length += line.length()+1; // +1 for \n symbol
            if (length >= charPos) return i;
            i++;
        }
        return 0;
    }

    private int getLines(String s) {
        return s.split("\n").length;
    }

    private String lineArea(final String s, final int line) {
        if (line < 0) throw new IndexOutOfBoundsException("argument line can't be negative");
        final String[] lines = s.split("\n");

        return concatLines(
                getLineIfExist(lines, line-2),
                getLineIfExist(lines, line-1),
                getLineIfExist(lines, line),
                getLineIfExist(lines, line+1),
                getLineIfExist(lines, line+2)
        );
    }

    private String getLineIfExist(String[] lines, int line) {
        if (line < 0) return null;
        if (line >= lines.length) return null;
        return lines[line];
    }

    private String concatLines(String... lines) {
        StringBuilder b = new StringBuilder();

        for (String line : lines) {
            if (line == null) continue;
            b.append(line).append("\n");
        }

        return b.substring(0, b.lastIndexOf("\n"));
    }


    public boolean isSystem() {
        return system != null;
    }
}