package com.example.android.trackmysleepquality

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.trackmysleepquality.SleepNightAdapter.ViewHolder.Companion.from
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.databinding.ListItemSleepNightBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The recyclerView need to distinguish each item's view type
// so that it can correctly assign a viewHolder to it.
private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

// Defining coroutine scope with Dispatchers.Default
private val adapterScope = CoroutineScope(Dispatchers.Default)

class SleepNightAdapter(private val clickListener: SleepNightListener) : ListAdapter<DataItem, RecyclerView.ViewHolder>(SleepNightDiffCallback()) {

    /**
     * This function is need to return the right header or item
     * constant depending on the type of the current item.
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DataItem.SleepNightItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    /**
     * this function is used for setting up all data for the ViewHolder
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        // Cast the object returned by getItem()
        when (holder) {
            is ViewHolder -> {
                val nightItem = getItem(position) as DataItem.SleepNightItem
                holder.bind(nightItem.sleepNight, clickListener)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextViewHolder.form(parent)
            ITEM_VIEW_TYPE_ITEM -> from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    // This function takes a list of SleepNight. Instead of using submitList()
    // provided by the ListAdapter, to submit your list, use this function to
    // add a header and then submit the list.
    fun addHeaderAndSubmitList(list: List<SleepNight>?) {
        // Start at the Default Dispatchers
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.SleepNightItem(it) }
            }
            // Switch to Main Thread
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    // This class inflates the header.xml layout, and returns a TextViewHolder instance.
    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            fun form(parent: ViewGroup): TextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.header, parent, false)
                return TextViewHolder(view)
            }
        }
    }

    class ViewHolder private constructor(private val binding: ListItemSleepNightBinding) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)

                val binding =
                        ListItemSleepNightBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }

        fun bind(item: SleepNight, clickListener: SleepNightListener) {
            // This to tell the binding object about the new SleepNight
            binding.sleep = item

            binding.executePendingBindings()

            // Create click listener
            binding.clickListener = clickListener
        }

    }

}

class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        // To check are the oldItem or the newItem has changed, if they
        // are the same item, so return true. Otherwise return false.
        // Uses this test to help discover if an item was added, removed,
        // or moved
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        // This equality will check all the fields, because SleepNight is a data class.
        // And if there are differences between old and new item, this code tells
        // DiffUtil that the item has been updated.
        return oldItem == newItem
    }
}

/**
 * Class to handle a click action, and the callback need the night.nightId
 * to access data from the database.
 */
class SleepNightListener(val clickListener: (sleepId: Long) -> Unit) {
    fun onClick(night: SleepNight) = clickListener(night.nightId)
}

sealed class DataItem {

    abstract val id: Long

    // To make it part of the sealed class, it must extend DataItem()

    data class SleepNightItem(val sleepNight: SleepNight): DataItem() {
        override val id = sleepNight.nightId
    }

    object Header: DataItem() {
        override val id = Long.MIN_VALUE
    }
}