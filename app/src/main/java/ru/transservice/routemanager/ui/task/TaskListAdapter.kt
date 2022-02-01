package ru.transservice.routemanager.ui.task

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.ItemTaskListBinding

class TaskListAdapter(val listener: (PointItem) -> Unit) : ListAdapter<PointItem, TaskListAdapter.TaskListViewHolder> (TaskDiffCallback()) {

    var selectedItem: PointItem? = null

    class TaskListViewHolder(val binding: ItemTaskListBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PointItem, listener: (PointItem) -> Unit, selectedItem: PointItem?){
            with(binding){
                tvPointName.text = if (item.polygon) item.polygonName else "${item.rowNumber}. ${item.addressName}"
                tvContainer.text = item.containerName
                tvContainerCount.text = if (item.polygon) "Рейс №${item.tripNumberFact}" else item.countPlan.toString()

                if (item.done) {
                    ivPointStatus.setImageResource(R.drawable.ic_check_24_small)
                    ivPointStatus.visibility = View.VISIBLE
                    ivPointStatus.setColorFilter(Color.GREEN)
                }else if (item.reasonComment != "") {
                    ivPointStatus.setImageResource(R.drawable.ic_block_24_small)
                    ivPointStatus.visibility = View.VISIBLE
                    ivPointStatus.setColorFilter(Color.RED)
                }else{
                    ivPointStatus.visibility = View.GONE
                }

                if (item.tripNumber == 0) {
                    tvOnCall.visibility = View.VISIBLE
                }else{
                    tvOnCall.visibility = View.GONE
                }

                if (item.polygon) {
                    tvPointName.typeface = Typeface.DEFAULT_BOLD
                    //root.setBackgroundResource(R.drawable.bg_item_task_list_polygon)
                }else{
                    tvPointName.typeface = Typeface.DEFAULT
                    //root.setBackgroundResource(R.drawable.bg_item_task_list_white)
                }
            }
            binding.root.isSelected = item == selectedItem
            binding.root.setOnClickListener {
                it.isSelected = true
                listener(item)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<PointItem>() {
        override fun areItemsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
            return oldItem?.lineUID == newItem?.lineUID
        }

        override fun areContentsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val binding = ItemTaskListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        holder.bind(getItem(position),listener,selectedItem)
    }

    /*override fun getItemCount(): Int = items.size

    fun updateItems(data: List<PointItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return data.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].lineUID == data[newItemPosition].lineUID
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].hashCode() == data[newItemPosition].hashCode()
            }

        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = data
        diffResult.dispatchUpdatesTo(this)

    }*/


}