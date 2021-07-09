package ru.transservice.routemanager.ui.routesettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.databinding.ItemVehicleListBinding

class VehicleListAdapter(val listener: (VehicleItem) -> Unit) : RecyclerView.Adapter<VehicleListAdapter.VehicleItemViewHolder>() {

    var items: List<VehicleItem> = listOf()
    var selectedPos = RecyclerView.NO_POSITION

    class VehicleItemViewHolder(val binding: ItemVehicleListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VehicleItem, isSelected:Boolean) {
            binding.tvName.text = item.number
            binding.root.isSelected = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleItemViewHolder {
        val binding =
            ItemVehicleListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleItemViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPos)
        holder.binding.root.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.layoutPosition
            notifyItemChanged(selectedPos)
            listener(items[selectedPos])
        }
    }


    override fun getItemCount(): Int = items.size

    fun updateItems(data: List<VehicleItem>) {

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

    fun getItemPosition(vehicleItem: VehicleItem): Int{
        return items.indexOf(vehicleItem)
    }
}