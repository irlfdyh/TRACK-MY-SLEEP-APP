package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

class SleepTrackerViewModel (
        val database: SleepDatabaseDao,
        application: Application): AndroidViewModel(application) {

    // Define a job
    private var viewModelJob = Job()

    // Defining uiScope for the coroutines, to set up what the coroutine will
    // run on, at this property the coroutine will run at the Main Thread
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Get all the nights from the database
    private val nights = database.getAllNights()

    // transforming the nights into a nightsString,
    // and getting the application resources
    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }!!

    // This variable is used to hold the current night, and make the variable
    // MutableLiveData, because the ViewModel need to be able to observe the data
    // and change it.
    private var tonight = MutableLiveData<SleepNight?>()

    // This variable to save the data at LiveData, and only can accessed for this class
    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    // The public properties to get and set value from recent _navigateToSleepQuality
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    // if the tonight data is null the start button will be able
    // for user click action
    val startButtonVisible: LiveData<Boolean> = Transformations.map(tonight) {
        it == null
    }

    // if the tonight data is not null the stop button will be visible
    // for user click action
    val stopButtonVisible: LiveData<Boolean> = Transformations.map(tonight) {
        it != null
    }

    // if the night data is not empty the clear button will
    // be able to get user click action
    val clearButtonVisible: LiveData<Boolean> = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    // Create encapsuled event
    private var _showSnackbarEvent = MutableLiveData<Boolean>()
    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            // set value for tonight variable from the database
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            // Creating variable to get the newest night from database
            var night = database.getTonight()

            // checking condition when the end time and start time is not same
            // that mean the night has already completed, then returning null.
            // Otherwise, return the night.
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    /**
     * Click handler function
     */

    // this function is used to create a new SleepNight, and insert it
    // into database, and assign it to tonight
    fun onStartTracking() {
        // launch a coroutine in the uiScope, because this app is need this result
        // to continue and update the UI.
        uiScope.launch {
            // creating new SleepNight with capturing  the current time
            // as the start time
            val newNight = SleepNight()

            // Using insert function to insert new SleepNight into the database
            insert(newNight)

            // Updating the new tonight value
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        // Launching a coroutine in the I/O context and insert
        // the night into the database by calling insert() from
        // the DAO
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    fun onStopTracking() {
        uiScope.launch {
            // getting the oldNight from the current night where
            // the night is started
            val oldNight = tonight.value ?: return@launch

            // set new value for the new night
            oldNight.endTimeMilli = System.currentTimeMillis()

            // set the value to update
            update(oldNight)

            // to pass the data when navigate to the SleepQuality
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun update(night: SleepNight) {
        // Launching a coroutine in the I/O context and update
        // the night data from the database by calling update()
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    // clear the database record
    fun onClear() {
        uiScope.launch {
            clear()
            // if the tonight have started record, the data will
            // be deleted
            tonight.value = null
        }
        // Trigger the _showSnackbarEvent value to true when this
        // method is called.
        _showSnackbarEvent.value = true
    }

    // function to reset the variable value that triggers navigation
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    // set the value of _showSnackbarEvent property
    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // when this ViewModel is cleared, all of the Job will be cancelled
        viewModelJob.cancel()
    }
}