package com.example.pruebaapp_moviles_c2;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Tag";
    private static final String URL = "http://www.geognos.com/api/en/countries/info/all.json";
    Button Choose;
    Button SeeMap;
    ImageView Country;
    TextView Result;
    TextView txtJson;

    InputImage inputImage;
    TextRecognizer textRecognizer;
    private static final int STORAGE_PERMISSION_CODE = 113;
    ActivityResultLauncher<Intent> intentActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Choose = findViewById(R.id.btnselect);
        Country = findViewById(R.id.imgvcountry);
        Result = findViewById(R.id.txtResult);
        txtJson = findViewById(R.id.txtJSON);
        SeeMap = findViewById(R.id.btnSeeMap);
        SeeMap.setVisibility(View.INVISIBLE);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        intentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        Uri imgUri = data.getData();
                        convertImageToText(imgUri);
                    }
                }
        );

        Choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                intentActivityResultLauncher.launch(i);
                Result.setText("");
            }
        });

        SeeMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getJSON();
            }
        });
    }

    private void convertImageToText(Uri imgUri)
    {
        try {
            inputImage = InputImage.fromFilePath(getApplicationContext(), imgUri);
            Task<Text> result = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(@NonNull @NotNull Text text) {
                            String min = text.getText();
                            Result.setText(min.substring(0,1).toUpperCase() + min.substring(1).toLowerCase());
                            Picasso.get()
                                    .load(imgUri)
                                    .into(Country);
                            SeeMap.setVisibility(View.VISIBLE);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            Result.setText("Error: " + e.getMessage());
                            Log.d(TAG, "Error al convertir: "+ e.getMessage());
                        }
                    });
        }catch (Exception e)
        {
            Log.d(TAG, "Error al convertir: "+ e.getMessage());
        }
    }


    public void getJSON() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                URL,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String country = (String) Result.getText();
                JSONObject jsonObject = null;
                try {
                    jsonObject = response.getJSONObject("Results");
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject obj = new JSONObject(jsonObject.get(key).toString());
                        if (obj.getString("Name").toLowerCase().trim().equals(country.toLowerCase().trim())) {
                            JSONObject city = obj.getJSONObject("Capital");
                            String city_2 = city.getString("Name");
                            JSONObject geo = obj.getJSONObject("GeoRectangle");
                            Double west = geo.getDouble("West");
                            Double East = geo.getDouble("East");
                            Double North = geo.getDouble("North");
                            Double South = geo.getDouble("South");
                            JSONObject ini = obj.getJSONObject("CountryCodes");
                            String iso = ini.getString("iso3");
                            String fips = ini.getString("fips");
                            System.out.println(fips);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.getMessage());
            }
        });
        requestQueue.add(jsObjectRequest);
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
    }

    public void checkPermission(String permission, int requestCode)
    {
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions((MainActivity.this), new String[]{permission},requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == STORAGE_PERMISSION_CODE)
        {
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(MainActivity.this, "Permiso Otorgado", Toast.LENGTH_LONG).show();
            }else
                Toast.makeText(MainActivity.this, "Permiso Denegado", Toast.LENGTH_LONG).show();
        }
    }
}