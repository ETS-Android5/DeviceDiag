package com.pacmac.devicediag;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DisplayInfo extends AppCompatActivity {


    TextView rotation, refreshRate, density, resolution, xyDPI, scaleFactor, absolutDimensions,
            diagonalInch, screenDP, layoutSize, drawSize, name;
    Point size = new Point();
    Display display;
    private final Handler mHandler = new Handler();
    private Runnable timer;
    private ShareActionProvider mShareActionProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_info);

        rotation = (TextView) findViewById(R.id.display_rot);
        refreshRate = (TextView) findViewById(R.id.refresh_rate);
        density = (TextView) findViewById(R.id.density);
        resolution = (TextView) findViewById(R.id.real_dimensions);
        xyDPI = (TextView) findViewById(R.id.xYDpi);
        scaleFactor = (TextView) findViewById(R.id.scaleFactor);
        absolutDimensions = (TextView) findViewById(R.id.absolutDimensions);
        diagonalInch = (TextView) findViewById(R.id.diagonal);
        screenDP = (TextView) findViewById(R.id.screenDP);
        layoutSize = (TextView) findViewById(R.id.layoutSize);
        drawSize = (TextView) findViewById(R.id.drawSize);
        name = (TextView) findViewById(R.id.name);

        display = getWindowManager().getDefaultDisplay();

        // update TextViews
        rotation.setText(getOrient(display.getRotation()));
        refreshRate.setText("" + display.getRefreshRate());
        getRealResolution();
        getLayoutQualifier();
        if (Build.VERSION.SDK_INT >= 17)
            name.setText(display.getName());

    }


    public void getRealResolution() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // since SDK_INT = 1;
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                screenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                screenHeight = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
            } catch (Exception ex) {
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
                Log.e("TAG", "could not resolve real screen metrics properly");
            }
        }
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }

        //getting dimensions in inches
        int densityDpi = metrics.densityDpi;
        double widthInch = (double) screenWidth / (double) metrics.xdpi;
        double heightInch = (double) screenHeight / (double) metrics.ydpi;
        double a = Math.pow(widthInch, 2);
        double b = Math.pow(heightInch, 2);
        double diagonal = Math.sqrt(a + b);




        // display data
        xyDPI.setText(metrics.xdpi + "x" + metrics.ydpi);
        density.setText(metrics.densityDpi + " dpi");
        scaleFactor.setText(metrics.density + "");
        resolution.setText(screenWidth + "x" + screenHeight + " px");
        absolutDimensions.setText(String.format("%.2f", widthInch) + "x" + String.format("%.2f", heightInch) + " in");
        diagonalInch.setText(String.format("%.3f", diagonal) + " in");
        drawSize.setText(densityQualifier(densityDpi));
        //The current width/height of the available screen space, in dp units, corresponding to screen width resource qualifier
        screenDP.setText(160 * screenWidth / metrics.densityDpi + "x" + 160 * screenHeight / metrics.densityDpi + " dp");

    }


    public void getLayoutQualifier() {
        Configuration config = getResources().getConfiguration();

        switch (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                layoutSize.setText("Undefined");
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                layoutSize.setText("Small");
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                layoutSize.setText("Normal");
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                layoutSize.setText("Large");
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                layoutSize.setText("Xlarge");
                break;
            default:
                layoutSize.setText("Undefined");
                break;
        }
    }

    public String densityQualifier(int densityDPI) {

        switch (densityDPI) {
            case DisplayMetrics.DENSITY_LOW:
                return "LDPI";
            case DisplayMetrics.DENSITY_MEDIUM:
                return "MDPI";
            case DisplayMetrics.DENSITY_HIGH:
                return "HDPI";
            case DisplayMetrics.DENSITY_XHIGH:
                return "XHDPI";
            case DisplayMetrics.DENSITY_XXHIGH:
                return "XXHDPI";
            case DisplayMetrics.DENSITY_XXXHIGH:
                return "XXXHDPI";
            case DisplayMetrics.DENSITY_TV:
                return "TVDPI";
            case DisplayMetrics.DENSITY_280:
                return "280DPI";
            case DisplayMetrics.DENSITY_400:
                return "400DPI";
            default:
                return "UNDEFINED";
        }
    }

    public String getOrient(int i) {

        switch (i) {
            case 0:
                return "0 degree rotation";
            case 1:
                return "90 degree rotation";
            case 2:
                return "180 degree rotation";
            case 3:
                return "270 degree rotation";
        }
        return "N/A";
    }


    // SHARE VIA ACTION_SEND
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_share, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        setShareIntent(createShareIntent());
        return true;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.shareTextEmpty));
        return shareIntent;
    }

    private Intent createShareIntent(StringBuilder sb) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        return shareIntent;
    }


    private void updateShareIntent() {

        StringBuilder sb = new StringBuilder();
        sb.append(getResources().getString(R.string.shareTextTitle1));
        sb.append("\n");
        sb.append(Build.MODEL +"\t-\t" + getResources().getString(R.string.title_activity_display_info));
        sb.append("\n");
        sb.append(getResources().getString(R.string.shareTextTitle1));
        sb.append("\n\n");
        //body

        sb.append("Density:\t\t" + density.getText().toString());
        sb.append("\n");
        sb.append("Scale Factor:\t\t" + scaleFactor.getText().toString());
        sb.append("\n");
        sb.append("Refresh Rate:\t\t" + refreshRate.getText().toString());
        sb.append("\n");
        sb.append("Resolution:\t\t" + resolution.getText().toString());
        sb.append("\n");
        sb.append("Dimensions:\t\t" + absolutDimensions.getText().toString());
        sb.append("\n");
        sb.append(screenDP.getText().toString());
        sb.append("\n");
        sb.append("Diagonal:\t\t" + diagonalInch.getText().toString());
        sb.append("\n");
        sb.append("X/Y DPI:\t\t" + xyDPI.getText().toString());
        sb.append("\n");
        sb.append("Orientation:\t\t" + rotation.getText().toString());
        sb.append("\n");
        sb.append("Type:\t\t" + name.getText().toString());
        sb.append("\n");
        sb.append("Layout Size:\t\t" + layoutSize.getText().toString());
        sb.append("\n");
        sb.append("Draw:\t\t" + drawSize.getText().toString());
        sb.append("\n\n");

        sb.append(getResources().getString(R.string.shareTextTitle1));
        setShareIntent(createShareIntent(sb));

    }


    @Override
    public void onResume() {
        super.onResume();
        timer = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        updateShareIntent();
                    }
                });
            }
        };
        mHandler.postDelayed(timer, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(timer);
    }
}
