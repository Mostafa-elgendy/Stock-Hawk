package com.mostafa.stock_hawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.mostafa.stock_hawk.R;
import com.mostafa.stock_hawk.data.QuoteColumns;
import com.mostafa.stock_hawk.data.QuoteProvider;
import com.mostafa.stock_hawk.rest.QuoteCursorAdapter;
import com.mostafa.stock_hawk.rest.RecyclerViewItemClickListener;
import com.mostafa.stock_hawk.rest.Utils;
import com.mostafa.stock_hawk.service.StockIntentService;
import com.mostafa.stock_hawk.service.StockTaskService;
import com.mostafa.stock_hawk.touch_helper.SimpleItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CURSOR_LOADER_ID = 0;

    private Context context;
    private CharSequence title;
    private TextView textMessage;
    private Intent serviceIntent;
    private QuoteCursorAdapter cursorAdapter;

    // Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        textMessage = (TextView) findViewById(R.id.text_message);
        /* The intent service is for executing immediate pulls from the Yahoo API
           GCMTaskService can only schedule tasks, they cannot execute immediately*/
        serviceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            serviceIntent.putExtra("tag", "init");
            if (isConnected()) {
                startService(serviceIntent);
            }
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        cursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        String symbol = ((TextView) v.findViewById(R.id.stock_symbol)).getText().toString();
                        Intent intent = new Intent(context, GraphActivity.class);
                        intent.putExtra("symbol", symbol);
                        startActivity(intent);
                    }
                }));
        cursorAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                refreshLayout();
            }
        });
        recyclerView.setAdapter(cursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    new MaterialDialog.Builder(context).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    /* On FAB click, receive user input. Make sure the stock doesn't already exist
                                       in the DB and proceed accordingly*/
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString().toUpperCase()}, null);
                                    if (c.getCount() > 0) {
                                        String symbol = input.toString().toUpperCase();
                                        Intent intent = new Intent(context, GraphActivity.class);
                                        intent.putExtra("symbol", symbol);
                                        startActivity(intent);
                                    } else {
                                        // Add the stock to DB
                                        serviceIntent.putExtra("tag", "add");
                                        serviceIntent.putExtra("symbol", input.toString().toUpperCase());
                                        startService(serviceIntent);
                                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.stock_saved), Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(cursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        title = getTitle();
        if (isConnected()) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            /* create a periodic task to pull stocks once every hour after the app has been opened.
               This is so Widget data stays up to date.*/
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            /* Schedule task with tag "periodic."
               This ensure that only the stocks present in the DB are updated.
             */
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

        // Setup UI
        refreshLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        //getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    // Helper Methods
    public void refreshLayout() {
        if (isConnected()) {
            if (cursorAdapter.getItemCount() == 0) {
                textMessage.setText(R.string.none_selected);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
            }
        } else {
            if (cursorAdapter.getItemCount() == 0) {
                textMessage.setText(R.string.network_toast);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
                Toast.makeText(this, R.string.app_offline, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void networkToast() {
        Toast.makeText(context, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting());
    }

    // ActionBar
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle action bar item clicks here.
           The action bar will automatically handle clicks on the Home/Up button,
           so long as you specify a parent activity in AndroidManifest.xml.
         */
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    // Cursor Loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }

}