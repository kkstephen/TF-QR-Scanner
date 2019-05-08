package com.stephen.tfscanner;

import java.util.*;
import java.io.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.*;
import android.app.AlertDialog;
import com.stephen.core.*;

public class DataActivity extends AppCompatActivity {

    private ListView grid;
    private TextView lbNumber;
    private AlertDialog alertDialog;
    private ClipboardManager clipboard;

    private List<TagLabel> _data;
    private EntityAdapter adapter;
    private TagRepository repo;

    private TagLabel current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        this.repo = new TagRepository(this);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        this.lbNumber = this.findViewById(R.id.txtCount);
        this.grid = this.findViewById(R.id.listView);

        this.grid.setOnItemClickListener((parent, view, pos, i) -> {
            String info = _data.get(pos).getCode();
            Toast.makeText(DataActivity.this, info, Toast.LENGTH_SHORT).show();

            view.setSelected(true);
        });

        this.grid.setOnCreateContextMenuListener((menu, view, info) -> {
            AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) info;

            current = this._data.get(menuInfo.position);

            //menu.setHeaderTitle("Action");
            menu.add(0, 0, 1, "Copy");
            menu.add(0, 1, 2, "Delete");

            if (URLUtil.isValidUrl(current.getCode())) {
                menu.add(0, 2, 3, "Open");
            }
        });
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = this.getSharedPreferences("com.stephen.tqs", Context.MODE_PRIVATE);

        try
        {
            this.repo.Open();

            this._data = this.repo.ToList("createdate desc");

            this.lbNumber.setText(String.valueOf(this._data.size()));
            this.adapter = new EntityAdapter(this, R.layout.row_item,  _data);

            grid.setAdapter(adapter);
        }
        catch (Exception e)
        {
            Toast.makeText(DataActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        this.repo.Close();

        super.onResume();
    }

    @Override
    public void onStop() {

        if (_data != null) {
            this._data.clear();
            this._data = null;
        }

        super.onStop();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {

            case 0:
                clipboard.setPrimaryClip(ClipData.newPlainText("qrcode", current.getCode()));
                Toast.makeText(DataActivity.this, "Copy OK: " + current.getCode(), Toast.LENGTH_LONG).show();
                break;

            case 1:
                delete(current);
                break;

            case 2:
                String url = current.getCode();

                if (url != "") {
                    Uri uri = Uri.parse(url);

                    if(uri != null) {
                        // create alert dialog
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                        // set title
                        alertDialogBuilder.setTitle("Open this link?");

                        // set dialog message
                        alertDialogBuilder
                                .setMessage(url)
                                .setCancelable(true)
                                .setPositiveButton("Yes", (dialog, aid) -> {
                                    Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(browser);
                                })
                                .setNegativeButton("No", (dialog, aid) -> {
                                    dialog.cancel();
                                });

                        alertDialog = alertDialogBuilder.create();

                        // show it
                        alertDialog.show();
                    }
                }
                break;

            default:
                break;
        }

        return true;
    }

    public void onClear(View v) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Clear History");

        // set dialog message
        alertDialogBuilder
                .setMessage("Are you sure delete all data?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    clearData();
                })
                .setNegativeButton("No", (dialog, id) -> {
                    dialog.cancel();
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void clearData() {
        try {
            this.repo.Open();

            this.repo.Clear();

            if (this._data != null && this.adapter != null) {
                this._data.clear();
                this.adapter.notifyDataSetChanged();

                this.lbNumber.setText("0");
            }

            Toast.makeText(DataActivity.this, "Delete data success.", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {
            Toast.makeText(DataActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        this.repo.Close();
    }

    private void delete(TagLabel label)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Delete");

        // set dialog message
        alertDialogBuilder
                .setMessage(label.getCode())
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    try {
                        this.repo.Open();
                        this.repo.Delete(label);

                        _data.remove(current);
                        adapter.notifyDataSetChanged();

                        this.lbNumber.setText(String.valueOf(this._data.size()));

                        current = null;

                        Toast.makeText(DataActivity.this, "Delete OK.", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception e)
                    {
                    }

                    this.repo.Close();
                })
                .setNegativeButton("No", (dialog, id) -> {
                    dialog.cancel();
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void onSave(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Get the directory for the user's public pictures directory.

            if (isExtStorageWritable()) {
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "ThunderQR");

                if (!file.exists()) {
                    file.mkdirs();
                }

                this.saveData(file);
            }
        }
    }

    private void saveData(File dir) {
        String fileName = dir.getAbsolutePath() + File.separator + "code.csv";

        try {

            FileWriter out = new FileWriter(fileName, false);

            for (TagLabel tag : _data) {
                String str = tag.getCode() + "," + StringHelper.ToDateTime(tag.getCreatedate()) + "\r\n";

                out.append(str);
            }

            out.flush();
            out.close();

            Toast.makeText(DataActivity.this, "Save ok: " + fileName, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Toast.makeText(DataActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isExtStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }
}
