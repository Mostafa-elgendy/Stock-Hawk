package com.mostafa.stock_hawk.rest;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;

/**
 * Created by mostafa on 18/04/17.
 */

public abstract class CursorRecyclerViewAdapter <VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>{
  private static final String LOG_TAG = CursorRecyclerViewAdapter.class.getSimpleName();
  private Cursor cursor;
  private boolean dataIsValid;
  private int rowIdColumn;
  private DataSetObserver dataSetObserver;
  public CursorRecyclerViewAdapter(Context context, Cursor cursor){
    this.cursor = cursor;
    dataIsValid = cursor != null;
    rowIdColumn = dataIsValid ? this.cursor.getColumnIndex("_id") : -1;
    dataSetObserver = new NotifyingDataSetObserver();
    if (dataIsValid){
      this.cursor.registerDataSetObserver(dataSetObserver);
    }
  }

  public Cursor getCursor(){
    return cursor;
  }

  @Override
  public int getItemCount(){
    if (dataIsValid && cursor != null) {
      return cursor.getCount();
    }
    return 0;
  }

  @Override public long getItemId(int position) {
    if (dataIsValid && cursor != null && cursor.moveToPosition(position)) {
      return cursor.getLong(rowIdColumn);
    }
    return 0;
  }

  @Override public void setHasStableIds(boolean hasStableIds) {
    super.setHasStableIds(true);
  }

  public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

  @Override
  public void onBindViewHolder(VH viewHolder, int position) {
    if (!dataIsValid){
      throw new IllegalStateException("This should only be called when Cursor is valid");
    }
    if (!cursor.moveToPosition(position)) {
      throw new IllegalStateException("Could not move Cursor to position: " + position);
    }

    onBindViewHolder(viewHolder, cursor);
  }

  public Cursor swapCursor(Cursor newCursor){
    if (newCursor == cursor) {
      return null;
    }
    final Cursor oldCursor = cursor;
    if (oldCursor != null && dataSetObserver != null) {
      oldCursor.unregisterDataSetObserver(dataSetObserver);
    }
    cursor = newCursor;
    if (cursor != null) {
      if (dataSetObserver != null) {
        cursor.registerDataSetObserver(dataSetObserver);
      }
      rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
      dataIsValid = true;
      notifyDataSetChanged();
    }else{
      rowIdColumn = -1;
      dataIsValid = false;
      notifyDataSetChanged();
    }
    return oldCursor;
  }

  private class NotifyingDataSetObserver extends DataSetObserver{
    @Override public void onChanged() {
      super.onChanged();
      dataIsValid = true;
      notifyDataSetChanged();
    }

    @Override public void onInvalidated() {
      super.onInvalidated();
      dataIsValid = false;
      notifyDataSetChanged();
    }
  }
}
