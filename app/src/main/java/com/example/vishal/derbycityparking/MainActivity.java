package com.example.vishal.derbycityparking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity
{

    private static final int REQUEST_WES = 0;
    String url = "https://www.dillonbrock.me/parking.php";
    String urlR = "https://www.dillonbrock.me/reserve.php";
    String guid = new String("");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AWSMobileClient.getInstance().initialize(this).execute();
        setContentView(R.layout.activity_main);
        Button parkingImage =  findViewById(R.id.parkingimage);
        Button reserveButton = findViewById(R.id.button);
        getData();

        parkingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                getData();
            }
        });
        reserveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                reserve();
            }
        });



    }

    public void reserve()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        getData();
        StringRequest stringRequest = new StringRequest
                (Request.Method.GET, urlR, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response)
                    {
                        guid = response;
                        Log.d("GUID: ",guid);
                        getData();
                        Intent intent = new Intent(MainActivity.this, QRCodeView.class);
                        intent.putExtra("GUID",guid);
                        startActivity(intent);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        queue.add(stringRequest);
    }


    public void getData()
    {
        final TextView openSpaces = findViewById(R.id.openSpaces);
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        int nOpen = 0;
                        int nReserved = 0;
                        try {
                            nOpen = response.getInt("open_spaces");
                            nReserved = response.getInt("reserved_spaces");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String message = "Available spaces: " + String.valueOf(nOpen - nReserved);
                        openSpaces.setText(message);
                        openSpaces.setGravity(Gravity.CENTER);
                        if(nOpen - nReserved <=0)
                        {
                            Button reserve = findViewById(R.id.button);
                            reserve.setClickable(false);
                        }

                        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        {
                            permission();
                        }
                        else
                        {
                            downloadImage();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        queue.add(jsObjRequest);
    }
    public void permission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WES);

            }

        int permissionCheckRES = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_WES:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    downloadImage();
                }
                else
                {

                }
                return;
            }

        }
    }



     public void downloadImage()
     {
         final ImageView parkingImage = findViewById(R.id.imageView);
         TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(this.getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        TransferObserver downloadObserver =
                transferUtility.download(
                        "output.jpg",
                        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"output.jpg")
);
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                if (percentDone == 100)
                {
                    File imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"output.jpg");
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    parkingImage.setImageBitmap(myBitmap);
                }

                Log.d("MainActivity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex)
            {
                Log.e("ERRRRRRRROR", ex.getMessage());
            }

        });
    }


}


