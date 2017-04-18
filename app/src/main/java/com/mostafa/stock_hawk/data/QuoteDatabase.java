package com.mostafa.stock_hawk.data;



import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by mostafa on 18/04/17.
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
    private QuoteDatabase(){}

    public static final int VERSION = 7;

    @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
}