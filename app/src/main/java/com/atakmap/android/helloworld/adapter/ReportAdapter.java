package com.atakmap.android.helloworld.adapter;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.SquireMapComponent;
import com.atakmap.android.helloworld.models.Report;
import com.atakmap.android.helloworld.plugin.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private static final String TAG = "ReportAdapter";
    private Callable<List<Report>> getLatestDataset;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView descTextView;
        private Report report;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // click listener would go here when im ready for it

            titleTextView = itemView.findViewById(R.id.polar_device_title);
            descTextView = itemView.findViewById(R.id.polar_device_desc);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "report Clicked.");
                    String message = SquireMapComponent.submissionToMessage(report);
                    SquireDropDownReceiver.showReport(message);
                    SquireDropDownReceiver.closeReportDialog();
                }
            };

            titleTextView.setClickable(true);
            descTextView.setClickable(true);
            titleTextView.setOnClickListener(listener);
            descTextView.setOnClickListener(listener);
        }
        public TextView getDescTextView() {
            return descTextView;
        }
        public TextView getTitleTextView() {
            return titleTextView;
        }
        public void setReport(Report report) {
            this.report = report;
        }
    }

    public ReportAdapter(Callable<List<Report>> getLatestDataset) {
        this.getLatestDataset = getLatestDataset;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(SquireDropDownReceiver.deviceAdapterView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<Report> dataset;
        try {
            dataset = getLatestDataset.call();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Report report = dataset.get(position);
        holder.getTitleTextView().setText(report.nineline.getLocation());
        holder.getDescTextView().setText(report.nineline.getDateTime());
        holder.setReport(report);
    }

    @Override
    public int getItemCount() {
        List<Report> dataset = null;
        try {
            dataset = getLatestDataset.call();
        } catch (Exception e) {
            e.printStackTrace();
            dataset = new ArrayList<>();
        }

        return dataset != null ? dataset.size() : 0;
    }
}
