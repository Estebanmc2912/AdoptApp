package com.example.adoptapp.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adoptapp.R;
import com.example.adoptapp.model.Solicitud;
import com.example.adoptapp.utils.FirebaseUtils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ActivityRecogerAdoptado extends AppCompatActivity implements OnMapReadyCallback {

    private Solicitud solicitud;

    private TextView textViewDescripcion;
    private ImageView imageViewFoto;
    private SupportMapFragment mapFragment;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 4;
    private static final int REQUEST_CHECK_SETTINGS = 5;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private String TAG = "Recoger adoptado";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private static String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    private LatLng latLng1, latLng2;

    private GeoPoint ubicacionCliente;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng latLng = new LatLng(4.657777, -74.093353);

        float zoom = 13; // Un zoom mayor que 13 hace que el emulador falle, pero un valor deseado para
        // callejero es 17 aprox.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recoger_adoptado);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if(currentUser==null){
            Toast.makeText(this,
                    "Debes iniciar sesión en AdoptApp para ver esta información",
                    Toast.LENGTH_LONG).show();
            finishAffinity();
        }

        db = FirebaseFirestore.getInstance();

        solicitud = (Solicitud) getIntent().getSerializableExtra("solicitud");

        textViewDescripcion = findViewById(R.id.tv_informacion_adopcion);
        imageViewFoto = findViewById(R.id.iv_foto_ir_adoptado);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_recoger_animal);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();

        String descripcion = "Ya puedes ir a recoger a "+solicitud.getNombreAnimal()+".\n\n"+
                solicitud.getNombreInstitucion()+" ha aceptado tu solicitud de adopción después " +
                "de que has cumplido con sus requisitos. Felicitaciones!!!\n"+
                solicitud.getNombreAnimal()+" te agradecerá por darle una segunda oportunidad en la " +
                "vida.";
        textViewDescripcion.setText(descripcion);

        new Thread(new Runnable() {
            public void run() {
                // a potentially time consuming task
                if (solicitud.getFotoUrl() != null && !solicitud.getFotoUrl().equals("")) {
                    //descargar la imagen
                    FirebaseUtils.descargarFotoImageView(solicitud.getFotoUrl(), imageViewFoto);
                }
            }
        }).start();

        ubicacionCliente = null;
        latLng2 = null;

        hacerBusquedaLocalizacion();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                final Location location = locationResult.getLastLocation();
                Log.i("LOCATION", "Location update in the callback: " + location);
                if (location != null) {
                    ubicacionCliente = new GeoPoint(location.getLatitude(), location.getLongitude());
                    stopLocationUpdates();
                }
            }
        };

        if (ContextCompat.checkSelfPermission(ActivityRecogerAdoptado.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            revisarActivacionGPS();
        }else{
            requestPermission(ActivityRecogerAdoptado.this, PERMISSIONS[0], "Acceso a GPS necesario",
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

    }

    private void requestPermission(Activity context, String permiso, String justificacion, int idCode){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permiso)) {
                Toast toast = Toast.makeText(context, justificacion, Toast.LENGTH_SHORT);
                toast.show();
            }
            //Request the permission
            ActivityCompat.requestPermissions(context, new String[]{permiso}, idCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK) {
                    startLocationUpdates(); //Se encendió la localización!!!
                } else {
                    Toast.makeText(this,
                            "Sin acceso a localización, hardware deshabilitado!",
                            Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {

            case MY_PERMISSIONS_REQUEST_LOCATION:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, continue with task related to permission
                    revisarActivacionGPS();
                }else{

                    Toast.makeText(this,
                            "Sin acceso a localización, permiso denegado!",
                            Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //tasa de refresco en milisegundos
        mLocationRequest.setFastestInterval(1000); //máxima tasa de refresco
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void startLocationUpdates() {
        //Verificación de permiso!!
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private void revisarActivacionGPS(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationSettingsRequest.Builder builder = new
                    LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    startLocationUpdates(); //Todas las condiciones para recibir localizaciones
                }
            });

            task.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case CommonStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                            try {// Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(ActivityRecogerAdoptado.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sendEx) {
                                // Ignore the error.
                            } break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. No way to fix the settings so we won't show the dialog.
                            break;
                    }
                }
            });

        }else{
            Toast.makeText(this,
                    "Sin acceso a localización, permiso denegado!",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void hacerBusquedaLocalizacion(){

        db.collection("animales").document(solicitud.getIdAnimal())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        GeoPoint ubicacion = document.getGeoPoint("Ubicacion");
                        latLng2 = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
                        Log.d("TAG", "Se obtuvo documento de "+document.getString("Nombre"));
                        while(ubicacionCliente == null) {
                            ;
                        }
                        actualizarMapa();
                    }
                }
            }
        });

    }

    private void actualizarMapa(){

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                latLng1 = new LatLng(ubicacionCliente.getLatitude(), ubicacionCliente.getLongitude());
                float zoom = 13;

                Marker inicio = googleMap.addMarker(new MarkerOptions()
                        .position(latLng1)
                        .title("Tu ubicación")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng1, zoom));

                Marker ubicacionReporte = googleMap.addMarker(new MarkerOptions()
                        .position(latLng2)
                        .title("Ubicación de "+solicitud.getNombreAnimal())
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setCompassEnabled(true);
                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng2, zoom));

                stopLocationUpdates();
            }
        });

    }
}
