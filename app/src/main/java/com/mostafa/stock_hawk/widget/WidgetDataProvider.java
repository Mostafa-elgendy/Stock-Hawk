package com.mostafa.stock_hawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mostafa.stock_hawk.R;
import com.mostafa.stock_hawk.data.QuoteColumns;
import com.mostafa.stock_hawk.data.QuoteProvider;

/**
 * Created by mostafa on 24/04/17.
 */

public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {


    Context context;
    Intent intent;
    private Cursor cursor;
    private int appWidgetId;

    public WidgetDataProvider(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    //private Cursor cursor;
    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
        initData();
    }

    public void onDestroy() {
        if (cursor != null) {
            cursor.close();
        }
    }

    public int getCount() {
        return cursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        String symbol = "";
        String bidPrice = "";
        String change = "";
        int isUp = 1;
        if (cursor.moveToPosition(position)) {
            Log.e("enter", "enter");
            final int symbolColIndex = cursor.getColumnIndex(QuoteColumns.SYMBOL);
            final int bidPriceColIndex = cursor.getColumnIndex(QuoteColumns.BIDPRICE);
            final int changeColIndex = cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
            final int isUpIndex = cursor.getColumnIndex(QuoteColumns.ISUP);
            symbol = cursor.getString(symbolColIndex);
            bidPrice = cursor.getString(bidPriceColIndex);
            change = cursor.getString(changeColIndex);
            isUp = cursor.getInt(isUpIndex);
            Log.e("enter", "enter" + symbol);
        }

        // Fill data in UI
        final int itemId = R.layout.widget_collection_item;
        RemoteViews rv = new RemoteViews(context.getPackageName(), itemId);
        rv.setTextViewText(R.id.stock_symbol, symbol);
        rv.setTextViewText(R.id.bid_price, bidPrice);
        rv.setTextViewText(R.id.change, change);
        if (isUp == 1) {
            rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
        } else {
            rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
        }

        return rv;
    }

    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        initData();

    }

    private void initData() {
        if (cursor != null) {
            cursor.close();
        }
        cursor = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }
}
