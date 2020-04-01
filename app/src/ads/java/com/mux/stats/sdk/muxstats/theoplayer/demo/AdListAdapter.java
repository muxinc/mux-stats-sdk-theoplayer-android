package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class AdListAdapter extends BaseAdapter {

    Context context;
    ArrayList<AdSample> adSamples;
    ListView parent;

    public AdListAdapter(Context context, ArrayList<AdSample> adSamples, ListView parent) {
        this.parent = parent;
        this.context = context;
        this.adSamples = adSamples;
    }

    @Override
    public int getCount() {
        return adSamples.size();
    }

    @Override
    public Object getItem(int position) {
        return adSamples.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tittle;
        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(R.layout.ad_list_row_layout, parent, false);
        tittle = convertView.findViewById(R.id.row_tittle);
        AdSample ad = adSamples.get(position);
        tittle.setText(ad.getName());
        return convertView;
    }
}
