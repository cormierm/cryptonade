package com.mattcormier.cryptonade.lib;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Pair;

import java.util.List;

/**
 * Created by matt on 10/18/2017.
 */

public class Crypto {
    public static void UpdatePairsSpinner(Context c,  Spinner spinner, CryptoDB db) {
        List<Pair> pairsList = db.getPairs(1);
        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(c,
                    android.R.layout.simple_spinner_item, pairsList);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Crypto", "Error in UpdatePairsSpinner: " + ex.toString());
        }
    }


}
