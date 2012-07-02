/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.doubtech.livewatch.googletvremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * The sample control preference activity handles the preferences for
 * the sample control extension.
 */
public class GTVRemotePreferenceActivity extends PreferenceActivity {
    private static final int DIALOG_READ_ME = 1;

	  /**
	   * Request code used by this activity.
	   */
	  private static final int CODE_SWITCH_BOX = 1;

	  /**
	   * Request code used by this activity for pairing requests.
	   */
	  private static final int CODE_PAIRING = 2;

	  private static final long MIN_TOAST_PERIOD = TimeUnit.SECONDS.toMillis(3);

	  /**
	   * User codes defined in activities extending this one should start above
	   * this value.
	   */
	  public static final int FIRST_USER_CODE = 100;

	  /**
	   * Code for delayed messages to dim the screen.
	   */
	  private static final int SCREEN_DIM = 1;

	  /**
	   * Backported brightness level from API level 8 
	   */
	  private static final float BRIGHTNESS_OVERRIDE_NONE = -1.0f;
	private static final String LOG_TAG = "GTV Remote Preference";

	  private boolean isKeepingConnection;

	  private boolean isScreenDimmed;

	  private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);
        
        findPreference(getText(R.string.preference_key_pairing)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {			    
				showPairingActivity();
			    return true;
			}
		});
        
        findPreference(getText(R.string.preference_key_finder)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				onShowDeviceFinder();
			    return true;
			}
		});

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_READ_ME:
                dialog = createReadMeDialog();
                break;
            default:
                Log.w(GTVRemoteExtensionService.LOG_TAG, "Not a valid dialog id: " + id);
                break;
        }

        return dialog;
    }

    /**
     * Create the Read me dialog
     *
     * @return the Dialog
     */
    private Dialog createReadMeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.preference_option_read_me_txt)
                .setTitle(R.string.preference_option_read_me)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

	  /**
	   * If connection failed due to SSL handshake failure, this method will be
	   * invoked to start the pairing session with device, and establish secure
	   * connection.
	   * <p>
	   * When pairing finishes, PairingListener's method will be called to
	   * differentiate the result.
	   */
	  private final void showPairingActivity() {
	  }


		public void onShowDeviceFinder() {
		}
}
