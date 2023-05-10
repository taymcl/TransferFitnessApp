package com.example.fitnesstracker.ui.activity_log

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Session
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
enum class FitActionRequestCode {
    SUBSCRIBE,
    CANCEL_SUBSCRIPTION,
    DUMP_SUBSCRIPTIONS
}


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
    private val button = view?.findViewById<Button>(R.id.session_button)


    private val fitnessOption = FitnessOptions.builder()
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thiscontext = container!!.context
        val dashboardViewModel =
            ViewModelProvider(this)[ActivityLogViewModel::class.java]

        _binding = FragmentActivityLogBinding.inflate(inflater, container, false)

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
        // Check if user is authenticated for Google Play Services
        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOption)
        if (!GoogleSignIn.hasPermissions(account, fitnessOption)) {
            GoogleSignIn.requestPermissions(
                this,
                1,
                account,
                fitnessOption
            )
            return
        }

        inSession = true
        Toast.makeText(context, "Activity session started!", Toast.LENGTH_SHORT).show()

        // Create new session and start recording data

        // Start recording the time
        startTime = System.currentTimeMillis()
    }


    private fun endSession() {
        inSession = false
        Toast.makeText(context, "Activity session ended!", Toast.LENGTH_SHORT).show()
        val dbHelper = ActivityDbHelper(thiscontext)

        // Calculate the time
        endTime = System.currentTimeMillis()
        val tDelta: Long = endTime - startTime
        var elapsedSeconds: Double = (tDelta / 1000.0) / 60
        val formattedMinutes = String.format("%.2f", elapsedSeconds)

        // Get the date
        val calendar: Calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val currentDate: String = dateFormat.format(calendar.getTime())

        // Add session info to the database
        dbHelper.insertData("Dist", currentDate, formattedMinutes)
        //createFromDb()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById<View>(com.example.fitnesstracker.R.id.recycler_view) as RecyclerView

        // Build the recyclerView from the database
        recyclerView.adapter = MyRecyclerAdapter(createFromDb())

        recyclerView.layoutManager = LinearLayoutManager(activity)
        // Divider
        val dividerItemDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // Check if location and activity recognition permissions are granted

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
           return
        }

    }

    private fun createFromDb() : ArrayList<LoggedActivity>{
        val dbHelper = ActivityDbHelper(thiscontext)
        val activities = ArrayList<LoggedActivity>()
        val cursor = dbHelper.viewAllData

        while (cursor.moveToNext()){
            val activity = LoggedActivity("Date: ${cursor.getString(2)}", " Time Elapsed: ${cursor.getString(3)}", "Distance: ${cursor.getString(1)}")
            activities.add(activity)
        }
        return activities
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}