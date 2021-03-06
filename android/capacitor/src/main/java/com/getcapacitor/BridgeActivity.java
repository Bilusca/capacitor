package com.getcapacitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.android.R;
import com.getcapacitor.cordova.MockCordovaWebViewImpl;
import com.getcapacitor.plugin.App;

import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;

import java.util.ArrayList;

public class BridgeActivity extends AppCompatActivity {
  private Bridge bridge;
  public CordovaInterfaceImpl cordovaInterface;
  private ArrayList<PluginEntry> pluginEntries;
  PluginManager pluginManager;
  private CordovaPreferences preferences;

  private int activityDepth = 0;

  private String lastActivityPlugin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    loadConfig(this.getApplicationContext(),this);
    Splash.showOnLaunch(this);

    getApplication().setTheme(getResources().getIdentifier("AppTheme_NoActionBar", "style", getPackageName()));
    setTheme(getResources().getIdentifier("AppTheme_NoActionBar", "style", getPackageName()));
    setTheme(R.style.AppTheme_NoActionBar);
    //setTheme(R.style.AppTheme_NoActionBar);
    //getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF0000")));
    WebView.setWebContentsDebuggingEnabled(true);

    setContentView(R.layout.bridge_layout_main);

    this.load(savedInstanceState);
  }

  /**
   * Load the WebView and create the Bridge
   */
  protected void load(Bundle savedInstanceState) {
    Log.d(Bridge.TAG, "Starting BridgeActivity");

    WebView webView = findViewById(R.id.webview);
    cordovaInterface = new CordovaInterfaceImpl(this);
    if (savedInstanceState != null) {
      cordovaInterface.restoreInstanceState(savedInstanceState);
    }

    MockCordovaWebViewImpl mockWebView = new MockCordovaWebViewImpl(this.getApplicationContext());
    mockWebView.init(cordovaInterface, pluginEntries, preferences, webView);

    this.pluginManager = mockWebView.getPluginManager();
    cordovaInterface.onCordovaInit(this.pluginManager);
    bridge = new Bridge(this, webView, cordovaInterface, this.pluginManager);

    if (savedInstanceState != null) {
      bridge.restoreInstanceState(savedInstanceState);
    }
  }

  /**
   * Notify the App plugin that the current state changed
   * @param isActive
   */
  private void fireAppStateChanged(boolean isActive) {
    PluginHandle handle = bridge.getPlugin("App");
    if (handle == null) {
      return;
    }

    App appState = (App) handle.getInstance();
    if (appState != null) {
      appState.fireChange(isActive);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    bridge.saveInstanceState(outState);
  }

  @Override
  public void onStart() {
    super.onStart();

    activityDepth++;

    this.bridge.onStart();

    Log.d(Bridge.TAG, "App started");
  }

  @Override
  public void onRestart() {
    super.onRestart();
    this.bridge.onRestart();
    Log.d(Bridge.TAG, "App restarted");
  }

  @Override
  public void onResume() {
    super.onResume();

    fireAppStateChanged(true);

    this.bridge.onResume();

    Log.d(Bridge.TAG, "App resumed");
  }

  @Override
  public void onPause() {
    super.onPause();

    this.bridge.onPause();

    Log.d(Bridge.TAG, "App paused");
  }

  @Override
  public void onStop() {
    super.onStop();

    activityDepth = Math.max(0, activityDepth - 1);
    if (activityDepth == 0) {
      fireAppStateChanged(false);
    }

    this.bridge.onStop();

    Log.d(Bridge.TAG, "App stopped");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(Bridge.TAG, "App destroyed");
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if (this.bridge == null) {
      return;
    }

    this.bridge.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (this.bridge == null) {
      return;
    }
    this.bridge.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (this.bridge == null) {
      return;
    }

    this.bridge.onNewIntent(intent);
  }

  public void loadConfig(Context context, Activity activity) {
    ConfigXmlParser parser = new ConfigXmlParser();
    parser.parse(context);
    preferences = parser.getPreferences();
    preferences.setPreferencesBundle(activity.getIntent().getExtras());
    pluginEntries = parser.getPluginEntries();
  }
}
