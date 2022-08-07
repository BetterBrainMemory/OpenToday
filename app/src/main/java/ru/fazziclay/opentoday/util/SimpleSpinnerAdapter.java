package ru.fazziclay.opentoday.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SimpleSpinnerAdapter<T> extends MinBaseAdapter {
    private final Context context;
    private final ViewStyle viewStyle;
    private final List<Set> setList = new ArrayList<>();

    public SimpleSpinnerAdapter(Context context, ViewStyle viewStyle) {
        this.context = context;
        this.viewStyle = viewStyle;
    }

    public SimpleSpinnerAdapter(Context context) {
        this(context, (string, convertedView, parent) -> createViewFromResource(string, LayoutInflater.from(context), convertedView, parent, android.R.layout.simple_dropdown_item_1line));
    }

    public SimpleSpinnerAdapter<T> add(String text, T value) {
        setList.add(new Set(text, value));
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getCount() {
        return setList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Set set = setList.get(position);
        return viewStyle.create(set.text, convertView, parent);
    }

    @Override
    public T getItem(int position) {
        Set set = setList.get(position);
        return set.value;
    }

    public String getItemText(int position) {
        Set set = setList.get(position);
        return set.text;
    }

    public int getValuePosition(T value) {
        for (Set set : setList) {
            if (set.value.equals(value)) {
                return setList.indexOf(set);
            }
        }
        return -1;
    }

    private class Set {
        private final String text;
        private final T value;

        public Set(String text, T value) {
            this.text = text;
            this.value = value;
        }
    }

    public static View createViewFromResource(String text, LayoutInflater inflater, View convertView, ViewGroup parent, int resource) {
        final View view;
        final TextView textView;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }
        textView = (TextView) view;

        textView.setText(text);
        return view;
    }

    private interface ViewStyle {
        View create(String string, View convertView, ViewGroup parent);
    }
}
