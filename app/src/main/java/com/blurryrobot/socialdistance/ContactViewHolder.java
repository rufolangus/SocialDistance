package com.blurryrobot.socialdistance;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class ContactViewHolder extends RecyclerView.ViewHolder {

    private View view;

    ContactViewHolder(View itemView) {
        super(itemView);
        view = itemView;
    }

    void setDistance(String distance) {
        TextView distanceView = view.findViewById(R.id.distance_msg);
        distanceView.setText(distance);
    }

    void setTime(String timestamp) {
        TextView timestampView = view.findViewById(R.id.timestamp_field);
        timestampView.setText(timestamp);
    }
}
