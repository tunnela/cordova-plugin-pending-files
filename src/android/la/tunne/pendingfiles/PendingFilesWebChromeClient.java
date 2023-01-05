package la.tunne.pendingfiles;

import android.net.Uri;
import android.app.Activity;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.content.Intent;
import android.content.ActivityNotFoundException;

import org.apache.cordova.LOG;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.apache.cordova.engine.SystemWebChromeClient;

public class PendingFilesWebChromeClient extends SystemWebChromeClient {

    private static final String LOG_TAG = "SystemWebChromeClient";

    protected CordovaInterface cordova;
    
    protected CordovaPlugin plugin;

    public PendingFilesWebChromeClient(SystemWebViewEngine parentEngine, CordovaInterface cordova, CordovaPlugin plugin) {
        super(parentEngine);

        this.cordova = cordova;
        this.plugin = plugin;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathsCallback, final WebChromeClient.FileChooserParams fileChooserParams) {
        // Check if multiple-select is specified
        Boolean selectMultiple = false;
        
        if (fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            selectMultiple = true;
        }
        Intent intent = fileChooserParams.createIntent();
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, selectMultiple);
        
        // Uses Intent.EXTRA_MIME_TYPES to pass multiple mime types.
        String[] acceptTypes = fileChooserParams.getAcceptTypes();

        if (acceptTypes.length > 1) {
            intent.setType("*/*"); // Accept all, filter mime types by Intent.EXTRA_MIME_TYPES.
            intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
        }
        PendingFiles pendingFiles = (PendingFiles) plugin;

        pendingFiles.setFilePathsCallback(filePathsCallback);
        pendingFiles.setAllowMultiple(selectMultiple);

        try {
            cordova.startActivityForResult(plugin, intent, PendingFiles.FILECHOOSER_RESULTCODE);
        } catch (ActivityNotFoundException e) {
            LOG.w("No activity found to handle file chooser intent.", e);
            
            filePathsCallback.onReceiveValue(null);
        }
        return true;
    }
}