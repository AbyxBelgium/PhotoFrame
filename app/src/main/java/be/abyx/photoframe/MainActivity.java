package be.abyx.photoframe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Exchanger;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    private ImageView imageViewer;
    private TextView temperatureView;
    private TextView pressureView;
    private TextView clockView;

    private List<File> pictures = new ArrayList<>();
    private int currentPicture = 0;

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST = 1;
    private static final String MAIN_FOLDER = "PhotoFrame";
    private static final String DATA_URL = "http://weerstationbelsele.be/weerstation/Api/get-latest-data.php";
    private static final int PICTURE_DELAY = 10000;
    private static final int WEATHER_DELAY = 5 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.imageViewer = (ImageView) findViewById(R.id.imageViewer);
        this.temperatureView = (TextView) findViewById(R.id.temperatureView);
        this.pressureView = (TextView) findViewById(R.id.pressureView);
        this.clockView = (TextView) findViewById(R.id.clockView);

        // Check for permissions
        if (!storagePermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // We should explain why this permission is necessary!
                showOKDialog(getString(R.string.permission_explanation), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Ask for permission to read and write from the external storage
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS},
                                PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST);
                    }
                });
            } else {
                // Ask for permission to read and write from the external storage
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS},
                        PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST);
            }
        } else {
            initializeSlideShow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private boolean storagePermissionGranted() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted. Load all images from the external storage
                    initializeSlideShow();
                } else {
                    // Permission denied. Inform the user that this permission is a key-component of
                    // this app and should be granted in order for this app to work.
                    showOKDialog(getString(R.string.no_permission_granted), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do nothing...
                        }
                    });
                }
                break;
            }
        }
    }

    private void showOKDialog(String message, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message)
                .setPositiveButton("OK", onClickListener);
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void loadPictures() {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), MAIN_FOLDER);
        if (!f.exists()) {
            // Make directory if it doesn't exist
            f.mkdirs();
        }

        for (File current : f.listFiles()) {
            if (current.getAbsolutePath().toLowerCase().endsWith(".jpg")) {
                pictures.add(current);
            }
        }

        Collections.shuffle(pictures);
    }

    private int getScreenWidth() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    private int getScreenHeight() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    /**
     * Set the current shown picture!
     *
     * @param picture File pointing to the picture that should be decoded and rescaled to fit.
     * @param uiHandler Handler that's connected to the UI-thread
     */
    private void setPic(File picture, Handler uiHandler) {
        // Get the dimensions of the View
        int targetW = getScreenWidth();
        int targetH = getScreenHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picture.getAbsolutePath(), bmOptions);
        double photoW = bmOptions.outWidth;
        double photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        double scaleFactor = Math.min(photoW / (double) targetW, photoH / (double) targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = (int) scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap temp = BitmapFactory.decodeFile(picture.getAbsolutePath(), bmOptions);
        if (temp != null) {
            final Bitmap bitmap = Bitmap.createScaledBitmap(temp, (int) (photoW / scaleFactor), (int) (photoH / scaleFactor), false);
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageViewer.setImageBitmap(bitmap);
                }
            });
        }
    }

    private void initializeSlideShow() {
        loadPictures();
        final Handler uiHandler = new Handler();

        final Runnable clockUpdater = new Runnable() {
            @Override
            public void run() {
                Date d = new Date();
                clockView.setText(DateFormat.format("HH:mm:ss", d.getTime()));
                uiHandler.postDelayed(this, 1000);
            }
        };

        uiHandler.post(clockUpdater);

        HandlerThread thread = new HandlerThread("BackgroundThread");
        thread.start();
        Looper looper = thread.getLooper();
        final Handler backgroundHandler = new Handler(looper);

        final Runnable pictureSwitcher = new Runnable() {
            @Override
            public void run() {
                if (pictures.size() > 0) {
                    currentPicture++;
                    currentPicture %= pictures.size();
                    File current = pictures.get(currentPicture);
                    setPic(current, uiHandler);
                }
                backgroundHandler.postDelayed(this, PICTURE_DELAY);
            }
        };

        backgroundHandler.post(pictureSwitcher);

        final Runnable weatherDownloader = new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(DATA_URL);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setConnectTimeout(1000);
                    InputStream stream = urlConnection.getInputStream();
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    StringBuilder responseStrBuilder = new StringBuilder();
                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null)
                        responseStrBuilder.append(inputStr);
                    JSONObject data = new JSONObject(responseStrBuilder.toString());
                    stream.close();

                    float temp = UnitConverter.convertFahrenheitToCelsius((float) data.getDouble("outTemp"));
                    int outHumidity = data.getInt("outHumidity");
                    long airPressure = UnitConverter.convertInchHGToHpa((float) data.getDouble("barometer"));

                    final WeatherData downloaded = new WeatherData(temp, outHumidity, airPressure);

                    // Updates to View-elements should happen on UI-thread!
                    uiHandler.post(new Runnable() {
                        @SuppressLint({"DefaultLocale", "SetTextI18n"})
                        @Override
                        public void run() {
                            temperatureView.setText(String.format("%.1f", downloaded.getTemperature()) + "°C");
                            pressureView.setText(downloaded.getAirPressure() + " hPa");
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                backgroundHandler.postDelayed(this, WEATHER_DELAY);
            }
        };

        backgroundHandler.post(weatherDownloader);
    }
}
