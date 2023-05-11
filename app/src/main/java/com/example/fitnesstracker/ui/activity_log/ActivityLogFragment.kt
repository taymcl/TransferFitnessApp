package com.example.fitnesstracker.ui.activity_log

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.ActivityDbHelper
import com.example.fitnesstracker.LoggedActivity
import com.example.fitnesstracker.MyRecyclerAdapter
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentActivityLogBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


class ActivityLogFragment : Fragment() {
    private lateinit var thiscontext: Context

    private var _binding: FragmentActivityLogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var inSession: Boolean = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var distance = 0.0
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thiscontext = container!!.context
        val dashboardViewModel =
            ViewModelProvider(this)[ActivityLogViewModel::class.java]

        _binding = FragmentActivityLogBinding.inflate(inflater, container, false)


        val sessionButton: Button = binding.sessionButton
        sessionButton.setOnClickListener {
            if (!inSession) {
                inSession = true
                sessionButton.text = "End Activity Session"
                sessionManager()
            }
            else {
                inSession = false
                sessionButton.text = "Start Activity Session"
                sessionManager()
            }
        }

        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshRecyclerView()
        }
        // Remember add this line

        return binding.root
    }

    private fun startLocationUpdates() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Register the location listener
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000,
            5f,
            locationListener
        )
    }
    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }


    private val locationListener = LocationListener { location ->
        onLocationUpdated(location)
    }

    private val dataPoints = ArrayList<Double>()

    private fun onLocationUpdated(location: Location) {
        if (lastLocation == null) {
            lastLocation = location
        } else {
            val distanceInMeters = lastLocation?.distanceTo(location)
            lastLocation = location
            if (distanceInMeters != null) {
                dataPoints.add(distanceInMeters.toDouble())
                Log.w(TAG, "Distance Added: $distanceInMeters")
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun sessionManager() {
        if (inSession) {
            // Initialize session
            distance = 0.0
            lastLocation = null
            dataPoints.clear()
            startLocationUpdates()

            // Make toast message that activity session has started
            Toast.makeText(context, "Activity session started!", Toast.LENGTH_SHORT).show()

            startTime = System.currentTimeMillis()


        } else { // If user is ending the session

            stopLocationUpdates()
            lastLocation = null

            // Make notification that the activity session has ended
            Toast.makeText(context, "Activity session ended!", Toast.LENGTH_SHORT).show()
            val dbHelper = ActivityDbHelper(thiscontext)
            // Get the time
            endTime = System.currentTimeMillis()
            // Set Session Name to current day and time


            for (dataPoint in dataPoints) {
                distance += dataPoint
                Log.w(TAG, "Total Distance So Far: $distance")
            }

            val tDelta: Long = endTime - startTime
            val elapsedSeconds: Double = (tDelta / 60000.0)
            val formattedMinutes = String.format("%.2f", elapsedSeconds)

            // Get the date
            val calendar: Calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val currentDate: String = dateFormat.format(calendar.time)

            // Add session info to the database
            dbHelper.insertData(distance.toInt().toString(), currentDate, formattedMinutes)
            //createFromDb()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView =
            view.findViewById<View>(R.id.recycler_view) as RecyclerView

        // Build the recyclerView from the database
        recyclerView.adapter = MyRecyclerAdapter(createFromDb())

        recyclerView.layoutManager = LinearLayoutManager(activity)
        // Divider
        val dividerItemDecoration =
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Check if location and activity recognition permissions are granted

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
        } else {
            return
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            return
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            return
        }

    }

    private fun createFromDb(): ArrayList<LoggedActivity> {
        val dbHelper = ActivityDbHelper(thiscontext)
        val activities = ArrayList<LoggedActivity>()
        val cursor = dbHelper.viewAllData

        while (cursor.moveToNext()) {
            val activity = LoggedActivity(
                "Date: ${cursor.getString(2)}",
                "Minutes Elapsed: ${cursor.getString(3)}",
                "Distance: ${cursor.getString(1)} Meters"
            )
            activities.add(activity)
        }
        return activities
    }

    // Used for swipe to refresh
    private fun refreshRecyclerView() {
        val recyclerView: RecyclerView = binding.recyclerView

        // Build the recyclerView from the database
        recyclerView.adapter = MyRecyclerAdapter(createFromDb())

        swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
