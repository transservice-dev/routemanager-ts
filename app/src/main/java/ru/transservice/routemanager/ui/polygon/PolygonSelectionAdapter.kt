package ru.transservice.routemanager.ui.polygon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.databinding.ItemPolygonRowBinding

class PolygonSelectionAdapter(val listener: (PolygonItem,Int) -> Unit): RecyclerView.Adapter<PolygonSelectionAdapter.PolygonItemViewHolder>() {

    var items: List<PolygonItem> = listOf()
    private var selectedPos = RecyclerView.NO_POSITION

    class PolygonItemViewHolder(val binding: ItemPolygonRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PolygonItem, isSelected:Boolean) {
            binding.tvPolygon.text = item.name
            binding.tvTripNumber.text = item.tripNumber.toString()
            binding.cbDone.isChecked =  item.done
            binding.root.isSelected = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolygonItemViewHolder {
        val binding =
            ItemPolygonRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PolygonItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PolygonItemViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPos)
        holder.binding.tvPolygon.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.layoutPosition
            notifyItemChanged(selectedPos)
            listener(items[selectedPos],0)
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