package be.bearhatcode.healthchecksio

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class FetchWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val key = inputData.getString(ARG_API_KEY)
        Log.i("WORKER", "Got key $key")
        if (key != null) {
            val future = HealthChecksRequest(context, "/checks/", key)

            return try {
                val data = future.get(5, TimeUnit.SECONDS)
                Result.success(workDataOf("result" to data.toString()))
            } catch (e: InterruptedException) {
                e.printStackTrace()
                // exception handling
                Result.retry()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                Result.retry()
                // exception handling
            } catch (e: TimeoutException) {
                e.printStackTrace()
                Result.retry()
            }
        } else {
            return Result.failure()
        }
    }

}