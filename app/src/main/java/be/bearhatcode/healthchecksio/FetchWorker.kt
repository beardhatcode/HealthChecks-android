package be.bearhatcode.healthchecksio

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class FetchWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val apiKeys = parseAPIKeys(sharedPreferences.getString("apiKeys", ""))
        if (apiKeys.isNotEmpty()) {
            val key = apiKeys[0]
            val future = HealthChecksRequest(context, "/checks/", key)

            return try {
                val data = future.get(5, TimeUnit.SECONDS)
                Result.success(workDataOf("result" to data.toString()))
            } catch (e: InterruptedException) {
                e.printStackTrace()
                // exception handling
                Result.failure()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                Result.failure()
                // exception handling
            } catch (e: TimeoutException) {
                e.printStackTrace()
                Result.failure()
            }
        } else {
            return Result.success(workDataOf("result" to "{}"))
        }
    }

}