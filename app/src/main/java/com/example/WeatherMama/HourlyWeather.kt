package com.example.WeatherMama
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.ArrayList
import androidx.recyclerview.widget.RecyclerView

class CustomAdapter(private val context: Context, private val list: ArrayList<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var time: TextView
    lateinit var temp: TextView
    lateinit var icon: ImageView

    private inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            time = itemView.findViewById(R.id.time)
            temp = itemView.findViewById(R.id.temp)
            icon = itemView.findViewById(R.id.icon)
        }

        internal fun bind(position: Int) {
            // This method will be called anytime a list item is created or update its data
            //Do your stuff here
            time.text = list[position]
            temp.text = list[position]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.hourly, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    public fun updateTemperature (newTemp : String) {
        temp.text = newTemp
    }
    public fun updateHour (newHour : String) {
        time.text = newHour
    }
    public fun getIconImageView () : ImageView {
        return icon
    }

}

class HourlyWeather {
    var temperature :String = ""
    var realFeel :String = ""
    var addressDescription :String = ""
    var iconName : String = ""
    var hour : String = ""
}