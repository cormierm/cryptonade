package com.mattcormier.cryptonade;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;

import java.util.List;

public class APISettingsActivity extends AppCompatActivity implements OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "APISettingsActivity";
    public int exchangeId = 0;
    EditText edProfileName;
    EditText edAPIKey;
    EditText edAPISecret;
    TextView tvAPIOther;
    EditText edAPIOther;
    Button btnSave;
    Switch swEnabled;
    CryptoDB db;
    Exchange ex;
    Spinner spnType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apisettings);
        Log.d(TAG, "onCreate: exchangeId =" + exchangeId);

        edProfileName = findViewById(R.id.edAPISettingsProfileName);
        edAPIKey = findViewById(R.id.edAPISettingsAPIKey);
        edAPISecret = findViewById(R.id.edAPISettingsAPISecret);
        tvAPIOther = findViewById(R.id.lblAPISettingsAPIOther);
        edAPIOther = findViewById(R.id.edAPISettingsAPIOther);
        btnSave = findViewById(R.id.btnAPISettingsSave);
        swEnabled = findViewById(R.id.swAPISettingsActive);
        btnSave.setOnClickListener(this);

        db = new CryptoDB(this);

        this.exchangeId = getIntent().getIntExtra("ExchangeId", 0);

        // setup exchange type spinner
        spnType = findViewById(R.id.spnAPISettingsExchangeType);
        spnType.setOnItemSelectedListener(this);
        List<ExchangeType> typesList = db.getTypes();
        try {
            ArrayAdapter<ExchangeType> dataAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, typesList);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnType.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.e(TAG, "onCreate: " + ex.getMessage());
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (exchangeId == 0) {
            edProfileName.setText(spnType.getSelectedItem().toString());
            edAPIKey.setText("");
            edAPISecret.setText("");
            edAPIOther.setText("");
            btnSave.setText(getString(R.string.create));
            actionBar.setTitle("Create API Key");
        } else {
            Log.d(TAG, "onCreateView: setting existing exchange info");
            ex = db.getExchange(exchangeId);
            spnType.setSelection(getIndex(spnType, ex.getTypeId()));
            setAPIOtherVisibility();
            edAPIOther.setVisibility(View.VISIBLE);
            edProfileName.setText(ex.getName());
            edAPIKey.setText(ex.getAPIKey());
            edAPISecret.setText(ex.getAPISecret());
            edAPIOther.setText(ex.getAPIOther());
            Log.d(TAG, "onCreate: active: " + ex.getActive());
            if (ex.getActive() == 1) {
                swEnabled.setChecked(true);
            } else {
                swEnabled.setChecked(false);
            }

            btnSave.setText(getString(R.string.update));
            actionBar.setTitle("Edit API Key");
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuDelete) {
            db.deleteExchange(exchangeId);
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(exchangeId > 0) {
            getMenuInflater().inflate(R.menu.apisettings_menu, menu);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnSave.getId()) {
            if (exchangeId == 0) {
                Exchange exchange = new Exchange();
                exchange.setTypeId(((ExchangeType)spnType.getSelectedItem()).getTypeId());
                exchange.setName(edProfileName.getText().toString());
                exchange.setAPIKey(edAPIKey.getText().toString());
                exchange.setAPISecret(edAPISecret.getText().toString());
                if (swEnabled.isChecked()) {
                    exchange.setActive(1);
                } else {
                    exchange.setActive(0);
                }
                if (edAPIOther.getVisibility() == View.VISIBLE) {
                    exchange.setAPIOther(edAPIOther.getText().toString());
                }
                long rowId = db.insertExchange(exchange);
                Exchange newEx = db.getExchange((int)rowId);
                APIClient client = Crypto.getAPIClient(newEx);
                if(client != null) {
                    client.RestorePairsInDB(this);
                }
                onBackPressed();
            } else {
                ex.setTypeId(((ExchangeType)spnType.getSelectedItem()).getTypeId());
                ex.setName(edProfileName.getText().toString());
                ex.setAPIKey(edAPIKey.getText().toString());
                ex.setAPISecret(edAPISecret.getText().toString());
                if (swEnabled.isChecked()) {
                    ex.setActive(1);
                } else {
                    ex.setActive(0);
                }
                if (edAPIOther.getVisibility() == View.VISIBLE) {
                    ex.setAPIOther(edAPIOther.getText().toString());
                }
                db.updateExchange(ex);
                onBackPressed();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (exchangeId == 0) {
            edProfileName.setText(spnType.getSelectedItem().toString());
        }
        setAPIOtherVisibility();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private int getIndex(Spinner spinner, long typeId){
        Log.d(TAG, "getIndex: " + typeId);
        int index = 0;
        for (int i=0;i<spinner.getCount();i++){
            if (((ExchangeType)spinner.getItemAtPosition(i)).getTypeId() == typeId){
                index = i;
                break;
            }
        }
        return index;
    }

    private void setAPIOtherVisibility() {
        ExchangeType type = (ExchangeType)spnType.getSelectedItem();
        if (type.getApiOther().isEmpty()) {
            tvAPIOther.setVisibility(View.INVISIBLE);
            edAPIOther.setVisibility(View.INVISIBLE);
        } else {
            tvAPIOther.setVisibility(View.VISIBLE);
            edAPIOther.setVisibility(View.VISIBLE);
            tvAPIOther.setText(type.getApiOther());
        }
    }
}
