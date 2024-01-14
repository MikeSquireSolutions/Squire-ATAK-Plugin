package com.atakmap.android.squire.fragment;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.squire.HelloWorldDropDownReceiver;
import com.atakmap.android.squire.adapter.ReportAdapter;
import com.atakmap.android.squire.db.ReportsRepository;
import com.atakmap.android.squire.models.Report;
import com.atakmap.android.squire.plugin.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReportFragment extends DialogFragment {
    private static final String TAG = "ReportFragment";

    private List<Report> reports;
    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private Button submitButton;

    public ReportFragment() {
        Log.d(TAG, "Constructed, not created");
    }

    public static ReportFragment newInstance() {
        return new ReportFragment();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null && args.containsKey("reports")) {
            Gson gson = new Gson();
            Type reportsType = new TypeToken<ArrayList<Report>>(){}.getType();
            //reports = gson.fromJson(args.getString("reports"), reportsType);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        reports = ReportsRepository.getReports();
    }

    @Override
    public void onStart() {
        super.onStart();
        reports = ReportsRepository.getReports();
        redrawRecyclerViews();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = HelloWorldDropDownReceiver.reportFragView;
        reportRecyclerView = view.findViewById(R.id.reports_recyclerview);
        setRecylerViewLayout(reportRecyclerView);
        reportAdapter = new ReportAdapter(this::getDataset);
        reportRecyclerView.setAdapter(reportAdapter);
        reportRecyclerView.setNestedScrollingEnabled(false);
        submitButton = view.findViewById(R.id.reports_submit_button);
        submitButton.setOnClickListener(v -> {
            try {
                HelloWorldDropDownReceiver.submitReportsToAtak.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Window window = getDialog().getWindow();
        if (window != null) {
            window.setTitle("Reports");
            //window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setLayout(1000, 100);
            ////window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            //setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Dialog_NoActionBar);
        }

        return view;
    }

    void setRecylerViewLayout(RecyclerView rView) {
        int scroll = 0;
        LinearLayoutManager currLayoutManager = (LinearLayoutManager) rView.getLayoutManager();

        if (currLayoutManager != null) {
            scroll = currLayoutManager.findFirstCompletelyVisibleItemPosition();
            //layoutManager.scrollToPosition(scroll);
            //return;
        }

        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.getLayoutManager().scrollToPosition(scroll);
    }

    void redrawRecyclerViews() {
        Log.d(TAG, "Redrawing recycler");
        ReportAdapter adapter = (ReportAdapter) reportRecyclerView.getAdapter();
        reportRecyclerView.setAdapter(null);
        reportRecyclerView.setAdapter(adapter);
    }

    public List<Report> getDataset() {
        return reports;
    }

    public void showReport(String msg) {
        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Squire Report")
                    .setMessage(msg)
                    .setPositiveButton("Ok", (dialog, which) -> com.atakmap.coremap.log.Log.d(HelloWorldDropDownReceiver.TAG, "Clicked accept"))
                    .show();
        });
    }
}