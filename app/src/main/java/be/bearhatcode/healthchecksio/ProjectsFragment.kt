/**
 * Fragment for "project" each project is uniqly defined by an API key
 *
 * The ProjectListAdapter helps to create a fragment for each key
 */
package be.bearhatcode.healthchecksio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import be.bearhatcode.healthchecksio.model.HealthCheck
import be.bearhatcode.healthchecksio.model.HealthCheckState
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ProjectListAdapter(fragment: FragmentActivity, private val apiKeys: List<String>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = apiKeys.size

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = ProjectFragment()
        fragment.arguments = Bundle().apply {
            putString(ARG_API_KEY, apiKeys[position]) // TODO: put API key here
        }
        return fragment
    }
}

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
        arguments?.takeIf { it.containsKey(ARG_API_KEY) }?.apply {
            val apiKey = getString(ARG_API_KEY)
            val workManager: WorkManager = WorkManager.getInstance(view.context)
            val fetchDataRequest =
                OneTimeWorkRequestBuilder<FetchWorker>()
                    .setInputData(workDataOf(ARG_API_KEY to apiKey))
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
                                Toast.makeText(context, "Work was not passed", Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                Toast.makeText(context, "Work was not parsing", Toast.LENGTH_SHORT)
                                    .show()
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

    }

    private fun adapterOnClick(check: HealthCheck) {
        Toast.makeText(context, "You clicked on " + check.name, Toast.LENGTH_SHORT)
            .show()
    }
}