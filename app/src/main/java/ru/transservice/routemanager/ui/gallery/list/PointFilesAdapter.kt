package ru.transservice.routemanager.ui.gallery.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.ItemPointPhotoBinding

class PointFilesAdapter(
    val pointItem: PointItem,
    val state: PhotoListViewModel.PhotoListState
) : ListAdapter<PointFile, PointFilesAdapter.PointPhotoViewHolder> (PointPhotoCallback()) {

    class PointPhotoViewHolder(val binding: ItemPointPhotoBinding, val context: Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pointFile: PointFile){
            with(binding){
                fileStatus.text = pointFile.photoOrder.title
                Glide.with(context).load(pointFile.filePath).into(ivPointPhoto)
            }
        }
    }

    class PointPhotoCallback : DiffUtil.ItemCallback<PointFile>() {
        override fun areItemsTheSame(oldItem: PointFile, newItem: PointFile): Boolean {
            return oldItem.lineUID == newItem.lineUID
        }

        override fun areContentsTheSame(oldItem: PointFile, newItem: PointFile): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointPhotoViewHolder {
        val binding = ItemPointPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PointPhotoViewHolder(binding,AppClass.appliactionContext())
    }

    override fun onBindViewHolder(holder: PointPhotoViewHolder, position: Int) {
        holder.bind(getItem(position))

        with(holder.binding){
            ivPointPhoto.setOnClickListener {
                handleSelection(false,ivPointPhotoSelected,position)
            }
            root.setOnClickListener {
                handleSelection(false,ivPointPhotoSelected,position)
            }
            ivPointPhoto.setOnLongClickListener {
                handleSelection(true,ivPointPhotoSelected,position)
                true
            }
            root.setOnLongClickListener {
                handleSelection(true,ivPointPhotoSelected,position)
                true
            }
        }
    }

    private fun handleSelection(longClick: Boolean, view: View, position: Int) {
        if (longClick)  { // set selection
            view.isSelected = true
            state.handleSelection(getItem(position),false)
        }else{
           if (state.selectedItems.isNotEmpty()){
               view.isSelected = !view.isSelected
               if (view.isSelected) {
                   state.handleSelection(getItem(position), false)
               }else{
                   state.handleSelection(getItem(position), true)
               }
           }else{
               state.navRequest(state.pointItem,position,currentList)
           }
        }

        view.visibility = if (view.isSelected) View.VISIBLE else View.GONE
    }

}