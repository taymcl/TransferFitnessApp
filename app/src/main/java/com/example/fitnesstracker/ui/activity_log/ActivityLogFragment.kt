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
    private var inSession: Boolean = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var sessionName = ""
    lateinit var session: Session
    private var distance = 0.0


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
        // Remember add this line

        return binding.root
    }

    @SuppressLint("SimpleDateFormat")
    private fun sessionManager() {

        // Initialize session
        distance = 0.0

        if (inSession) {

            // Make toast message that activity session has started
            Toast.makeText(context, "Activity session started!", Toast.LENGTH_SHORT).show()

            // Set Session Name to current day and time
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            sessionName = "Activity Session ${sdf.format(Date())}"

            // Start time is the current time
            startTime = System.currentTimeMillis()

            // Create unique random ID for the session
            val sessionId = UUID.randomUUID().toString()

            // Build a new session
            session = Session.Builder()
                .setName(sessionName)
                .setIdentifier(sessionId)
                .setDescription("Walk")
                .setActivity(FitnessActivities.WALKING)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .build()

            // Subscribe to distance data (start recording distance data)
            Fitness.getRecordingClient(requireContext(), GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions))
                .subscribe(DataType.TYPE_DISTANCE_DELTA)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully Subscribed!")
                }
                .addOnFailureListener {e ->
                    Log.w(TAG, "There was a problem subscribing", e)
                }

            // Log the session identifier for testing purposes
            Log.w(TAG, "Session Identifier: ${session.identifier}")

            // Start the Session
            Fitness.getSessionsClient(requireContext(), GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions))
                .startSession(session)
                .addOnSuccessListener {
                    Log.i(TAG, "Session started successfully!")
                    Log.i(TAG, "Session Identifier: ${session.identifier}") // Logging the session identifier again
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error starting the session", e)
                }
        } else { // If user is ending the session

            // Make notification that the activity session has ended
            Toast.makeText(context, "Activity session ended!", Toast.LENGTH_SHORT).show()
            val dbHelper = ActivityDbHelper(thiscontext)

            // Invoke the SessionsClient to stop the session
            Fitness.getSessionsClient(requireContext(), GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions))
                .stopSession(session.identifier) // Stopping the session
                .addOnSuccessListener {
                    Log.i(TAG, "Session stopped successfully!")

                    // Using record client to stop recording distance data
                    Fitness.getRecordingClient(
                        requireContext(),
                        GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions)
                    )
                        .unsubscribe(DataType.TYPE_DISTANCE_DELTA) // Stop recording distance data
                        .addOnSuccessListener {
                            Log.i(TAG, "Successfully unsubscribed.")

                            // Creating a session read request
                            val readRequest = SessionReadRequest.Builder()
                                .setSessionId(session.identifier)
                                .setTimeInterval(
                                    session.getStartTime(TimeUnit.MILLISECONDS),
                                    System.currentTimeMillis(),
                                    TimeUnit.MILLISECONDS
                                )
                                .read(DataType.TYPE_DISTANCE_DELTA)
                                .build()

                            // Getting the session client to read the session data
                            Fitness.getSessionsClient(
                                requireContext(),
                                GoogleSignIn.getAccountForExtension(
                                    requireActivity(),
                                    fitnessOptions
                                )
                            )
                                .readSession(readRequest) // Reading session data
                                .addOnSuccessListener { response ->
                                    val sessions = response.sessions
                                    Log.i(TAG, "Number of returned sessions is: ${sessions.size}") // Logging the number of returned sessions (should be 1)

                                    // Start querying for distance data (where I believe issue is)
                                    for (session in sessions) {
                                        val dataSets = response.getDataSet(session)
                                        for (dataSet in dataSets) {
                                            val dataPoints = dataSet.dataPoints
                                            for (dataPoint in dataPoints) {
                                                distance += dataPoint.getValue(Field.FIELD_DISTANCE) // Adding distance data points
                                                    .asFloat().toDouble()
                                            }
                                        }
                                    }
                                    Log.i(TAG, "Distance: $distance") // Logging the distance user traveled
                                } .addOnFailureListener {e ->
                                    Log.w(TAG, "Failed to read Session", e)
                                }

                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to unsubscribe.")
                            // Retry the unsubscribe request.
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error stopping the session", e)
                }

            // Get the time
            endTime = System.currentTimeMillis()
            val tDelta: Long = endTime - startTime
            val elapsedSeconds: Double = (tDelta / 1000.0) / 60
            val formattedMinutes = String.format("%.2f", elapsedSeconds)

            // Get the date
            val calendar: Calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val currentDate: String = dateFormat.format(calendar.time)

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

