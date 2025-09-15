package com.example.parcellocker.ui.feauture;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;


import com.example.parcellocker.R;
import com.example.parcellocker.cu16.CU16Client;
import com.example.parcellocker.cu16.CU16Service;
import com.example.parcellocker.db.DoorDao;
import com.example.parcellocker.db.DoorEntity;
import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.ui.feauture.adapter.*;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class TestActivity extends AppCompatActivity {

    private CU16Service cu16Service;
    private DoorDao doorDao;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private DoorListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // ✅ Init DB
        MachineDatabase db = MachineDatabase.getInstance(this);
        doorDao = db.doorDao();
        seedIfEmpty();

        // ✅ Init CU16 service (simulator endpoint)
        CU16Client client = new CU16Client("172.22.7.31", 3133); // Emulator → Host
        cu16Service = new CU16Service(client, 0);

        // ✅ RecyclerView setup
        RecyclerView rv = findViewById(R.id.recycler_doors);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoorListAdapter(cu16Service); // Adapter now only takes service
        rv.setAdapter(adapter);

        // ✅ Observe LiveData (auto-refresh when DB changes)
        doorDao.getDoorsLive("M001").observe(this, new Observer<List<DoorEntity>>() {
            @Override
            public void onChanged(List<DoorEntity> doors) {
                adapter.submitList(doors);  // Uses ListAdapter’s diff util
            }
        });

        // ✅ Unlock All button
        Button btnAll = findViewById(R.id.btn_unlock_all);
        btnAll.setOnClickListener(v -> {
            exec.submit(() -> {
                try {
                    cu16Service.unlockAllAsync().get();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Unlock all command sent", Toast.LENGTH_SHORT).show()
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            });
        });
    }

    // ✅ Seed database with default doors if empty
    private void seedIfEmpty() {
        exec.submit(() -> {
            if (doorDao.getAll().isEmpty()) {
                for (int i = 0; i < 16; i++) {
                    DoorEntity d = new DoorEntity();
                    d.machineId = "M001";
                    d.cuId = 0;
                    d.doorIndex = i;
                    d.locked = 1;
                    d.occupied = 0;
                    d.label = "Locker " + (i + 1);
                    doorDao.insert(d);
                }
            }
        });
    }
}