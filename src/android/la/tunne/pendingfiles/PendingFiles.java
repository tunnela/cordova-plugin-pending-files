package la.tunne.pendingfiles;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.Uri;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.content.ContentResolver;

import org.apache.cordova.LOG;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

import org.json.JSONArray;
import org.json.JSONObject;

public class PendingFiles extends CordovaPlugin {

    private static final String LOG_TAG = "PendingFiles";

    public static final int FILECHOOSER_RESULTCODE = 5173;

    protected ValueCallback<Uri[]> filePathsCallback = null;

    protected CallbackContext callbackContext = null;

    protected boolean allowMultiple = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        SystemWebView view = (SystemWebView) webView.getView();
        SystemWebViewEngine engine = (SystemWebViewEngine) webView.getEngine();
        
        view.setWebChromeClient(new PendingFilesWebChromeClient(engine, cordova, this));
    }

    public void setFilePathsCallback(ValueCallback<Uri[]> filePathsCallback) {
        this.filePathsCallback = filePathsCallback;
    }

    public void setAllowMultiple(boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Uri[] result = null;

        if (resultCode == Activity.RESULT_OK && intent != null) {
            if (intent.getClipData() != null) {
                // handle multiple-selected files
                final int numSelectedFiles = intent.getClipData().getItemCount();

                result = new Uri[numSelectedFiles];

                for (int i = 0; i < numSelectedFiles; i++) {
                    result[i] = intent.getClipData().getItemAt(i).getUri();
                }
            }
            else if (intent.getData() != null) {
                // handle single-selected file
                result = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            }
        }
        if (filePathsCallback != null) {
            filePathsCallback.onReceiveValue(result);
        } else if (callbackContext != null) {
            final Uri[] uris = result;
            final boolean allowMultiple = this.allowMultiple;
            
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                      JSONObject obj = new JSONObject();
                      obj.put("allowMultiple", allowMultiple);

                      if (!allowMultiple) {
                          obj.put("file", saveToTempFile(uris[0]).toString());

                          callbackContext.success(obj);
                          
                          return;
                      }
                      JSONArray files = new JSONArray();

                      for (int j = 0; j < uris.length; j++) {
                          String file = saveToTempFile(uris[j]).toString();

                          if (file != null) {
                              files.put(file);
                          }
                      }
                      obj.put("files", files);

                      callbackContext.success(obj);
                    } catch (Exception e) {
                      callbackContext.error("Unable to recover files");
                    }
                }
            });
        }
        setFilePathsCallback(null);
    }

    protected Uri saveToTempFile(Uri sourceUri) {
        ContentResolver contentResolver = cordova.getActivity().getContentResolver();
        Uri destinationUri = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(contentResolver.getType(sourceUri));

            File destinationFile = createTempFile(type, "file-" + System.currentTimeMillis());

            destinationUri = Uri.fromFile(destinationFile);

            inputStream = contentResolver.openInputStream(sourceUri);
            outputStream = contentResolver.openOutputStream(destinationUri);

            int len;
            byte[] buffer = new byte[10 * 1024];

            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {

        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {} 
        }
        return destinationUri;
    }

    protected String getTempDirectoryPath() {
        File cache = cordova.getActivity().getCacheDir();
        
        cache.mkdirs();

        return cache.getAbsolutePath();
    }

    protected File createTempFile(String ext) {
        return createTempFile(ext, "");
    }

    protected File createTempFile(String ext, String fileName) {
        if (fileName.isEmpty()) {
            fileName = ".temp";
        }
        if (ext.isEmpty()) {
            ext = "jpg";
        }
        fileName = fileName + "." + ext;

        return new File(getTempDirectoryPath(), fileName);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("allowMultiple", allowMultiple);

        return bundle;
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.setAllowMultiple(state.getBoolean("allowMultiple", false));
        
        this.callbackContext = callbackContext;
    }
}