import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ActivityCommsViewModel : ViewModel() {
    private val _retryNetworkAction = MutableLiveData<Event<Unit>>()
    val retryNetworkAction: LiveData<Event<Unit>> = _retryNetworkAction

    fun triggerRetryNetworkAction() {
        _retryNetworkAction.value = Event(Unit) // Event wrapper prevents re-triggering on config change
    }
}

// Helper class for events (to ensure they are consumed only once)
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
    