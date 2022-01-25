package ru.transservice.routemanager.ui.gallery.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPointPhotosBinding
import ru.transservice.routemanager.repositories.RootRepository

class PointListAdapter(
    val photoOrder: PhotoOrder,
    val displayWidth: Int,
    val state: PhotoListViewModel.PhotoListState
    ) : ListAdapter<PointItem, PointListAdapter.PhotoListViewHolder> (PointListAdapter.PhotoListCallback()) {

    class PhotoListViewHolder(
        val binding: FragmentPointPhotosBinding,
        val displayWidth: Int,
        val state: PhotoListViewModel.PhotoListState) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            pointData: PointItem,
            photoOrder: PhotoOrder
        ) {
            val repository = RootRepository
            with(binding) {
                pointNameText.text = pointData.addressName
                // set size for screen elements (-2 - wrap content)
                photoFilesFragment.layoutParams.height = -2
                pointFilesParent.layoutParams.height = -2
                rvPointsPhotos.layoutParams.height = -2
                listOfPointFiles.layoutParams.height = -2
                //settings for recycle view
                val countOfImages = (displayWidth / 300) // grid size
                rvPointsPhotos.layoutManager =
                    GridLayoutManager(AppClass.appliactionContext(), countOfImages)
                rvPointsPhotos.isVerticalScrollBarEnabled = false
                rvPointsPhotos.isNestedScrollingEnabled = false
                rvPointsPhotos.adapter = PointFilesAdapter(pointData,state)
                //(rvPointsPhotos.adapter as PointPhotosAdapter).submitList(pointData.second)
                repository.getPointFilesForGallery(pointData,photoOrder) {
                    (rvPointsPhotos.adapter as PointFilesAdapter).submitList(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoListViewHolder {
        val binding = FragmentPointPhotosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoListViewHolder(binding, displayWidth,state)
    }

    override fun onBindViewHolder(holder: PhotoListViewHolder, position: Int) {
        holder.bind(getItem(position),photoOrder)
    }


    class PhotoListCallback : DiffUtil.ItemCallback<PointItem>() {
        override fun areItemsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
            return oldItem.lineUID == newItem.lineUID
        }
        override fun areContentsTheSame(oldItem: PointItem, newItem: PointItem): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

}


