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
import com.doubtech.livewatch.googletvremote.widgets.SoftDpad;
import com.doubtech.livewatch.googletvremote.widgets.SoftDpad.DpadListener;
import com.google.android.apps.tvremote.ConnectionManager.ConnectionListener;
import com.google.android.apps.tvremote.CoreService;
import com.google.android.apps.tvremote.DeviceFinder;
import com.google.android.apps.tvremote.PairingActivity;
import com.google.android.apps.tvremote.RemoteDevice;
import com.google.android.apps.tvremote.protocol.ICommandSender;
import com.google.android.apps.tvremote.protocol.QueuingSender;
import com.google.android.apps.tvremote.util.Action;
import com.google.android.apps.tvremote.util.Debug;
import com.sonyericsson.extras.liveware.extension.util.view.HorizontalPager;
import com.sonyericsson.extras.liveware.sdk.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

/**
 * The sample control for SmartWatch handles the control on the accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class GoogleTVControl extends HorizontalPager implements ConnectionListener, CoreServiceHandlerInterface {
	private static final String TAG = "GoogleTVControl";
	  
	private SoftDpad mDpad;

	private View mChannelDown;

	private View mChannelUp;

	private final QueuingSender commands;
	
	private boolean isConnected;
	
	private boolean isKeepingConnection;

	private CoreServiceHandler mCoreServiceHandler;

    /**
     * Create sample control.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     * @param handler The handler to use
     */
    GoogleTVControl(final Context context, int device, final String hostAppPackageName) {
        super(context, device, hostAppPackageName);

        mCoreServiceHandler = new CoreServiceHandler(context);
        commands = new QueuingSender(mCoreServiceHandler.getMissingSenderToaster());
        
        addView(R.layout.playback_controls);
        addView(R.layout.dpad);

        mChannelDown = findViewById(R.id.channel_down);
        mChannelUp = findViewById(R.id.channel_up);
        
        
        mDpad = (SoftDpad)findViewById(R.id.dpad);
        mDpad.setDpadListener(new DpadListener() {
			
			@Override
			public void onDpadMoved(int direction, boolean pressed) {
				Log.d(TAG, "DPad moved: " + direction + " pressed?" + pressed);
			}
			
			@Override
			public void onDpadClicked() {
				Log.d(TAG, "DPAD Clicked");
			}
		});
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        setKeepConnected(true);
    }

    @Override
    public void onStop() {
      setKeepConnected(false);
      super.onStop();
    }

    @Override
    public void onResume() {
      super.onResume();
      connect();
    }

    @Override
    public void onPause() {
      disconnect();
      super.onPause();
    }


    /**
     * Translates a direction and a key pressed in an action.
     *
     * @param direction the direction of the movement
     * @param pressed   {@code true} if the key was pressed
     */
	private static Action translateDirection(int direction, boolean pressed) {
		if ((direction & SoftDpad.DOWN) > 0) {
			return pressed ? Action.DPAD_DOWN_PRESSED
					: Action.DPAD_DOWN_RELEASED;
		}
		if ((direction & SoftDpad.LEFT) > 0) {
			return pressed ? Action.DPAD_LEFT_PRESSED
					: Action.DPAD_LEFT_RELEASED;
		}
		if ((direction & SoftDpad.RIGHT) > 0) {
			return pressed ? Action.DPAD_RIGHT_PRESSED
					: Action.DPAD_RIGHT_RELEASED;
		}
		if ((direction & SoftDpad.UP) > 0) {
			return pressed ? Action.DPAD_UP_PRESSED : Action.DPAD_UP_RELEASED;
		}

		return null;
	}

    private void connect() {
      if (!isConnected) {
        isConnected = true;
        mCoreServiceHandler.executeWhenCoreServiceAvailable(new Runnable() {
          public void run() {
        	  mCoreServiceHandler.getConnectionManager().connect(GoogleTVControl.this);
          }
        });
      }
    }

    private void disconnect() {
      if (isConnected) {
        commands.setSender(null);
        isConnected = false;
        mCoreServiceHandler.executeWhenCoreServiceAvailable(new Runnable() {
          public void run() {
        	  mCoreServiceHandler.getConnectionManager().disconnect(GoogleTVControl.this);
          }
        });
      }
    }

    private void setKeepConnected(final boolean keepConnected) {
      if (isKeepingConnection != keepConnected) {
        isKeepingConnection = keepConnected;
        mCoreServiceHandler.executeWhenCoreServiceAvailable(new Runnable() {
          public void run() {
            logConnectionStatus("Keep Connected: " + keepConnected);
            mCoreServiceHandler.getConnectionManager().setKeepConnected(keepConnected);
          }
        });
      }
    }

    private void logConnectionStatus(CharSequence sequence) {
      String message = String.format("%s (%s)", sequence,
          getClass().getSimpleName());
      if (Debug.isDebugConnection()) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
      }
      Log.d(TAG, "Connection state: " + sequence);
    }

	  /**
	   * Starts the box selection dialog.
	   */
	  private final void showSwitchBoxActivity() {
	    disconnect();
	  }

	@Override
	public void onConnecting() {
	    commands.setSender(null);
	    logConnectionStatus("Connecting");
	}

	@Override
	public void onConnectionSuccessful(ICommandSender sender) {
	    logConnectionStatus("Connected");
	    commands.setSender(sender);
	}

	@Override
	public void onNeedsPairing(RemoteDevice remoteDevice) {
	    logConnectionStatus("Pairing");
        Toast.makeText(mContext, "Needs Pairing.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDisconnected() {
	    commands.setSender(null);
	    logConnectionStatus("Disconnected");
	}

	@Override
	public void onShowDeviceFinder() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onServiceAvailable(CoreService coreService) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onServiceDisconnecting(CoreService coreService) {
		// TODO Auto-generated method stub
		
	}
}
