package com.mattcormier.cryptonade;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.mattcormier.cryptonade.models.Coin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by matt on 12/22/2017.
 */

class GetMarketCapData extends AsyncTask<String, Void, ArrayList<Coin>> {
    private static final String TAG = "GetMarketCapData";
    private MarketCapFragment mMarketCapFragment;
    String baseUrl = "https://api.coinmarketcap.com/v1/ticker/?limit=100";

    public GetMarketCapData(MarketCapFragment fragMC) {
        mMarketCapFragment = fragMC;
    }


    @Override
    protected void onPostExecute(ArrayList<Coin> coinList) {
        Log.d(TAG, "onPostExecute: starts");
        mMarketCapFragment.onDownloadComplete(coinList);
    }

    @Override
    protected ArrayList<Coin> doInBackground(String... strings) {
        Log.d(TAG, "doInBackground: starts");
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(baseUrl);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int response = connection.getResponseCode();
            Log.d(TAG, "doInBackground: The response code was: " + response);

            StringBuilder result = new StringBuilder();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                result.append(line).append("\n");
            }

            try {
                JSONArray jsonResult = new JSONArray(result.toString());
                ArrayList<Coin> coinList = new ArrayList<>();
                for(int i=0; i < jsonResult.length(); i++) {
                    JSONObject jsonCoin = jsonResult.getJSONObject(i);
                    Coin coin = new Coin (
                            jsonCoin.getString("name"),
                            jsonCoin.getString("symbol"),
                            jsonCoin.getInt("rank"),
                            jsonCoin.getDouble("price_btc"),
                            jsonCoin.getDouble("price_usd"),
                            jsonCoin.getDouble("market_cap_usd"),
                            jsonCoin.getDouble("24h_volume_usd"),
                            jsonCoin.getDouble("percent_change_1h"),
                            jsonCoin.getDouble("percent_change_24h"),
                            jsonCoin.getDouble("percent_change_7d"));
                    coinList.add(coin);
                }
                return coinList;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

        } catch(MalformedURLException e) {
            Log.e(TAG, "doInBackground: Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "doInBackground: IO Exception reading data: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e(TAG, "doInBackground: Security Exception. Need permissions? " + e.getMessage());
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch(IOException e) {
                    Log.e(TAG, "doInBackground: Error closing stream: " + e.getMessage());
                }
            }
        }

        return null;
    }


}
