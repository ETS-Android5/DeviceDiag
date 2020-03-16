package com.pacmac.devinfo.storage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.pacmac.devinfo.ExportActivity;
import com.pacmac.devinfo.R;
import com.pacmac.devinfo.UIObject;
import com.pacmac.devinfo.cellular.BasicItemAdapter;
import com.pacmac.devinfo.cpu.CPUViewModel;
import com.pacmac.devinfo.utils.ExportTask;
import com.pacmac.devinfo.utils.ExportUtils;

import java.util.ArrayList;
import java.util.List;

public class StorageInfo extends AppCompatActivity implements ExportTask.OnExportTaskFinished {

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLinearLayoutManager;
    private BasicItemAdapter mItemAdapter;

    private boolean isExporting = false;
    private StorageViewModel viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_info);


        viewModel = new ViewModelProvider(this).get(StorageViewModel.class);

        mRecyclerView = findViewById(R.id.recylerView);
        mRecyclerView.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mItemAdapter = new BasicItemAdapter(this, new ArrayList<>());
        mRecyclerView.setAdapter(mItemAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Observer<List<UIObject>> basicObserver = uiObjects -> mItemAdapter.updateData(uiObjects);
        viewModel.getStorageInfo(getApplicationContext()).observe(this, basicObserver);

    }

    // SHARE CPU INFO VIA ACTION_SEND
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menu_item_share) {
            if (!isExporting) {
                isExporting = true;
                new ExportTask(getApplicationContext(), StorageViewModel.EXPORT_FILE_NAME, this).execute(viewModel);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onExportTaskFinished(String filePath) {
        isExporting = false;
        if (filePath != null) {
            Intent intent = new Intent(getApplicationContext(), ExportActivity.class);
            intent.putExtra(ExportUtils.EXPORT_FILE, filePath);
            startActivity(intent);
        }
    }
}
