package be.bearhatcode.healthchecksio.model

import be.bearhatcode.healthchecksio.R
import org.json.JSONException
import java.time.Instant

data class HealthCheck(
    val id: Long,
    val name: String,
    val description: String,
    val timeout: Long, // In seconds
    val grace: Long, // In seconds
    val lastPing: Instant?,
    val state: HealthCheckState
) : Comparable<HealthCheck> {
    /**
     * Place error checks first
     */
    override fun compareTo(other: HealthCheck): Int {
        val stateComp = this.state.compareTo(other.state)
        return if (stateComp != 0) {
            stateComp
        } else {
            (this.id - other.id).toInt()
        }
    }


}

enum class HealthCheckState(
    val stringRep: String,
    val isGood: Boolean,
    val icon: Int,
    val color: Int
) {
    DOWN("down", false, R.drawable.down, R.color.color_down),
    LATE("grace", false, R.drawable.late, R.color.color_late),
    STARTED("started", true, R.drawable.up, R.color.color_late),
    UP("up", true, R.drawable.up, R.color.color_up),
    NEW("new", true, R.drawable.up, R.color.color_paused),
    PAUSED("paused", true, R.drawable.paused, R.color.color_paused);

    companion object {
        fun fromJSONString(kind: String): HealthCheckState {
            for (e in values()) {
                if (e.stringRep == kind)
                    return e
            }
            throw JSONException("Could not parse $kind as a HealthCheckState")
        }
    }
}