package com.example.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private EditText cityEditText;
    private TextView resultTextView;
    private Button searchButton;

    private static final String API_KEY = "4e4a6d9a7c0a40deb9ffeb3eb155910b";

    private LocationManager locationManager;
    private String currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityEditText = findViewById(R.id.cityEditText);
        resultTextView = findViewById(R.id.resultTextView);
        searchButton = findViewById(R.id.searchButton);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEditText.getText().toString().trim();
                if (!city.isEmpty()) {
                    currentLocation = null;
                    fetchWeatherData(city);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a city", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    0, 0, this);
        }
    }

    private void fetchWeatherData(String city) {
        String url;
        if (currentLocation != null) {
            double latitude = Double.parseDouble(currentLocation.split(",")[0]);
            double longitude = Double.parseDouble(currentLocation.split(",")[1]);
            url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY;
        } else {
            url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;
        }
        WeatherTask weatherTask = new WeatherTask();
        weatherTask.execute(url);
    }

    private void updateWeatherInfo(String weatherInfo, double temperature) {
        String temperatureString = String.format("%.1f °C", temperature - 273.15); // Convert temperature to Celsius
        resultTextView.setText("Temperature: " + temperatureString + "\n" + weatherInfo);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location.getLatitude() + "," + location.getLongitude();
        fetchWeatherData("");
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private class WeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray weatherArray = jsonObject.getJSONArray("weather");
                    JSONObject weatherObject = weatherArray.getJSONObject(0);
                    String weatherInfo = weatherObject.getString("description");

                    JSONObject mainObject = jsonObject.getJSONObject("main");
                    double temperature = mainObject.getDouble("temp");

                    updateWeatherInfo(weatherInfo, temperature);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                resultTextView.setText("Failed to fetch weather data!");
            }
        }
    }
}
