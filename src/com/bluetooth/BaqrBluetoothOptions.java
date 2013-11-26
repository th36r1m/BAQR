package com.bluetooth;

import com.baqr.baqr.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class BaqrBluetoothOptions extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.bluetooth_preferences);
		
		// Set back button as home
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
}
