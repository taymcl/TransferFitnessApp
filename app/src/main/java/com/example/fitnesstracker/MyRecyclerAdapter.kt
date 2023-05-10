package com.example.fitnesstracker
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class MyRecyclerAdapter (private val activities: ArrayList<LoggedActivity>): RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.date_text)
        val timeElapsed: TextView = itemView.findViewById(R.id.total_time_text)
        val distanceTraveled: TextView = itemView.findViewById(R.id.distance_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // Create the view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = activities[position]
        holder.distanceTraveled.text = currentItem.distance
        holder.timeElapsed.text = currentItem.totalTime
        holder.dateText.text = currentItem.date

    }

    // Get size
    override fun getItemCount(): Int {
        return activities.size
    }

}