package ru.transservice.routemanager.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.ItemTaskListBinding

class TaskListAdapter(val listener: (PointItem) -> Unit) : RecyclerView.Adapter<TaskListAdapter.TaskListViewHolder>() {

    var items: List<PointItem> = listOf()
    var selectedPos = RecyclerView.NO_POSITION
    var selectedItem: PointItem? = null

    class TaskListViewHolder(val binding: ItemTaskListBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PointItem, isSelected: Boolean){
            with(binding){
                tvPointName.text = if (item.polygon) item.addressName else "${item.rowNumber}. ${item.addressName}"
                tvContainer.text = item.containerName
                tvContainerCount.text = item.countPlan.toString()

                if (item.done) {
                    ivPointStatus.setImageResource(R.drawable.ic_check_24_small)
                    ivPointStatus.visibility = View.VISIBLE
                }else if (item.reasonComment != "") {
                    ivPointStatus.setImageResource(R.drawable.ic_block_24_small)
                    ivPointStatus.visibility = View.VISIBLE
                }else{
                    ivPointStatus.visibility = View.GONE
                }

                if (item.tripNumber == 0) {
                    tvOnCall.visibility = View.VISIBLE
                }else{
                    tvOnCall.visibility = View.GONE
                }
            }
            binding.root.isSelected = isSelected

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val binding = ItemTaskListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        val isSelected = when {
            position == selectedPos && selectedItem == null -> true
            position == selectedPos && items[position].hashCode() == selectedItem.hashCode() -> true
            else -> false
        }
        holder.bind(items[position],isSelected)
        holder.binding.root.setOnClickListener {
            notifyItemChanged(selectedPos)
            notifyItemChanged(items.indexOf(selectedItem))
            selectedPos = holder.layoutPosition
            selectedItem = items[selectedPos]
            notifyItemChanged(selectedPos)
            notifyItemChanged(items.indexOf(selectedItem))
            listener(items[selectedPos])
        }
    }

    override fun getItemCount(): Int = items.size

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

    }
}