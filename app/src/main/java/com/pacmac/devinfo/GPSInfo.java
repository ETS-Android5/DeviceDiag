package com.pacmac.devinfo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;


/**
 * Created by pacmac on 6/10/2015.
 */

public class GPSInfo extends AppCompatActivity implements LocationListener {

    String gpsInfo = null;
    String timeToFix = "Waiting...";
    String longitude = null;
    String latitude = null;
    String altitude = null;
    String speed = null;
    String accuracy = null;
    String bearing = null;
    String lastFix = null;
    private final String FRAG_GPS_INFO = GpsInfoLocation.class.getSimpleName();
    private final String FRAG_GPS_SATS = GPSSatelitesListFrag.class.getSimpleName();
    private final String FRAG_SAVE = "frag_save";
    private String fragTag = null;
    private GpsStatus gpsStatus;
    private LocationManager locationManager;
    private GpsStatus.Listener gpsStatusListener;

    private Iterable<GpsSatellite> gpsSatelites;
    private ArrayList<Satelites> satelites = new ArrayList<Satelites>();
    double locLat, locLong;

    private boolean enabled = false;
    private Fragment fragment;
    FragmentTransaction ft = null;
    private ShareActionProvider mShareActionProvider;

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private boolean isPermissionEnabled = true;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (fragment instanceof GpsInfoLocation) {
            outState.putString(FRAG_SAVE, FRAG_GPS_INFO);
        } else {
            outState.putString(FRAG_SAVE, FRAG_GPS_SATS);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {


        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_info_base);

        // Check if user disabled LOCATION permission at some point
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(), LOCATION_PERMISSION);
        }
        if (!isPermissionEnabled) {
            Utility.requestPermissions(this, LOCATION_PERMISSION);
        }
        // PORTRAIT

        if (savedInstanceState == null) {
            fragment = new GpsInfoLocation();
            fragTag = FRAG_GPS_INFO;
        } else if (savedInstanceState.getString(FRAG_SAVE).equals(FRAG_GPS_INFO)) {
            fragment = getSupportFragmentManager().findFragmentByTag(FRAG_GPS_INFO);
            fragTag = FRAG_GPS_INFO;
        } else {
            fragment = getSupportFragmentManager().findFragmentByTag(FRAG_GPS_SATS);
            if (fragment.isVisible()) {
                fragTag = FRAG_GPS_SATS;
            }
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.gpsPortrait, fragment, fragTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {

            locationManager = (LocationManager) getApplicationContext().getSystemService(getApplicationContext().LOCATION_SERVICE);
            // check if GPS provider is enabled
            enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!enabled && fragment.isVisible()) {
                ((GpsInfoLocation) fragment).showAlertOnDisabled();
            }


            // GPS STATUS Listener
            gpsStatusListener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {

                    gpsStatus = locationManager.getGpsStatus(null);

                    switch (event) {
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                            timeToFix = gpsStatus.getTimeToFirstFix() + " ms";
                            gpsInfo = "First Fix";
                            break;

                        case GpsStatus.GPS_EVENT_STARTED:
                            gpsInfo = "Starting";
                            break;

                        case GpsStatus.GPS_EVENT_STOPPED:
                            gpsInfo = "Inactive";
                            break;

                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                            gpsInfo = "Active";
                            satelites.clear();
                            gpsSatelites = gpsStatus.getSatellites();
                            Iterator<GpsSatellite> satelliteIterator = gpsSatelites.iterator();
                            int i = 1;
                            while (satelliteIterator.hasNext()) {
                                GpsSatellite satellite = satelliteIterator.next();
                                if (satellite.usedInFix()) {
                                    //  Log.d("TAG", "sat:" + (i) + ": PNR:" + satellite.getPrn() + ",SNR:" + String.format("%.02f", satellite.getSnr()) + ",azimuth:" + satellite.getAzimuth() + ",elevation:" + satellite.getElevation() + "\n\n");
                                    satelites.add(new Satelites(i, satellite.getSnr(), satellite.getPrn(), satellite.getAzimuth(), satellite.getElevation()));
                                    //
                                    i++;
                                }

                            }

                            if (fragment instanceof GPSSatelitesListFrag && fragment.isVisible() && fragment != null)
                                ((GPSSatelitesListFrag) fragment).invalidateListView(satelites);
                            break;


                    }

                    if (fragment instanceof GpsInfoLocation && fragment.isVisible() && fragment != null)
                        ((GpsInfoLocation) fragment).displayGPSStatus(gpsInfo, timeToFix);
                }

            };

        } else if (isPermissionEnabled) {
            Toast.makeText(getApplicationContext(), "GPS is not available on this device.", Toast.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onResume() {
        if (isPermissionEnabled) {
            if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
                locationManager.addGpsStatusListener(gpsStatusListener);
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (isPermissionEnabled) {
            if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
                locationManager.removeGpsStatusListener(gpsStatusListener);
                locationManager.removeUpdates(this);
            }
        }
        super.onPause();
    }


    @Override
    public void onLocationChanged(Location location) {

        locLat = location.getLatitude();
        locLong = location.getLongitude();
        boolean isConnected = false;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo networkInfo;

        // check WIFI state and if present in device
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            isConnected = networkInfo.isConnectedOrConnecting();
        } else if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            isConnected = networkInfo.isConnectedOrConnecting();
        }


        if (isConnected) {
            // geocoder will resolve


            new Thread(new Runnable() {

                @Override
                public void run() {

                    // TODO don't do this on every loc update - check for distance to update 
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    if (geocoder.isPresent()) {
                        try {
                            List<Address> addresses = geocoder.getFromLocation(locLat, locLong, 1);
                            if (addresses == null) return;
                            String street = addresses.get(0).getThoroughfare();
                            String numHouse = addresses.get(0).getSubThoroughfare();
                            String city = addresses.get(0).getSubAdminArea();
                            String postalCode = addresses.get(0).getPostalCode();

                            street = street == null ? "" : street;
                            numHouse = numHouse == null ? "" : numHouse;
                            city = city == null ? "" : city;
                            postalCode = postalCode == null ? "" : postalCode;


                            //display address in ACTION BAR
                            runOnUiThread(new RunnableShowTitle(street, numHouse, city, postalCode) {
                                @Override
                                public void run() {
                                    getSupportActionBar().setTitle(getStreet() + " " + getNumHouse());
                                    getSupportActionBar().setSubtitle(getCity() + " " + getPostalCode());
                                }
                            });


                        } catch (IOException ex) {
                            ex.printStackTrace(); // will throw if no connection to server
                        }
                    }


                }
            }).start();

        } else {
            getSupportActionBar().setTitle(getResources().getString(R.string.activity_title_gps_information));
            getSupportActionBar().setSubtitle("");
        }


        int speedInt = (int) (location.getSpeed() * 3.6f);
        long timeRaw = location.getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeRaw);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        // display Location on screen
        lastFix = hour + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
        latitude = location.getLatitude() + "";
        longitude = location.getLongitude() + "";
        altitude = String.format("%.01f", location.getAltitude()) + " m";
        speed = speedInt + " km/s";
        accuracy = String.format("%.01f", location.getAccuracy()) + " m";
        bearing = String.format("%.02f", location.getBearing()) + "°";

        if (fragment instanceof GpsInfoLocation && fragment.isVisible()) {
            ((GpsInfoLocation) fragment).displayGPSData(longitude, latitude, altitude,
                    speed, accuracy, bearing, lastFix);
        }

        updateShareIntent();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (fragment instanceof GPSSatelitesListFrag) {
            fragment = new GpsInfoLocation();
            ft = getSupportFragmentManager().beginTransaction().replace(R.id.gpsPortrait, fragment, FRAG_GPS_INFO);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.action_satellites) {
            if (fragment instanceof GpsInfoLocation)
                showSatListFrag();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSatListFrag() {
        fragment = new GPSSatelitesListFrag();
        ft = getSupportFragmentManager().beginTransaction().replace(R.id.gpsPortrait, fragment, FRAG_GPS_SATS);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(FRAG_GPS_SATS);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_gps, menu);

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

        if (lastFix != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getResources().getString(R.string.shareTextTitle1));
            sb.append("\n");
            sb.append(Build.MODEL + "\t-\t" + getResources().getString(R.string.activity_title_gps_information));
            sb.append("\n");
            sb.append(getResources().getString(R.string.shareTextTitle1));
            sb.append("\n\n");
            //body
            sb.append("Last location:\t\t" + lastFix);
            sb.append("\n");
            sb.append("Time to first fix:\t\t" + timeToFix);
            sb.append("\n");
            sb.append("Location:\t\t");
            sb.append(latitude + ", " + longitude);
            sb.append("\n");
            sb.append("Altitude:\t\t" + altitude);
            sb.append("\n");
            sb.append("Speed:\t\t" + speed);
            sb.append("\n");
            sb.append("Accuracy:\t\t" + accuracy);
            sb.append("\n");
            sb.append("Bearing:\t\t" + bearing);
            sb.append("\n\n");

            sb.append(getResources().getString(R.string.shareTextTitle1));
            setShareIntent(createShareIntent(sb));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        isPermissionEnabled = Utility.checkPermission(getApplicationContext(), LOCATION_PERMISSION);
    }
}