@file:Suppress("DEPRECATION")

package com.example.android.trackmysleepquality.sleeptracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.SleepNightAdapter
import com.example.android.trackmysleepquality.SleepNightListener
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */

class SleepTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_sleep_quality.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sleep_tracker, container, false)

        // Get a reference to the application context. This reference is needed
        // to the app that this fragment is attached to, to pass into the view-model
        // factory provider
        val application = requireNotNull(this.activity).application

        // To get a reference to the DAO of the database
        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao

        // Creating instance of the viewModelFactory
        val viewModelFactory = SleepTrackerViewModelFactory(dataSource, application)

        // Get a reference to the ViewModel associated with this fragment.
        val sleepTrackerViewModel =
                ViewModelProviders.of(
                        this, viewModelFactory
                ).get(SleepTrackerViewModel::class.java)

        // Observing the navigateToSleepQuality function at the ViewModel
        sleepTrackerViewModel.navigateToSleepQuality.observe(viewLifecycleOwner, Observer { night ->
            night?.let {
                this.findNavController().navigate(
                        SleepTrackerFragmentDirections
                                .actionSleepTrackerFragmentToSleepQualityFragment(night.nightId))
                sleepTrackerViewModel.doneNavigating()
            }
        })

        // Observer the snackbar
        sleepTrackerViewModel.showSnackbarEvent.observe(viewLifecycleOwner, Observer {
            if (it == true) { // Observe if the state is true
                Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        getString(R.string.cleared_message),
                        Snackbar.LENGTH_SHORT
                ).show()
                // set the value to false again
                sleepTrackerViewModel.doneShowingSnackbar()
            }
        })

        // Also create click handler for the button
        val adapter = SleepNightAdapter((SleepNightListener { nightId ->
            Toast.makeText(context, "$nightId", Toast.LENGTH_LONG ).show()
            sleepTrackerViewModel.onSleepNightClicked(nightId)
        }))

        binding.sleepList.adapter = adapter

        // Using lifecycle owner to make sure this observer is only active when
        // the recycler view is on the screen.
        sleepTrackerViewModel.nights.observe(viewLifecycleOwner, Observer {
            // Checking are the night data are null
            it?.let {
                // using submitList() to tell the adapter, that a new version of the
                // list is available. And when this method is called, the ListAdapter
                // diffs the new list against the old one and detects items that were
                // added, removed, changed, or deleted.
                // Then the ListAdapter updates the items shown by RecyclerView.

                // changed with custom function called this
                adapter.addHeaderAndSubmitList(it)
            }
        })

        sleepTrackerViewModel.navigateToSleepDetail.observe(viewLifecycleOwner, Observer { night ->
            night?.let {
                this.findNavController().navigate(
                        SleepTrackerFragmentDirections
                                .actionSleepTrackerFragmentToSleepDetailFragment(night))
                sleepTrackerViewModel.onSleepDetailNavigated()
            }
        })

        // GridLayoutManager have 4 constructor
        // (activity, number span, orientation (default: vertical),
        // reverse layout (default: false))
        val manager = GridLayoutManager(activity, 3)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

            // To return the right size for each position, position 0
            // has a span 3, and the other position have a span size 1
            override fun getSpanSize(position: Int) = when (position) {
                0 -> 3
                else ->1
            }

        }

        // Tell the RecyclerView to use this Layout Manager
        binding.sleepList.layoutManager = manager

        binding.lifecycleOwner = this
        binding.sleepTrackerViewModel = sleepTrackerViewModel

        return binding.root
    }
}