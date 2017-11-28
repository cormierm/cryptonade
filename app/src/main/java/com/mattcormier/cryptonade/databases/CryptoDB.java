package com.mattcormier.cryptonade.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;
import com.mattcormier.cryptonade.models.Pair;

import java.util.ArrayList;

/**
 * Created by matt on 10/17/2017.
 */

public class CryptoDB {
    private static final String TAG = "CryptoDB";
    // DB Settings
    public static final String  DB_NAME = "crypto.db";
    public static final int     DB_VERSION = 16;

    // Exchange table
    public static final String  EXCHANGE_TABLE = "exchange";

    public static final String  EXCHANGE_ID = "_id";
    public static final int     EXCHANGE_ID_COL = 0;

    public static final String  EXCHANGE_TYPE_ID = "type_id";
    public static final int     EXCHANGE_TYPE_ID_COL = 1;

    public static final String  EXCHANGE_NAME = "exchange_name";
    public static final int     EXCHANGE_NAME_COL = 2;

    public static final String  EXCHANGE_API_KEY = "api_key";
    public static final int     EXCHANGE_API_KEY_COL = 3;

    public static final String  EXCHANGE_API_SECRET = "api_secret";
    public static final int     EXCHANGE_API_SECRET_COL = 4;

    public static final String  EXCHANGE_API_OTHER = "api_other";
    public static final int     EXCHANGE_API_OTHER_COL = 5;

    public static final String  EXCHANGE_ACTIVE = "active";
    public static final int     EXCHANGE_ACTIVE_COL = 6;

    // Pair table
    public static final String  PAIR_TABLE = "pair";

    public static final String  PAIR_ID = "_id";
    public static final int     PAIR_ID_COL = 0;

    public static final String  PAIR_EXCHANGE_ID = "exchange_id";
    public static final int     PAIR_EXCHANGE_ID_COL = 1;

    public static final String  PAIR_EXCHANGE_PAIR = "exchange_pair";
    public static final int     PAIR_EXCHANGE_PAIR_COL = 2;

    public static final String  PAIR_TRADING_PAIR = "trading_pair";
    public static final int     PAIR_TRADING_PAIR_COL = 3;

    // Pair table
    public static final String  TYPE_TABLE = "type";

    public static final String  TYPE_ID = "_id";
    public static final int     TYPE_ID_COL = 0;

    public static final String  TYPE_NAME = "type_name";
    public static final int     TYPE_NAME_COL = 1;

    public static final String  TYPE_API_OTHER = "api_other";
    public static final int     TYPE_API_OTHER_COL = 2;

    // Setting Table
    public static final String  SETTINGS_TABLE = "settings";

    public static final String  SETTINGS_ID = "_id";
    public static final int     SETTINGS_ID_COL = 0;

    public static final String  SETTINGS_VALUE = "value";
    public static final int     SETTINGS_VALUE_COL = 1;

    // create and drop table statements
    public static final String CREATE_EXCHANGE_TABLE =
            "CREATE TABLE " + EXCHANGE_TABLE + " (" +
            EXCHANGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            EXCHANGE_TYPE_ID + " INTEGER NOT NULL, " +
            EXCHANGE_NAME + " TEXT NOT NULL, " +
            EXCHANGE_API_KEY + " TEXT, " +
            EXCHANGE_API_SECRET + " TEXT, " +
            EXCHANGE_API_OTHER + " TEXT, " +
            EXCHANGE_ACTIVE + " INT1 DEFAULT 1);";

    public static final String CREATE_PAIR_TABLE =
            "CREATE TABLE " + PAIR_TABLE + " (" +
            PAIR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PAIR_EXCHANGE_ID + " INTEGER NOT NULL, " +
            PAIR_EXCHANGE_PAIR + " TEXT NOT NULL, " +
            PAIR_TRADING_PAIR + " TEXT NOT NULL);";

    public static final String CREATE_TYPE_TABLE =
            "CREATE TABLE " + TYPE_TABLE + " (" +
                    TYPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TYPE_NAME + " TEXT NOT NULL, " +
                    TYPE_API_OTHER + " TEXT);";

    public static final String CREATE_SETTINGS_TABLE =
            "CREATE TABLE " + SETTINGS_TABLE + " (" +
                    SETTINGS_ID + " INTEGER PRIMARY KEY, " +
                    SETTINGS_VALUE + " TEXT NOT NULL);";

    public static final String DROP_EXCHANGE_TABLE =
            "DROP TABLE IF EXISTS " + EXCHANGE_TABLE;

    public static final String DROP_PAIR_TABLE =
            "DROP TABLE IF EXISTS " + PAIR_TABLE;

    public static final String DROP_TYPE_TABLE =
            "DROP TABLE IF EXISTS " + TYPE_TABLE;

    public static final String DROP_SETTINGS_TABLE =
            "DROP TABLE IF EXISTS " + SETTINGS_TABLE;


    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context c, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(c, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate: starting.");
            db.execSQL(CREATE_EXCHANGE_TABLE);
            db.execSQL(CREATE_PAIR_TABLE);
            db.execSQL(CREATE_TYPE_TABLE);
            db.execSQL(CREATE_SETTINGS_TABLE);

            //insert sample exchange
            db.execSQL("INSERT INTO exchange VALUES (1, 1, 'Poloniex', 'key', 'secret', '', 1)");
            // insert sample pair
            db.execSQL("INSERT INTO pair VALUES (1, 1, 'BTC-ETH', 'BTC-ETH')");

            db.execSQL("INSERT INTO type VALUES (1, 'Poloniex', '')");
            db.execSQL("INSERT INTO type VALUES (2, 'QuadrigaCX', 'Client Id')");
            db.execSQL("INSERT INTO type VALUES (3, 'Bitfinex', '')");
            db.execSQL("INSERT INTO type VALUES (4, 'Bittrex', '')");
            db.execSQL("INSERT INTO type VALUES (5, 'CEX.IO', 'Username')");
            db.execSQL("INSERT INTO type VALUES (6, 'GDAX', 'Passphrase')");
            db.execSQL("INSERT INTO type VALUES (7, 'Gemini', '')");
            db.execSQL("INSERT INTO type VALUES (8, 'HitBTC', '')");
            db.execSQL("INSERT INTO type VALUES (9, 'Binance', '')");

            // set password blank
            db.execSQL("INSERT INTO settings VALUES (1, '')");

            Log.d(TAG, "onCreate: done.");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade: starting.");
            Log.d("Crypto DB", "Upgrading db from version "
                    + oldVersion + " to " + newVersion);

//            db.execSQL(CREATE_SETTINGS_TABLE);
//            db.execSQL("INSERT INTO settings VALUES (1, '')");
//
//            db.execSQL("ALTER TABLE exchange ADD column " + EXCHANGE_ACTIVE);

            db.execSQL(CryptoDB.DROP_TYPE_TABLE);
            db.execSQL(CREATE_TYPE_TABLE);
            db.execSQL("INSERT INTO type VALUES (1, 'Poloniex', '')");
            db.execSQL("INSERT INTO type VALUES (2, 'QuadrigaCX', 'Client Id')");
            db.execSQL("INSERT INTO type VALUES (3, 'Bitfinex', '')");
            db.execSQL("INSERT INTO type VALUES (4, 'Bittrex', '')");
            db.execSQL("INSERT INTO type VALUES (5, 'CEX.IO', 'Username')");
            db.execSQL("INSERT INTO type VALUES (6, 'GDAX', 'Passphrase')");
            db.execSQL("INSERT INTO type VALUES (7, 'Gemini', '')");
            db.execSQL("INSERT INTO type VALUES (8, 'HitBTC', '')");
            db.execSQL("INSERT INTO type VALUES (9, 'Binance', '')");

            Log.d(TAG, "onUpgrade: done.");
        }
    }

    private SQLiteDatabase db;
    private DBHelper dbHelper;

    public CryptoDB(Context c) {
        dbHelper = new DBHelper(c, DB_NAME, null, DB_VERSION);
    }

    private void openReadableDB() {
        db = dbHelper.getReadableDatabase();
    }

    private void openWriteableDB() {
        db = dbHelper.getWritableDatabase();
    }

    private void closeDB() {
        if (db != null)
            db.close();
    }

    public ArrayList<Exchange> getExchanges() {
        ArrayList<Exchange> exchanges = new ArrayList<>();
        openReadableDB();
        Cursor cur = db.query(EXCHANGE_TABLE,
                null, null, null, null, null, null);
        while (cur.moveToNext()) {
            Exchange ex = new Exchange();
            ex.setId(cur.getInt(EXCHANGE_ID_COL));
            ex.setTypeId(cur.getInt(EXCHANGE_TYPE_ID_COL));
            ex.setName(cur.getString(EXCHANGE_NAME_COL));
            ex.setAPIKey(cur.getString(EXCHANGE_API_KEY_COL));
            ex.setAPISecret(cur.getString(EXCHANGE_API_SECRET_COL));
            ex.setAPIOther(cur.getString(EXCHANGE_API_OTHER_COL));

            exchanges.add(ex);
        }
        if(cur != null)
            cur.close();
        closeDB();

        return exchanges;
    }

    public Exchange getExchange(int id) {
        String where = EXCHANGE_ID + "= ?";
        String[] whereArgs = { Integer.toString(id) };

        this.openReadableDB();
        Cursor cursor = db.query(EXCHANGE_TABLE,
                null, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        Exchange exchange = getExchangeFromCursor(cursor);
        if (cursor != null)
            cursor.close();
        this.closeDB();

        return exchange;
    }

    private Exchange getExchangeFromCursor(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0){
            return null;
        }
        else {
            try {
                Exchange exchange = new Exchange(
                        cursor.getInt(EXCHANGE_ID_COL),
                        cursor.getInt(EXCHANGE_TYPE_ID_COL),
                        cursor.getString(EXCHANGE_NAME_COL),
                        cursor.getString(EXCHANGE_API_KEY_COL),
                        cursor.getString(EXCHANGE_API_SECRET_COL),
                        cursor.getString(EXCHANGE_API_OTHER_COL),
                        cursor.getInt(EXCHANGE_ACTIVE_COL));
                return exchange;
            }
            catch(Exception e) {
                return null;
            }
        }
    }

    public long insertExchange(Exchange exchange) {
        ContentValues cv = new ContentValues();
        cv.put(EXCHANGE_TYPE_ID, exchange.getTypeId());
        cv.put(EXCHANGE_NAME, exchange.getName());
        cv.put(EXCHANGE_API_KEY, exchange.getAPIKey());
        cv.put(EXCHANGE_API_SECRET, exchange.getAPISecret());
        cv.put(EXCHANGE_API_OTHER, exchange.getAPIOther());
        cv.put(EXCHANGE_ACTIVE, exchange.getActive());

        this.openWriteableDB();
        long rowID = db.insert(EXCHANGE_TABLE, null, cv);
        this.closeDB();

        return rowID;
    }

    public int updateExchange(Exchange exchange) {
        ContentValues cv = new ContentValues();
        cv.put(EXCHANGE_NAME, exchange.getName());
        cv.put(EXCHANGE_TYPE_ID, exchange.getTypeId());
        cv.put(EXCHANGE_API_KEY, exchange.getAPIKey());
        cv.put(EXCHANGE_API_SECRET, exchange.getAPISecret());
        cv.put(EXCHANGE_API_OTHER, exchange.getAPIOther());
        cv.put(EXCHANGE_ACTIVE, exchange.getActive());

        String where = EXCHANGE_ID + "= ?";
        String[] whereArgs = { String.valueOf(exchange.getId()) };

        this.openWriteableDB();
        int rowCount = db.update(EXCHANGE_TABLE, cv, where, whereArgs);
        this.closeDB();

        return rowCount;
    }

    public int deleteExchange(long id) {
        String where = EXCHANGE_ID + "= ?";
        String[] whereArgs = { String.valueOf(id) };

        this.openWriteableDB();
        int rowCount = db.delete(EXCHANGE_TABLE, where, whereArgs);
        this.closeDB();

        return rowCount;
    }


    public ArrayList<Pair> getPairs(int exchangeId) {
        ArrayList<Pair> pairs = new ArrayList<>();
        String where = PAIR_EXCHANGE_ID + "= ?";
        String[] whereArgs = { Integer.toString(exchangeId) };
        String orderBy = PAIR_TRADING_PAIR;
        openReadableDB();
        Cursor cur = db.query(PAIR_TABLE,
                null, where, whereArgs, null, null, orderBy);
        while (cur.moveToNext()) {
            Pair p = new Pair();
            p.setId(cur.getInt(PAIR_ID_COL));
            p.setExchangeId(cur.getInt(PAIR_EXCHANGE_ID_COL));
            p.setExchangePair(cur.getString(PAIR_EXCHANGE_PAIR_COL));
            p.setTradingPair(cur.getString(PAIR_TRADING_PAIR_COL));

            pairs.add(p);
        }
        if(cur != null)
            cur.close();
        closeDB();

        return pairs;
    }

    public Pair getPair(int id) {
        String where = PAIR_ID + "= ?";
        String[] whereArgs = { Integer.toString(id) };

        this.openReadableDB();
        Cursor cursor = db.query(PAIR_TABLE,
                null, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        Pair pair = getPairFromCursor(cursor);
        if (cursor != null)
            cursor.close();
        this.closeDB();

        return pair;
    }

    private Pair getPairFromCursor(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0){
            return null;
        }
        else {
            try {
                Pair pair = new Pair(
                        cursor.getInt(PAIR_ID_COL),
                        cursor.getInt(PAIR_EXCHANGE_ID_COL),
                        cursor.getString(PAIR_EXCHANGE_PAIR_COL),
                        cursor.getString(PAIR_TRADING_PAIR_COL));
                return pair;
            }
            catch(Exception e) {
                return null;
            }
        }
    }

    public long insertPair(Pair pair) {
        ContentValues cv = new ContentValues();
        cv.put(PAIR_EXCHANGE_ID, pair.getExchangeId());
        cv.put(PAIR_EXCHANGE_PAIR, pair.getExchangePair());
        cv.put(PAIR_TRADING_PAIR, pair.getTradingPair());

        this.openWriteableDB();
        long rowID = db.insert(PAIR_TABLE, null, cv);
        this.closeDB();

        return rowID;
    }

    public void insertPairs(ArrayList<Pair> pairList) {
        for (Pair pair : pairList) {
            insertPair(pair);
        }
    }

    public int updatePair(Pair pair) {
        ContentValues cv = new ContentValues();
        cv.put(PAIR_EXCHANGE_ID, pair.getExchangeId());
        cv.put(PAIR_EXCHANGE_PAIR, pair.getExchangePair());
        cv.put(PAIR_TRADING_PAIR, pair.getTradingPair());

        String where = PAIR_ID + "= ?";
        String[] whereArgs = { String.valueOf(pair.getId()) };

        this.openWriteableDB();
        int rowCount = db.update(PAIR_TABLE, cv, where, whereArgs);
        this.closeDB();

        return rowCount;
    }

    public int deletePair(long id) {
        String where = PAIR_ID + "= ?";
        String[] whereArgs = { String.valueOf(id) };

        this.openWriteableDB();
        int rowCount = db.delete(PAIR_TABLE, where, whereArgs);
        this.closeDB();

        return rowCount;
    }

    public int deletePairsByExchangeId(long exchangeId) {
        String where = PAIR_EXCHANGE_ID + "= ?";
        String[] whereArgs = { String.valueOf(exchangeId) };

        this.openWriteableDB();
        int rowCount = db.delete(PAIR_TABLE, where, whereArgs);
        this.closeDB();

        return rowCount;
    }

    public ArrayList<ExchangeType> getTypes() {
        ArrayList<ExchangeType> types = new ArrayList<>();
        openReadableDB();
        Cursor cur = db.query(TYPE_TABLE,
                null, null, null, null, null, null);
        while (cur.moveToNext()) {
            ExchangeType type = new ExchangeType();
            type.setTypeId(cur.getInt(TYPE_ID_COL));
            type.setName(cur.getString(TYPE_NAME_COL));
            type.setApiOther(cur.getString(TYPE_API_OTHER_COL));

            types.add(type);
        }
        if(cur != null)
            cur.close();
        closeDB();

        return types;
    }

    public ExchangeType getExchangeTypeById(int typeId) {
        String where = TYPE_ID + "= ?";
        String[] whereArgs = { Integer.toString(typeId) };

        this.openReadableDB();
        Cursor cursor = db.query(TYPE_TABLE,
                null, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        ExchangeType exType = getExchangeTypeFromCursor(cursor);
        if (cursor != null)
            cursor.close();
        this.closeDB();

        return exType;
    }

    private ExchangeType getExchangeTypeFromCursor(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0){
            return null;
        }
        else {
            try {
                ExchangeType exType = new ExchangeType(
                        cursor.getInt(TYPE_ID_COL),
                        cursor.getString(TYPE_NAME_COL),
                        cursor.getString(TYPE_API_OTHER_COL));
                return exType;
            }
            catch(Exception e) {
                return null;
            }
        }
    }

    public int updatePasswordHash(String passwordHash) {
        ContentValues cv = new ContentValues();
        cv.put(SETTINGS_ID, 1);
        cv.put(SETTINGS_VALUE, passwordHash);
        String where = SETTINGS_ID + "= ?";
        String[] whereArgs = { "1" };
        this.openWriteableDB();
        int rowCount = db.update(SETTINGS_TABLE, cv, where, whereArgs);
        this.closeDB();
        return rowCount;
    }

    public String getPasswordHash() {
        String where = SETTINGS_ID + "= ?";
        String[] whereArgs = { "1" };
        this.openReadableDB();
        Cursor cursor = db.query(SETTINGS_TABLE,
                null, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        String passwordHash = getSettingFromCursor(cursor);
        if (cursor != null)
            cursor.close();
        this.closeDB();
        return passwordHash;
    }

    private String getSettingFromCursor(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0){
            return null;
        }
        else {
            try {
                return cursor.getString(SETTINGS_VALUE_COL);
            }
            catch(Exception e) {
                return null;
            }
        }
    }
}
