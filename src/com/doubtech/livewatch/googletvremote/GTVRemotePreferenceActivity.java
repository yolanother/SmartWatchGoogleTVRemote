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

import com.doubtech.livewatch.googletvremote.CoreServiceHandler.CoreServiceHandlerInterface;
import com.google.android.apps.tvremote.ConnectionManager;
import com.google.android.apps.tvremote.CoreService;
import com.google.android.apps.tvremote.DeviceFinder;
import com.google.android.apps.tvremote.PairingActivity;
import com.google.android.apps.tvremote.RemoteDevice;
import com.google.android.apps.tvremote.protocol.QueuingSender;
import com.google.android.apps.tvremote.util.Debug;

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

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * The sample control preference activity handles the preferences for
 * the sample control extension.
 */
public class GTVRemotePreferenceActivity extends PreferenceActivity implements CoreServiceHandlerInterface {
	private static RemoteDevice mRemoteDevice;
    private static final int DIALOG_READ_ME = 1;
	private CoreServiceHandler mCoreServiceHandler;

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

	  private QueuingSender commands;

	  private boolean isConnected;

	  private boolean isKeepingConnection;

	  private boolean isScreenDimmed;

	  private Handler handler;
	private Preference mPairingOk;
	private Preference mDeviceFinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCoreServiceHandler = new CoreServiceHandler(this);
        commands = new QueuingSender(mCoreServiceHandler.getMissingSenderToaster());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);

        // Handle read me
        /*Preference preference = findPreference(getText(R.string.preference_key_read_me));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                showDialog(DIALOG_READ_ME);
                return true;
            }
        });*/
        
        mPairingOk = findPreference(getText(R.string.preference_key_pairing_ok));
        mPairingOk.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showPairingActivity(mRemoteDevice);
			    return true;
			}
		});
        mPairingOk.setEnabled(false);
        
        mDeviceFinder = findPreference(getText(R.string.preference_key_finder_label));
        mDeviceFinder.setEnabled(false);
        mDeviceFinder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
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
	  private final void showPairingActivity(RemoteDevice target) {
	    if (target != null) {
	      startActivityForResult(
	          PairingActivity.createIntent(this, new RemoteDevice(
	              target.getName(), target.getAddress(), target.getPort() + 1)),
	          CODE_PAIRING);
	    }
	  }


		public void onShowDeviceFinder() {
		    commands.setSender(null);
		    logConnectionStatus("Show device finder");
		    showSwitchBoxActivity();
		}
		

		  /**
		   * Starts the box selection dialog.
		   */
		  private final void showSwitchBoxActivity() {
			ConnectionManager connectionManager = mCoreServiceHandler.getConnectionManager();
			RemoteDevice target = connectionManager.getTarget();
			ArrayList<RemoteDevice> recent = connectionManager.getRecentlyConnected();
		    startActivityForResult(
		        DeviceFinder.createConnectIntent(this, target, recent), 
		        CODE_SWITCH_BOX);
		  }
		  


		  public void onNeedsPairing(RemoteDevice remoteDevice) {
		    logConnectionStatus("Pairing");
		    showPairingActivity(remoteDevice);
		  }

		  private void logConnectionStatus(CharSequence sequence) {
		    String message = String.format("%s (%s)", sequence,
		        getClass().getSimpleName());
		    if (Debug.isDebugConnection()) {
		      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		    }
		    Log.d(LOG_TAG, "Connection state: " + sequence);
		  }

		@Override
		public void onServiceAvailable(CoreService coreService) {
			mDeviceFinder.setEnabled(true);
			mPairingOk.setEnabled(true);
		}

		@Override
		public void onServiceDisconnecting(CoreService coreService) {
			mDeviceFinder.setEnabled(false);
			mPairingOk.setEnabled(false);			
		}
}
