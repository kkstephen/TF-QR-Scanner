package com.stephen.core;

import java.util.*;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.stephen.tfscanner.*;

public class EntityAdapter extends ArrayAdapter<TagLabel>{

    private final Activity context;
    private List<TagLabel> _list;
    private int ViewId;
    private LayoutInflater mInflater;

    public EntityAdapter(Activity context, int layoutId, List<TagLabel> data) {
        super(context, layoutId, data);

        this.context = context;
        this.ViewId = layoutId;
        this._list = data;

        mInflater = context.getLayoutInflater();
    }

    @Override
    public TagLabel getItem(int position) {
        return _list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            rowView = mInflater.inflate(ViewId, parent, false);
        }

        TagLabel tag = getItem(position);

        TextView txtCode = (TextView) rowView.findViewById(R.id.list_code);
        txtCode.setText(tag.getCode());

        TextView txtDate = (TextView) rowView.findViewById(R.id.list_date);
        txtDate.setText(StringHelper.ToDateTime(tag.getCreatedate()));

        return rowView;
    }
}
