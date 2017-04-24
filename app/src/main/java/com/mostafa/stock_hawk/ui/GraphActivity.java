package com.mostafa.stock_hawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mostafa.stock_hawk.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class GraphActivity extends AppCompatActivity {

    private final String COMPANY_NAME = "company_name";
    private final String SYMBOL = "symbol";
    private final String VALUES = "values";
    private final String LABELS = "labels";
    private View errorMessage;
    private View progressCircle;
    private LineChart lineChart;
    private boolean isLoaded = false;
    private String companySymbol;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;

    // Activity life cycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        errorMessage = findViewById(R.id.error_message);
        progressCircle = findViewById(R.id.progress_circle);

        lineChart = (LineChart) findViewById(R.id.chart1);
        companySymbol = getIntent().getStringExtra(SYMBOL);
        if (savedInstanceState == null) {
            downloadStockDetails();
        }
    }

    // Save/Restore activity state
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isLoaded) {
            outState.putString(COMPANY_NAME, companyName);
            outState.putStringArrayList(LABELS, labels);

            float[] valuesArray = new float[values.size()];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = values.get(i);
            }
            outState.putFloatArray(VALUES, valuesArray);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(COMPANY_NAME)) {
            isLoaded = true;

            companyName = savedInstanceState.getString(COMPANY_NAME);
            labels = savedInstanceState.getStringArrayList(LABELS);
            values = new ArrayList<>();

            float[] valuesArray = savedInstanceState.getFloatArray(VALUES);
            for (float f : valuesArray) {
                values.add(f);
            }
            // onDownloadCompleted();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    // Home button click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return false;
        }
    }

    // Download and JSON parsing
    private void downloadStockDetails() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + companySymbol + "/chartdata;type=quote;range=5y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        // Trim response string
                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        // Parse JSON
                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }

            @Override
            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }
        });
    }

    private void onDownloadCompleted() {
        GraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(companyName);
                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.GONE);

                ArrayList<Entry> entries = new ArrayList<Entry>();

                ValueLineSeries series = new ValueLineSeries();
                series.setColor(getResources().getColor(R.color.colorAccent));
                for (int i = 0; i < labels.size(); i++) {
                    series.addPoint(new ValueLinePoint(labels.get(i), values.get(i)));
                    entries.add(new Entry(values.get(i), i));
                }

                LineDataSet dataset = new LineDataSet(entries, getResources().getString(R.string.stock_distribution));
                dataset.setDrawCubic(true);
                dataset.setDrawFilled(true);

                LineData data = new LineData(labels, dataset);
                lineChart.setData(data);
                lineChart.animateY(5000);
                lineChart.setDescription(getResources().getString(R.string.stock_distribution));

            }
        });
    }

    private void onDownloadFailed() {
        GraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.GONE);
                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.VISIBLE);
                setTitle(R.string.error);
            }
        });
    }
}

