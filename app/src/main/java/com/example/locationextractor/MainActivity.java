package com.example.locationextractor;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private Context mContext;
    private int FILE_SELECTOR_CODE = 10000;
    private int FILE_SELECTOR_CODE1 = 10001;
    private int DIR_SELECTOR_CODE = 20000;
    private List<Map<Integer, Object>> readExcelList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ListAdapter excelAdapter;
    FileUtil excelUtil;
    Button exportbtn, importbtn, importbtn1;
    LinearLayout ll_bottom;
    ProgressDialog pdia;
    int addindex = 0, latindex = 0, lngindex = 0;
    String date="",displaydate="";
    boolean statusOfGPS = false;
    boolean internetavailable = false;
    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    public static final String PROGRESS_UPDATE = "progress_update";
    public  static String path="";
    TextView tv_records;
    int type=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        //get realm instance
     //   realm = RealmController.with(this).getRealm();
        initViews();
    }

    private void initViews() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        excelUtil = new FileUtil(this);
        recyclerView = findViewById(R.id.excel_content_rv);
        excelAdapter = new ListAdapter(readExcelList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(excelAdapter);
        importbtn = findViewById(R.id.import_excel_btn);
        importbtn1 = findViewById(R.id.import_excel_btn1);
        exportbtn = findViewById(R.id.export_excel_btn);
        ll_bottom = findViewById(R.id.ll_bottom);

        checkPermission();
        registerReceiver();


    }

    private void registerReceiver() {

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);

    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(PROGRESS_UPDATE)) {

                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                //Log.d("API123", download.getProgress() + " current progress");

                if (downloadComplete) {

                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();

                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                Snackbar.make(this.findViewById(android.R.id.content),
                        "Please Grant Permissions to access phone storage",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(
                                            new String[]{Manifest.permission
                                                    .WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION},
                                            PERMISSIONS_MULTIPLE_REQUEST);

                                }
                            }
                        }).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_MULTIPLE_REQUEST);
                }
            }
        } else {
            // write your logic code if permission already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode)
        {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraPermission && readExternalFile)
                    {
                        // write your logic here
                    } else {
                        Snackbar.make(this.findViewById(android.R.id.content),
                                "Please Grant Permissions to upload profile photo",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(
                                                    new String[]{Manifest.permission
                                                            .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                                    PERMISSIONS_MULTIPLE_REQUEST);
                                        }
                                    }
                                }).show();
                    }
                }
                break;
        }
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.import_excel_btn:
//                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//
//                System.out.println("gps status-=-"+statusOfGPS);
              //  locationEnabled();

                LocationManager lm = (LocationManager)
                        getSystemService(Context. LOCATION_SERVICE ) ;
                boolean gps_enabled = false;
                boolean network_enabled = false;
                try {
                    gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
                } catch (Exception e) {
                    e.printStackTrace() ;
                }
                try {
                    network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
                } catch (Exception e) {
                    e.printStackTrace() ;
                }
                if (!gps_enabled && !network_enabled) {
                    new AlertDialog.Builder(MainActivity. this )
                            .setMessage( "Gps not enabled. Please Enable Gps to proceed further." )
                            .setPositiveButton( "Settings" , new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                            startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                        }
                                    })
                            .setNegativeButton( "Cancel" , null )
                            .show() ;
                }
                else {
                    ConnectivityManager connectivityManager
                            = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    internetavailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                    //    openFileSelector(0);
                    if (internetavailable == false) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                        alertDialog.setMessage("Internet Connection not available!! Please turn on internet to proceed further.");
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alertDialog.show();


                    } else {
                        type=0;
                        showdialog(0);
                    }
                }
                break;

            case R.id.import_excel_btn1:
               // LocationManager manager1 = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
               // statusOfGPS = manager1.isProviderEnabled(LocationManager.GPS_PROVIDER);


                LocationManager lm1 = (LocationManager)
                        getSystemService(Context. LOCATION_SERVICE ) ;
                boolean gps_enabled1 = false;
                boolean network_enabled1 = false;
                try {
                    gps_enabled1 = lm1.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
                } catch (Exception e) {
                    e.printStackTrace() ;
                }
                try {
                    network_enabled1 = lm1.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
                } catch (Exception e) {
                    e.printStackTrace() ;
                }
                if (!gps_enabled1 && !network_enabled1) {
                    new AlertDialog.Builder(MainActivity. this )
                            .setMessage( "Gps not enabled. Please Enable Gps to proceed further." )
                            .setPositiveButton( "Settings" , new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                            startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                        }
                                    })
                            .setNegativeButton( "Cancel" , null )
                            .show() ;
                }
                else {
                    ConnectivityManager connectivityManager1
                            = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetworkInfo1 = connectivityManager1.getActiveNetworkInfo();
                    internetavailable = activeNetworkInfo1 != null && activeNetworkInfo1.isConnected();

                    if (internetavailable == false) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                        alertDialog.setMessage("Internet Connection not available!! Please turn on internet to proceed further.");
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alertDialog.show();


                    } else {
                        type=1;
                        showdialog(1);
                    }
                }
                break;

            case R.id.export_excel_btn:
                export(type);
                //saveExcelFile(this,"address");
                break;

            case R.id.reset_btn:
                readExcelList.clear();
                excelAdapter.notifyDataSetChanged();
                ll_bottom.setVisibility(View.GONE);
                importbtn.setVisibility(View.VISIBLE);
                importbtn1.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void locationEnabled () {

    }

    private void export(int type) {
        if (readExcelList.size() > 0) {
            //openFolderSelector();

            new Thread(() -> {
                Log.i(TAG, "doInBackground: Exporting...");

//                        this.runOnUiThread(new Runnable() {
//                            public void run() {
//                                pdia=new ProgressDialog(MainActivity.this);
//                                pdia.setMessage("Exporting.........");
//                                pdia.show();
//                                pdia.setCancelable(false);
//                            }
//                        });

                boolean exportsuccess = excelUtil.writeExcelNew(this, readExcelList,latindex,lngindex,date);
                this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (exportsuccess) {
                            // pdia.dismiss();
                            //     exportbtn.setVisibility(View.VISIBLE);
                            Toast.makeText(mContext, "export successfull",
                                    Toast.LENGTH_SHORT).show();
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setCancelable(false);
                            // set the custom layout
                            final View customLayout = getLayoutInflater().inflate(R.layout.dialog_export, null);
                            builder.setView(customLayout);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            TextView tv_path=(TextView)dialog.findViewById(R.id.tv_path);
                            Button btn_ok = (Button) dialog.findViewById(R.id.btn_ok);
                             tv_records=(TextView)dialog.findViewById(R.id.tv_records);
                        tv_path.setText("Your excel has been exported successfully to path "+path);

                            btn_ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });


                        } else {
                            // pdia.dismiss();
                            Toast.makeText(mContext, "export failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // call upload api
                    }
                });

            }).start();
        } else {
            Toast.makeText(mContext, "please import excel first", Toast.LENGTH_SHORT).show();
        }
    }

    private void showdialog(int type) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_input, null);
        builder.setView(customLayout);
        AlertDialog dialog = builder.create();
        dialog.show();
        Button btn_save = (Button) dialog.findViewById(R.id.btn_save);
        EditText et_add = (EditText) dialog.findViewById(R.id.et_address);
        EditText et_lat = (EditText) dialog.findViewById(R.id.et_lat);
        EditText et_lng = (EditText) dialog.findViewById(R.id.et_lng);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);
        TextView tv_date=(TextView)dialog.findViewById(R.id.tv_date);

        if (type == 0) {
            et_add.setText("7");
            et_lat.setText("4");
            et_lng.setText("5");
        } else {
            et_add.setText("8");
            et_lat.setText("9");
            et_lng.setText("10");
        }
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btn_save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (et_add.getText().toString().equals("")) {
                    showToast("Address index cannot be blank");
                } else if (et_lat.getText().toString().equals("")) {
                    showToast("Latitude index cannot be blank");
                } else if (et_lng.getText().toString().equals("")) {
                    showToast("Longitude index cannot be blank");
                } else {
                    addindex = Integer.parseInt(et_add.getText().toString());
                    latindex = Integer.parseInt(et_lat.getText().toString());
                    lngindex = Integer.parseInt(et_lng.getText().toString());
                    openFileSelector(type);
                    dialog.dismiss();
                }

            }
        });



        Calendar cad= Calendar.getInstance();
        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
         date=sdf1.format(cad.getTime());
        displaydate=sdf.format(cad.getTime());
        tv_date.setText(displaydate);

        tv_date.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                final Calendar newCalendar = Calendar.getInstance();
                final DatePickerDialog StartTime = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, monthOfYear, dayOfMonth);
                        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-yyyy");
                        SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
                        date=sdf1.format(newDate.getTime());
                        displaydate=sdf.format(newDate.getTime());
                        tv_date.setText(displaydate);
                    }

                }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
                StartTime.show();
            }
        });

    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * open local filer to select file
     *
     * @param type
     */
    private void openFileSelector(int type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        if (type == 0) {
            startActivityForResult(intent, FILE_SELECTOR_CODE);
        } else {
            startActivityForResult(intent, FILE_SELECTOR_CODE1);
        }

    }

    /**
     * open the local filer and select the folder
     */
    private void openFolderSelector() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_TITLE,
                System.currentTimeMillis() + ".xlsx");
        startActivityForResult(intent, DIR_SELECTOR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri == null) return;
            Log.i(TAG, "onActivityResult: " + "filePath：" + uri.getPath());
            //select file and import
            importExcelFile(uri, 0);
        } else if (requestCode == FILE_SELECTOR_CODE1 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri == null) return;
            Log.i(TAG, "onActivityResult: " + "filePath：" + uri.getPath());
            //select file and import
            importExcelFile(uri, 1);
        } else if (requestCode == DIR_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri == null) return;
            Log.i(TAG, "onActivityResult: " + "filePath：" + uri.getPath());
            Toast.makeText(mContext, "Exporting...", Toast.LENGTH_SHORT).show();
            //you can modify readExcelList, then write to excel.
//            excelUtil.writeExcelNew(this, readExcelList, uri);
        }
    }

    private void importExcelFile(final Uri uri, int type) {
        new Thread(() -> {
            Log.i(TAG, "doInBackground: Importing...");

            List<Map<Integer, Object>> readExcelNew;
//            = excelUtil.readExcelNew(mContext, uri,
//                    uri.getPath());

            if (type == 0) {
                readExcelNew = excelUtil.readExcelNew(mContext, uri,
                        uri.getPath(), addindex, latindex, lngindex);
            } else {
                readExcelNew = excelUtil.readExcelForLatLong(mContext, uri,
                        uri.getPath(), addindex, latindex, lngindex);
            }


            Log.i(TAG, "onActivityResult:readExcelNew " + ((readExcelNew != null) ? readExcelNew.size() : ""));

            if (readExcelNew != null && readExcelNew.size() > 0) {
                readExcelList.clear();
                readExcelList.addAll(readExcelNew);
                this.runOnUiThread(new Runnable() {
                    public void run() {
                        //  exportbtn.setVisibility(View.VISIBLE);
                        if (type == 0) {
                            importbtn1.setVisibility(View.GONE);
                        } else {
                            importbtn.setVisibility(View.GONE);
                        }
                        ll_bottom.setVisibility(View.VISIBLE);
                    }
                });
                updateUI();

                Log.i(TAG, "run: successfully imported");

                runOnUiThread(() ->
                {
                    Toast.makeText(mContext, "successfully imported", Toast.LENGTH_SHORT).show();
                   // export(type);


                });
            } else {

                runOnUiThread(() -> Toast.makeText(mContext, "no data", Toast.LENGTH_SHORT).show());
            }


        }).start();
    }

    public static void setPath(String filepath)
    {
        path=filepath;
        System.out.println("path---"+path);
    }


    /**
     * refresh RecyclerView
     */
    private void updateUI() {
        runOnUiThread(() -> {
            if (readExcelList != null && readExcelList.size() > 0) {
                excelAdapter.notifyDataSetChanged();

            }
        });
    }
}
