package ru.transservice.routemanager.ui.polygon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.databinding.ItemPolygonListBinding

class PolygonListAdapter(val listener: (PolygonItem) -> Unit): RecyclerView.Adapter<PolygonListAdapter.PolygonItemViewHolder>() {

    var items: List<PolygonItem> = listOf()
    private var selectedPos = RecyclerView.NO_POSITION

    class PolygonItemViewHolder(val binding: ItemPolygonListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PolygonItem, isSelected:Boolean) {
            binding.tvName.text = item.name
            binding.root.isSelected = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolygonItemViewHolder {
        val binding =
            ItemPolygonListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PolygonItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PolygonItemViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPos)
        holder.binding.root.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.layoutPosition
            notifyItemChanged(selectedPos)
            listener(items[selectedPos])
        }
    }


    override fun getItemCount(): Int = items.size

    fun updateItems(data: List<PolygonItem>) {

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return data.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].uid == data[newItemPosition].uid
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].hashCode() == data[newItemPosition].hashCode()
            }

        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = data
        diffResult.dispatchUpdatesTo(this)

    }

    fun getItemPosition(polygonItem: PolygonItem): Int{
        return items.indexOf(polygonItem)
    }
}