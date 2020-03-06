package com.example.olungavanthuru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    TextView name, distance, duration, Temp, City, Description, Date, Sim1No, Sim2No, Sim1Op, Sim2Op;
    TextView Network,Myorientation;
    int orientation;
    boolean wifi,mobile;
    double weatherlat, weatherlng;
    private String time;
    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    AutocompleteSupportFragment places_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initplaces();
        setupPlaceAutoComplete();
        name = (TextView) findViewById(R.id.textView);
        distance = (TextView) findViewById(R.id.distance);
        duration = (TextView) findViewById(R.id.duration);
        Date = (TextView) findViewById(R.id.date);
        City = (TextView) findViewById(R.id.city);
        Temp = (TextView) findViewById(R.id.temp);
        Description = (TextView) findViewById(R.id.description);
        Sim1No = (TextView) findViewById(R.id.sim1no);
        Sim1Op = (TextView) findViewById(R.id.sim1op);
        Sim2No = (TextView) findViewById(R.id.sim2no);
        Sim2Op = (TextView) findViewById(R.id.sim2op);
        //Myorientation=(TextView)findViewById(R.id.orientation);
        SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},2);
                    }
        List<SubscriptionInfo> subinfoList = subscriptionManager.getActiveSubscriptionInfoList();
        Log.i("Simdetails","length"+subinfoList.size());
        String number1=subinfoList.get(0).getNumber();
        String number2=subinfoList.get(1).getNumber();
        String op1=String.valueOf(subinfoList.get(0).getCarrierName());
        String op2=String.valueOf(subinfoList.get(1).getCarrierName());
        Sim1No.setText("SIM 1 NO: "+number1);
        Sim2No.setText("SIM 1 NO: "+number2);
        Sim1Op.setText(op1);
        Sim2Op.setText(op2);
        WifiManager wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo=wifiManager.getConnectionInfo();
    }

    public void checkBTpermission()
    {

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissioncheck=this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissioncheck+=this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissioncheck!=0)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1000);
            }
        }
        else
        {
            Toast.makeText(this, "No need of checking permission", Toast.LENGTH_SHORT).show();
        }
    }
    private void findOrientation() {
        orientation=this.getResources().getConfiguration().orientation;
        if(orientation== Configuration.ORIENTATION_PORTRAIT)
        {
            Myorientation.setText("Portrait Mode");
        }
        else
        {
            Myorientation.setText("Landscape Mode");
        }
    }


    private void checkConnectionStatus() {
        ConnectivityManager connectivityManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeinfo=connectivityManager.getActiveNetworkInfo();
        if(activeinfo!=null && activeinfo.isConnected())
        {
            wifi=activeinfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobile=activeinfo.getType()==ConnectivityManager.TYPE_MOBILE;
            if (wifi)
            {
                Network.setText("WIFI");
            }
            else if (mobile)
            {
                Network.setText("Mobile");
            }

        }
        else {
            Network.setText("NO Internet");
        }
    }

    public class  GetDistanceAsyncTask extends AsyncTask<Void,Void,String>{
        double lat1;
        double lng1;
        double lat2;
        double lng2;

        public GetDistanceAsyncTask(double lat1,double lng1,double lat2,double lng2)
        {
            this.lat1=lat1;
            this.lng1=lng1;
            this.lat2=lat2;
            this.lng2=lng2;
            this.execute();
        }
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            String dis="";
            String url="https://maps.googleapis.com/maps/api/distancematrix/xml?origins=" +lat1+","+lng1+"&destinations=" +
                    lat2+","+lng2+"&key=AIzaSyBArquOWZ8gu-gS4ce2P2757iBov4yhPXI";
            String[] tag={"text"};
            try {

                URL url1 = new URL(url);
                HttpsURLConnection connection = (HttpsURLConnection) url1.openConnection();
                connection.setReadTimeout(30000);
                connection.setConnectTimeout(30000);
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();

                InputStream is=connection.getInputStream();
                DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc=builder.parse(is);
                if (doc!=null)
                {
                    NodeList nl;
                    ArrayList args=new ArrayList();
                    for (String s : tag)
                    {
                        Log.i("string value","the variable value is"+s);
                        nl=doc.getElementsByTagName(s);
                        Log.i("nodelist","checked the list"+nl);
                        if(nl.getLength()>0)
                        {
                            Node node=nl.item(nl.getLength()-1);
                            Node node1=nl.item(nl.getLength()-2);
                            args.add(node.getTextContent());
                            args.add(node1.getTextContent());
                        }
                        else {
                            args.add(" nl element is 0 ");
                        }
                    }
                    dis=String.format("%s",args.get(0));
                    time=String.format("%s",args.get(1));
                }
                else
                {
                    Toast.makeText(MainActivity.this, "DOC is nul", Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return dis;
        }
        protected  void onPostExecute(String result)
        {
            if(result!=null)
            {
                distance.setText("Distance"+result);
                duration.setText("Time Duration"+time);
            }
        }


    }

    private void setupPlaceAutoComplete() {
        places_fragment=(AutocompleteSupportFragment)getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Toast.makeText(MainActivity.this, ""+place.getName(), Toast.LENGTH_SHORT).show();

               //name.setText(place.getLatLng());

                LatLng answer=place.getLatLng();
                double lat1=answer.latitude;
                double lng1=answer.longitude;
                weatherlat=lat1;
                weatherlng=lng1;
                find_weather();
                name.setText("LATLNG:  "+lat1+" "+lng1);
                new GetDistanceAsyncTask(12.9726,77.6312,lat1,lng1);
                //float[] ans=new float[10];
               // Location.distanceBetween(12.9726383,77.6312165,lat1,lng1,ans);
                //distance.setText("Distance ="+ans[0]/1000+"KM");

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initplaces() {
        Places.initialize(this,getString(R.string.API_KEY));
        placesClient=Places.createClient(this);


    }
    public void find_weather()
    {
        String url="http://api.openweathermap.org/data/2.5/weather?lat="+weatherlat+"&lon="+weatherlng+"&appid=0fc4c06e1014b1ffb30b1730639130d3&units=Imperial";

        JsonObjectRequest jor=new JsonObjectRequest(Request.Method.GET, url, null,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject mainob=response.getJSONObject("main");
                    JSONArray jsonArray=response.getJSONArray("weather");
                    JSONObject object=jsonArray.getJSONObject(0);
                    String temp=String.valueOf(mainob.getDouble("temp"));
                    String descrip=object.getString("description");
                    String city=response.getString("name");
                    City.setText(city);
                    Description.setText(descrip);
                    Calendar calendar=Calendar.getInstance();
                    SimpleDateFormat sdf=new SimpleDateFormat("EEEE-MM-dd");
                    String fromatteddate=sdf.format(calendar.getTime());
                    Date.setText(fromatteddate);
                    double tempint= Double.parseDouble(temp);
                    double centi=(tempint-32)/1.8000;
                    centi=Math.round(centi);
                    int i=(int)centi;
                    Temp.setText("Temperature(in celcius)"+"    "+String.valueOf(i));

                }catch (JSONException e)
                {
                    e.printStackTrace();
                }



            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        RequestQueue queue= Volley.newRequestQueue(this);
        queue.add(jor);

    }

}
