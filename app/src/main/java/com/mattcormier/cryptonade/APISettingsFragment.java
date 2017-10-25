package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.exchanges.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;

import java.util.List;

public class APISettingsFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "APISettingsFragment";
    public int exchangeId = 0;
    EditText edProfileName;
    EditText edAPIKey;
    EditText edAPISecret;
    TextView tvAPIOther;
    EditText edAPIOther;
    Button btnSave;
    Button btnDelete;
    Button btnCancel;
    CryptoDB db;
    Exchange ex;
    View apiView;
    Spinner spnType;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: exchangeId =" + exchangeId);
        apiView = inflater.inflate(R.layout.apisettings_layout, container, false);
        context = getActivity();
        edProfileName = apiView.findViewById(R.id.edAPISettingsProfileName);
        edAPIKey = apiView.findViewById(R.id.edAPISettingsAPIKey);
        edAPISecret = apiView.findViewById(R.id.edAPISettingsAPISecret);
        tvAPIOther = apiView.findViewById(R.id.lblAPISettingsAPIOther);
        edAPIOther = apiView.findViewById(R.id.edAPISettingsAPIOther);
        btnSave = apiView.findViewById(R.id.btnAPISettingsSave);
        btnDelete = apiView.findViewById(R.id.btnAPISettingsDelete);
        btnCancel = apiView.findViewById(R.id.btnAPISettingsCancel);
        btnSave.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        db = new CryptoDB(getActivity());

        // setup exchange type spinner
        spnType = apiView.findViewById(R.id.spnAPISettingsExchangeType);
        spnType.setOnItemSelectedListener(this);
        List<ExchangeType> typesList = db.getTypes();
        try {
            ArrayAdapter<ExchangeType> dataAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, typesList);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnType.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d(TAG, "onCreateView: " + ex.getMessage());
        }

        if (exchangeId == 0) {
            edProfileName.setText("API Name");
            edAPIKey.setText("");
            edAPISecret.setText("");
            edAPIOther.setText("");
            btnDelete.setVisibility(View.INVISIBLE);
            btnSave.setText(getString(R.string.create));
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
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setText(getString(R.string.update));
        }

        return apiView;
    }

    @Override
    public void onClick(View v) {
        FragmentManager fragmentManager = getFragmentManager();
        if (v.getId() == btnSave.getId()) {
            if (exchangeId == 0) {
                Exchange exchange = new Exchange();
                exchange.setTypeId(((ExchangeType)spnType.getSelectedItem()).getTypeId());
                exchange.setName(edProfileName.getText().toString());
                exchange.setAPIKey(edAPIKey.getText().toString());
                exchange.setAPISecret(edAPISecret.getText().toString());
                if (edAPIOther.getVisibility() == View.VISIBLE) {
                    exchange.setAPIOther(edAPIOther.getText().toString());
                }
                db.insertExchange(exchange);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new APIFragment())
                        .addToBackStack("api_settings")
                        .commit();
            } else {
                ex.setTypeId(((ExchangeType)spnType.getSelectedItem()).getTypeId());
                ex.setName(edProfileName.getText().toString());
                ex.setAPIKey(edAPIKey.getText().toString());
                ex.setAPISecret(edAPISecret.getText().toString());
                if (edAPIOther.getVisibility() == View.VISIBLE) {
                    ex.setAPIOther(edAPIOther.getText().toString());
                }
                db.updateExchange(ex);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new APIFragment())
                        .addToBackStack("api_settings")
                        .commit();
            }

        }
        else if (v.getId() == btnCancel.getId()) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new APIFragment())
                    .commit();
        }
        else if (v.getId() == btnDelete.getId()) {
            db.deleteExchange(exchangeId);
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new APIFragment())
                    .commit();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setAPIOtherVisibility();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void setExchangeId(int exchangeId){
        this.exchangeId = exchangeId;
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
