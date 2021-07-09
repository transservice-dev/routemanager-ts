package ru.transservice.routemanager.ui.routesettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.databinding.ItemRouteListBinding

class RouteListAdapter(val listener: (RouteItem) -> Unit) : RecyclerView.Adapter<RouteListAdapter.RouteItemViewHolder>() {

    var items: List<RouteItem> = listOf()
    var selectedPos = RecyclerView.NO_POSITION

    class RouteItemViewHolder(val binding: ItemRouteListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RouteItem, isSelected:Boolean) {
            binding.tvName.text = item.name
            binding.root.isSelected = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteItemViewHolder {
        val binding =
            ItemRouteListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteItemViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPos)
        holder.binding.root.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.layoutPosition
            notifyItemChanged(selectedPos)
            listener(items[selectedPos])
        }
    }


    override fun getItemCount(): Int = items.size

    fun updateItems(data: List<RouteItem>) {

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

    fun getItemPosition(routeItem: RouteItem): Int{
        return items.indexOf(routeItem)
    }
}