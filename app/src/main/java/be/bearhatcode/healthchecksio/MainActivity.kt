package be.bearhatcode.healthchecksio

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import be.bearhatcode.healthchecksio.model.HealthCheck
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.time.Instant


class MainActivity : AppCompatActivity() {

    private lateinit var projectListAdapter: ProjectListAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        val apiKeys = parseAPIKeys(sharedPreferences.getString("apiKeys", ""))

        projectListAdapter = ProjectListAdapter(this, apiKeys)
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = projectListAdapter


        val tabLayout : TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = apiKeys[position].substring(0,5)
        }.attach()
    }


}


class ChecksListViewModel : ViewModel() {
    val checksData: List<HealthCheck> = ArrayList()
}

class ChecksAdapter(private val onClick: (HealthCheck) -> Unit) :
    ListAdapter<HealthCheck, ChecksAdapter.ChecksHolder>(HealthCheckDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecksHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.check_row_item, parent, false)
        return ChecksHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ChecksHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class ChecksHolder(itemView: View, onClick: (HealthCheck) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val timeLine: TextView = itemView.findViewById(R.id.timing)
        private val sinceLine: TextView = itemView.findViewById(R.id.since)
        private var currentCheck: HealthCheck? = null

        init {
            itemView.setOnClickListener {
                currentCheck?.let {
                    onClick(it)
                }
            }
        }

        fun bind(check: HealthCheck) {
            currentCheck = check
            if (check.name.isNotEmpty()) {
                nameTextView.text = check.name
            } else {
                nameTextView.text = buildSpannedString {
                    italic {
                        append(itemView.context.getString(android.R.string.untitled))
                        append(" ")
                        append(check.id.toString())
                    }
                }
            }
            icon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    itemView.resources,
                    check.state.icon,
                    null
                )
            )
            icon.setColorFilter(
                ResourcesCompat.getColor(
                    itemView.resources,
                    check.state.color,
                    null
                ), PorterDuff.Mode.SRC_IN
            )
            icon.contentDescription = check.state.stringRep // TODO: translate

            timeLine.text = buildSpannedString {
                append("Expected every ")
                bold { append(fmtDuration(check.timeout)) }
                append(" (")
                append(fmtDuration(check.grace))
                append(" ")
                append("grace")
                append(")")
            }
            if (check.lastPing != null) {
                sinceLine.text = buildSpannedString {
                    append("Last ping: ")
                    bold {
                        append(
                            DateUtils.getRelativeTimeSpanString(
                                check.lastPing.toEpochMilli(),
                                Instant.now().toEpochMilli(), 0L, 0
                            )
                        )
                    }

                }

            } else {
                sinceLine.text = itemView.context.getString(R.string.no_past_pings)
            }

        }
    }

}


object HealthCheckDiffCallback : DiffUtil.ItemCallback<HealthCheck>() {
    override fun areItemsTheSame(oldItem: HealthCheck, newItem: HealthCheck): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: HealthCheck, newItem: HealthCheck): Boolean {
        return oldItem.id == newItem.id
    }
}


