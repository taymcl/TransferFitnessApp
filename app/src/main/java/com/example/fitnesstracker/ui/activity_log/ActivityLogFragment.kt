package com.example.fitnesstracker.ui.activity_log

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
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
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ActivityLogFragment : Fragment() {
    private lateinit var thiscontext: Context

    private var _binding: FragmentActivityLogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var session: Session? = null
    private var inSession: Boolean = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var sessionName = ""


    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .build()


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thiscontext = container!!.context
        val dashboardViewModel =
            ViewModelProvider(this)[ActivityLogViewModel::class.java]

        _binding = FragmentActivityLogBinding.inflate(inflater, container, false)

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                requireActivity(), // your activity
                1, // e.g. 1
                GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions),
                fitnessOptions)
        }

        val sessionButton: Button = binding.sessionButton
        sessionButton.setOnClickListener {
            if (session == null && !inSession) {
                sessionButton.setText("End Activity Session")
                startSession()
            } else {
                sessionButton.setText("Start Activity Session")
                endSession()
            }
        }
        // Remember add this line

        return binding.root
    }

    private fun startSession() {
        inSession = true
        Toast.makeText(context, "Activity session started!", Toast.LENGTH_SHORT).show()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        sessionName = "Activity Session ${sdf.format(Date())}"

        // Start recording the time
        startTime = System.currentTimeMillis()

        Fitness.getRecordingClient(requireActivity(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_DISTANCE_DELTA)

    }


    @SuppressLint("SimpleDateFormat")
    private fun endSession() {

        inSession = false
        Toast.makeText(context, "Activity session ended!", Toast.LENGTH_SHORT).show()
        val dbHelper = ActivityDbHelper(thiscontext)

        // Calculate the time

        var distance: Double = 0.0
        endTime = System.currentTimeMillis()


        val distanceDataSource = DataSource.Builder()
            .setAppPackageName(requireContext().packageName)
            .setDataType(DataType.TYPE_DISTANCE_DELTA)
            .setStreamName("$sessionName-distance")
            .setType(DataSource.TYPE_RAW)
            .build()

        // Build a dataset that includes the session data
        val distanceDp = DataPoint.builder(distanceDataSource)
            .setField(Field.FIELD_DISTANCE, distance.toFloat())
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val dataset = DataSet.builder(distanceDataSource)
            .add(distanceDp)
            .build()

        val newSession = Session.Builder()
            .setName(sessionName)
            .setIdentifier("UniqueIdentifierHere")
            .setDescription("Walk")
            .setActivity(FitnessActivities.WALKING)
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .setEndTime(endTime, TimeUnit.MILLISECONDS)
            .build()


        val insertrequest = SessionInsertRequest.Builder()
            .setSession(newSession)
            .addDataSet(dataset)
            .build()


        // Invoke the SessionsClient with the session identifier
        Fitness.getSessionsClient(requireActivity(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
            .insertSession(insertrequest)
            .addOnSuccessListener {
                Log.i(TAG, "Session inserted")
                Fitness.getRecordingClient(
                    requireActivity(),
                    GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)
                )
                    .unsubscribe(DataType.TYPE_DISTANCE_DELTA)
            }

        val readRequest = SessionReadRequest.Builder()
            .read(DataType.TYPE_DISTANCE_DELTA)
            .setSessionId(newSession.identifier)
            .setTimeInterval(newSession.getStartTime(TimeUnit.MILLISECONDS), newSession.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
            .build()


        Fitness.getSessionsClient(requireActivity(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
            .readSession(readRequest)
            .addOnSuccessListener { response ->
                val sessions = response.sessions
                Log.i(TAG, "Number of returned sessions is: ${sessions.size}")
                for (session in sessions) {
                    val dataSets = response.getDataSet(session)
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            distance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat().toDouble()
                            Log.i(TAG, "Distance: $distance")
                        }
                    }
                }
                val tDelta: Long = endTime - startTime
                val elapsedSeconds: Double = (tDelta / 1000.0) / 60
                val formattedMinutes = String.format("%.2f", elapsedSeconds)

                // Get the date
                val calendar: Calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                val currentDate: String = dateFormat.format(calendar.getTime())

                // Add session info to the database
                dbHelper.insertData(distance.toString(), currentDate, formattedMinutes)
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

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
                " Time Elapsed: ${cursor.getString(3)}",
                "Distance: ${cursor.getString(1)}"
            )
            activities.add(activity)
        }
        return activities
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

