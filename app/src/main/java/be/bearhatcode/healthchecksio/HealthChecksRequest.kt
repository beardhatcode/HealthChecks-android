package be.bearhatcode.healthchecksio

import android.content.Context
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class HealthChecksRequest(context: Context, path: String, key: String) :
    Future<JSONObject> {
    private var future: RequestFuture<JSONObject> = RequestFuture.newFuture()
    private val urlBase = "https://healthchecks.io/api/v1"
    private val jsonObjectRequest: JsonObjectRequest

    init {
        val queue = Volley.newRequestQueue(context)
        jsonObjectRequest = HealthChecksRequest(urlBase + path, key, future, future)
        queue.add(jsonObjectRequest)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        jsonObjectRequest.cancel()
        return true
    }

    override fun isCancelled(): Boolean {
        return jsonObjectRequest.isCanceled
    }

    override fun isDone(): Boolean {
        return future.isDone
    }

    override fun get(): JSONObject {
        return future.get()
    }

    override fun get(timeout: Long, unit: TimeUnit?): JSONObject {
        return future.get(timeout, unit)
    }

    class HealthChecksRequest(
        url: String,
        private val key: String,
        a: Response.Listener<JSONObject>,
        b: Response.ErrorListener,
    ) : JsonObjectRequest(Method.GET, url, null, a, b) {

        override fun getHeaders(): MutableMap<String, String> {
            return mutableMapOf("X-Api-Key" to key)
        }

    }

}