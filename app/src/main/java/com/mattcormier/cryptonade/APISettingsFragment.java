package com.mattcormier.cryptonade;

import android.app.Fragment;
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
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;

import java.util.List;

public class APISettingsFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "APISettingsFragment";
    EditText edProfileName;
    EditText edAPIKey;
    EditText edAPISecret;
    TextView tvAPIOther;
    EditText edAPIOther;
    Button btnSave;
    CryptoDB db;
    Exchange ex;
    View apiView;
    Spinner spnType;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        apiView = inflater.inflate(R.layout.apisettings_layout, container, false);
        context = getActivity();
        edProfileName = apiView.findViewById(R.id.edAPISettingsProfileName);
        edAPIKey = apiView.findViewById(R.id.edAPISettingsAPIKey);
        edAPISecret = apiView.findViewById(R.id.edAPISettingsAPISecret);
        tvAPIOther = apiView.findViewById(R.id.lblAPISettingsAPIOther);
        edAPIOther = apiView.findViewById(R.id.edAPISettingsAPIOther);
        btnSave = apiView.findViewById(R.id.btnAPISettingsSave);
        btnSave.setOnClickListener(this);

        db = new CryptoDB(getActivity());

        ex = db.getExchange(1);
        edProfileName.setText(ex.getName());
        edAPIKey.setText(ex.getAPIKey());
        edAPISecret.setText(ex.getAPISecret());

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

        return apiView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnSave.getId()) {
            ex.setId(1);
            ex.setTypeId(((ExchangeType)spnType.getSelectedItem()).getTypeId());
            ex.setName(edProfileName.getText().toString());
            ex.setAPIKey(edAPIKey.getText().toString());
            ex.setAPISecret(edAPISecret.getText().toString());
            db.updateExchange(ex);
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PairsFragment())
                    .addToBackStack("api_settings")
                    .commit();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ExchangeType type = (ExchangeType)spnType.getSelectedItem();
        if (type.getApiOther().isEmpty()) {
            tvAPIOther.setVisibility(View.INVISIBLE);
            edAPIOther.setVisibility(View.INVISIBLE);
        } else {
            tvAPIOther.setVisibility(View.VISIBLE);
            edAPIOther.setVisibility(View.VISIBLE);
            tvAPIOther.setText(type.getApiOther());
            edAPIOther.setText("");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
