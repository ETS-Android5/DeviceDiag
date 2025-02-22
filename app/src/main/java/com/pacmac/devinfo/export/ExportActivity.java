package com.pacmac.devinfo.export;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.widget.ImageViewCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.pacmac.devinfo.R;

import java.io.File;

import static com.pacmac.devinfo.export.ExportUtils.EXPORT_SLOT_AVAILABLE;

public class ExportActivity extends AppCompatActivity {

    private final static int PROMO_REQUEST_CODE = 8;

    private String filePathString;
    private RewardedAd rewardedAd;
    private SharedPreferences sharedPreferences;

    private TextView exportSlotCounter;
    private Button watchVideoBtn;
    private Button exportBtn;
    private AppCompatImageView slot1;
    private AppCompatImageView slot2;
    private AppCompatImageView slot3;
    private AppCompatImageView slot4;
    private AppCompatImageView slot5;

    private ProgressBar progressBar;


    private int slotCount = 0;
    private int error = -1;

    private boolean isAdLoading = false;
    private boolean userClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        createAndLoadRewardedAd();

        exportSlotCounter = findViewById(R.id.exportSlotCounter);
        slot1 = findViewById(R.id.slot1);
        slot2 = findViewById(R.id.slot2);
        slot3 = findViewById(R.id.slot3);
        slot4 = findViewById(R.id.slot4);
        slot5 = findViewById(R.id.slot5);
        exportBtn = findViewById(R.id.exportButton);
        watchVideoBtn = findViewById(R.id.watchVideoBtn);
        exportSlotCounter = findViewById(R.id.exportSlotCounter);

        progressBar = findViewById(R.id.progress);

        sharedPreferences = getSharedPreferences(ExportUtils.EXPORT_SHARED_PREF_FILE, MODE_PRIVATE);
        slotCount = sharedPreferences.getInt(EXPORT_SLOT_AVAILABLE, 0);

        updateSlotViews(slotCount);


        Intent intent = getIntent();
        if (intent != null) {
            filePathString = intent.getStringExtra(ExportUtils.EXPORT_FILE);
        }


        exportBtn.setOnClickListener(v -> {
            slotCount -= 1;
            if (slotCount < 0) {
                slotCount = 0;
            }
            sharedPreferences.edit().putInt(EXPORT_SLOT_AVAILABLE, slotCount).apply();
            updateSlotViews(slotCount);

            File exportFile = new File(filePathString);
            ExportUtils.sendShareIntent(ExportActivity.this, exportFile);
        });


        watchVideoBtn.setOnClickListener(v -> {
            userClick = true;
            progressBar.setVisibility(View.VISIBLE);
            if (!isAdLoading) {
                if (rewardedAd != null && error != AdRequest.ERROR_CODE_NETWORK_ERROR) {
                    watchVideoBtn.setEnabled(false);
                    userClick = false;
                    rewardedAd.show(ExportActivity.this, onUserEarnedRewardListener);
                } else if (error == AdRequest.ERROR_CODE_NETWORK_ERROR) {
                    createAndLoadRewardedAd();
                    Toast.makeText(getApplicationContext(), R.string.check_internet_connection, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", "The rewarded ad wasn't loaded yet.");
                    createAndLoadRewardedAd();
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void createAndLoadRewardedAd() {
        isAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getResources().getString(R.string.rewarded1), adRequest, adShowCallback);
    }

    private RewardedAdLoadCallback adShowCallback = new RewardedAdLoadCallback() {
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            rewardedAd = null;
            Log.d("PACMAC-EXPORT", "onAdFailedToLoad:" + loadAdError.getMessage());
            error = loadAdError.getCode();
            isAdLoading = false;
            userClick = false;
            watchVideoBtn.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
            Log.d("PACMAC-EXPORT", "onAdLoaded");
            rewardedAd.setFullScreenContentCallback(fullScreenContentCallback);
            ExportActivity.this.rewardedAd = rewardedAd;
            isAdLoading = false;
            error = -1;
            watchVideoBtn.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
            if (userClick) {
                rewardedAd.show(ExportActivity.this, onUserEarnedRewardListener);
            }
            userClick = false;
        }
    };


    private OnUserEarnedRewardListener onUserEarnedRewardListener = new OnUserEarnedRewardListener() {
        @Override
        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            if (slotCount < 5) {
                slotCount += 1;
                sharedPreferences.edit().putInt(EXPORT_SLOT_AVAILABLE, slotCount).apply();
                updateSlotViews(slotCount);
                Toast.makeText(getApplicationContext(), R.string.earning_export_slots, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.export_slots_unlocked, Toast.LENGTH_LONG).show();
            }
        }

    };

   private FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            Log.d("PACMAC-EXPORT", "Ad was shown.");
            watchVideoBtn.setEnabled(false);
            createAndLoadRewardedAd();
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            // Called when ad fails to show.
            Log.d("PACMAC-EXPORT", "Ad failed to show: " + adError.getMessage());
            rewardedAd = null;
            createAndLoadRewardedAd();
            startActivityForResult(new Intent(getApplicationContext(), PromoActivity.class), PROMO_REQUEST_CODE);
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            // Set the ad reference to null so you don't show the ad a second time.
            Log.d("PACMAC-EXPORT", "Ad was dismissed.");
            rewardedAd = null;
        }
    };


    private void updateSlotViews(int slotCount) {
        exportSlotCounter.setText(String.valueOf(slotCount));

        ImageViewCompat.setImageTintList(slot1, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_disabled)));
        ImageViewCompat.setImageTintList(slot2, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_disabled)));
        ImageViewCompat.setImageTintList(slot3, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_disabled)));
        ImageViewCompat.setImageTintList(slot4, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_disabled)));
        ImageViewCompat.setImageTintList(slot5, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_disabled)));

        if (slotCount > 0) {
            exportBtn.setEnabled(true);
            ImageViewCompat.setImageTintList(slot1, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_earned)));
            if (slotCount > 1) {
                ImageViewCompat.setImageTintList(slot2, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_earned)));
            }
            if (slotCount > 2) {
                ImageViewCompat.setImageTintList(slot3, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_earned)));
            }
            if (slotCount > 3) {
                ImageViewCompat.setImageTintList(slot4, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_earned)));
            }
            if (slotCount > 4) {
                ImageViewCompat.setImageTintList(slot5, ColorStateList.valueOf(getResources().getColor(R.color.export_slot_earned)));
            }
        } else {
            exportBtn.setEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROMO_REQUEST_CODE && resultCode == RESULT_OK) {
            if (slotCount < 5) {
                slotCount += 1;
                sharedPreferences.edit().putInt(EXPORT_SLOT_AVAILABLE, slotCount).apply();
                updateSlotViews(slotCount);
                Toast.makeText(getApplicationContext(), R.string.earning_export_slots, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.export_slots_unlocked, Toast.LENGTH_LONG).show();
            }
        }
    }
}
