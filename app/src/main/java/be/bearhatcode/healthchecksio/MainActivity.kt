package be.bearhatcode.healthchecksio

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import be.bearhatcode.healthchecksio.model.HealthCheck
import be.bearhatcode.healthchecksio.model.HealthCheckState
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class MainActivity : AppCompatActivity() {

    private lateinit var demoCollectionAdapter: DemoCollectionAdapter
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

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.pager, ProjectFragment())
                .commit()
        }
     //   supportActionBar?.setDisplayHomeAsUpEnabled(true)




      //  demoCollectionAdapter = DemoCollectionAdapter(this)
     //   viewPager = this.findViewById(R.id.pager)
     //   viewPager.adapter = demoCollectionAdapter

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


class CollectionDemoFragment : Fragment() {
    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    private lateinit var demoCollectionAdapter: DemoCollectionAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        demoCollectionAdapter = DemoCollectionAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = demoCollectionAdapter
    }
}

class DemoCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = ProjectFragment()
        fragment.arguments = Bundle().apply {
            // Our object is just an integer :-P
            putInt(ARG_OBJECT, position + 1)
        }
        return fragment
    }
}

private const val ARG_OBJECT = "object"

// Instances of this class are fragments representing a single
// object in our collection.
class ProjectFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_project, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        val workManager: WorkManager = WorkManager.getInstance(view.context)
        val fetchDataRequest =
            OneTimeWorkRequestBuilder<FetchWorker>()
                .build()
        workManager.enqueue(fetchDataRequest)

        val test: MutableLiveData<List<HealthCheck>> = MutableLiveData(ArrayList())


        workManager
            .getWorkInfoByIdLiveData(fetchDataRequest.id)
            .observe(viewLifecycleOwner,
                { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    try {
                        val res = workInfo.outputData.getString("result")
                            ?: throw JSONException("cannot parse null")
                        val resJSON = JSONObject(res)
                        val checks = resJSON.getJSONArray("checks")
                        val arrayList: MutableList<HealthCheck> = mutableListOf()
                        var k: Long = 0
                        for (i in 0 until checks.length()) {
                            val cur = checks.getJSONObject(i)
                            arrayList.add(
                                HealthCheck(
                                    k++,
                                    cur.getString("name"),
                                    cur.getString("desc"),
                                    cur.getLong("timeout"),
                                    cur.getLong("grace"),
                                    try {
                                        Instant.from(
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
                                                cur.getString("last_ping")
                                            )
                                        )
                                    } catch (e: DateTimeParseException) {
                                        null
                                    },
                                    HealthCheckState.fromJSONString(cur.getString("status"))
                                )
                            )
                        }
                        arrayList.sort()
                        test.value = arrayList
                        Toast.makeText(context, "Work was not passed", Toast.LENGTH_SHORT).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(context, "Work was not parsing", Toast.LENGTH_SHORT).show()
                    }
                }

            })


        val list: RecyclerView = view.findViewById(R.id.checksView)
        val checkAdapter = ChecksAdapter { healthCheck -> adapterOnClick(healthCheck) }
        list.adapter = checkAdapter

        test.observe(viewLifecycleOwner, {
            it?.let {
                checkAdapter.submitList(it as MutableList<HealthCheck>)
                // headerAdapter.updateFlowerCount(it.size)
            }
        })


    }

    private fun adapterOnClick(check: HealthCheck) {
        Toast.makeText(context, "You clicked on " + check.name, Toast.LENGTH_SHORT)
            .show()
    }
}
