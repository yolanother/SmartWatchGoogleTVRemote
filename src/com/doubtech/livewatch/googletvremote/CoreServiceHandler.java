package com.doubtech.livewatch.googletvremote;
/*
 * Copyright (C) 2010 Google Inc.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.google.android.apps.tvremote.ConnectionManager;
import com.google.android.apps.tvremote.CoreService;
import com.google.android.apps.tvremote.KeyStoreManager;
import com.google.android.apps.tvremote.protocol.QueuingSender;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Abstract activity that handles connection to the {@link CoreService}.
 *
 * The activity connects to service in {@link #onCreate(Bundle)}, and
 * disconnects in {@link #onDestroy()}. Upon successful connection, and before
 * disconnection appropriate callbacks are invoked.
 *
 */
public class CoreServiceHandler {
  private static final String TAG = "CoreServiceHandler";

private static final String LOG_TAG = "CoreServiceActivity";

  /**
   * Used to connect to the background service.
   */
  private ServiceConnection serviceConnection;
  private CoreService coreService;
  private Queue<Runnable> runnableQueue;
  MissingSenderToaster missingSenderToaster = new MissingSenderToaster();
  public MissingSenderToaster getMissingSenderToaster() {
	  return missingSenderToaster;
  }

  private static final long MIN_TOAST_PERIOD = TimeUnit.SECONDS.toMillis(3);
private Context mContext;

public static interface CoreServiceHandlerInterface {
	  /**
	   * Callback that is called when the core service become available.
	   */
	  void onServiceAvailable(CoreService coreService);

	  /**
	   * Callback that is called when the core service is about disconnecting.
	   */
	  void onServiceDisconnecting(CoreService coreService);

}

  public CoreServiceHandler(Context context) {
	mContext = context;
    runnableQueue = new LinkedList<Runnable>();
    connectToService();
  }

  protected void onDestroy() {
    disconnectFromService();
  }

  /**
   * Opens the connection to the underlying service.
   */
  private void connectToService() {
    serviceConnection = new ServiceConnection() {
      public void onServiceConnected(ComponentName name, IBinder service) {
    	Log.d(TAG, "Connecting to service...");
        coreService = ((CoreService.LocalBinder) service).getService();
        runQueuedRunnables();
        onServiceAvailable(coreService);
      }

      public void onServiceDisconnected(ComponentName name) {
        onServiceDisconnecting(coreService);
        coreService = null;
      }
    };
    Log.d(TAG, "Binding service...");
    Intent intent = new Intent(mContext, CoreService.class);
    mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * Closes the connection to the background service.
   */
  private synchronized void disconnectFromService() {
	  mContext.unbindService(serviceConnection);
    serviceConnection = null;
  }

  private void runQueuedRunnables() {
    Runnable runnable;
    while ((runnable = runnableQueue.poll()) != null) {
      runnable.run();
    }
  }

  /**
   * Callback that is called when the core service become available.
   */
  protected void onServiceAvailable(CoreService coreService) {
	  ((CoreServiceHandlerInterface)mContext).onServiceAvailable(coreService);
  }

  /**
   * Callback that is called when the core service is about disconnecting.
   */
  protected void onServiceDisconnecting(CoreService coreService) {
	  ((CoreServiceHandlerInterface)mContext).onServiceDisconnecting(coreService);
  }

  /**
   * Starts an activity based on its class.
   */
  protected void showActivity(Class<?> activityClass) {
    Intent intent = new Intent(mContext, activityClass);
    mContext.startActivity(intent);
  }

  protected ConnectionManager getConnectionManager() {
    return coreService;
  }

  public KeyStoreManager getKeyStoreManager() {
    if (coreService != null) {
      return coreService.getKeyStoreManager();
    }
    return null;
  }

  protected boolean executeWhenCoreServiceAvailable(Runnable runnable) {
    if (coreService == null) {
      Log.d(LOG_TAG, "Queueing runnable: " + runnable);
      runnableQueue.offer(runnable);
      return false;
    }
    runnable.run();
    return true;
  }


  public class MissingSenderToaster
      implements QueuingSender.MissingSenderListener {
    private long lastToastTime;

    public void onMissingSender() {
      if (System.currentTimeMillis() - lastToastTime > MIN_TOAST_PERIOD) {
        lastToastTime = System.currentTimeMillis();
        showMessage(R.string.sender_missing);
      }
    }
  }

  private void showMessage(int resId) {
    Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT).show();
  }
}
