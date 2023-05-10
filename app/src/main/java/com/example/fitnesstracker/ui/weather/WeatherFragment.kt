package com.example.fitnesstracker.ui.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentActivityLogBinding
import com.example.fitnesstracker.databinding.FragmentWeatherBinding
import com.example.fitnesstracker.ui.activity_log.ActivityLogViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class WeatherFragment : Fragment(), LocationListener {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private val WEATHER_API_KEY = "70a0963ae6d2b57484dad2e8e95d8842"
    private lateinit var locationManager: LocationManager
    private lateinit var weatherService: WeatherService
    private var latitude = 0.0
    private var longitude = 0.0
    private var weather_text : TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        weather_text = view?.findViewById(R.id.weather_view)

        // Initialize location manager and check for permissions
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // Initialize Retrofit and WeatherService
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherService = retrofit.create(WeatherService::class.java)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onLocationChanged(location: Location) {
        // Update latitude and longitude and make API call
        latitude = location.latitude
        longitude = location.longitude

        // Checks to see if lat long is being updated properly
        //Log.d("Weather", "Latitude: $latitude, Longitude: $longitude")
        getWeatherData()
        locationManager.removeUpdates(this)
    }


    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    @SuppressLint("SetTextI18n")
    private fun getWeatherData() {
        weather_text = view?.findViewById(R.id.weather_view)

        // Make API call to get current weather data

        val call = weatherService.getCurrentWeather(latitude, longitude, WEATHER_API_KEY, "metric")

        // Prints the url being sent
        //val url = call.request().url.toString()
        //Log.d("Weather", "URL: $url")
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    val weatherText =
                        "${weatherResponse?.main?.temp} °C, ${weatherResponse?.weather?.get(0)?.description}"
                    weather_text?.text = weatherText
                }
                else {
                    weather_text?.text = "Something is wrong"
                    Log.e("Weather", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // Handle API call failure here
                weather_text?.text = "Could not get weather data! Fix Me!"
            }
        })
    }
}
