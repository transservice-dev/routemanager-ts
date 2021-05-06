package ru.transservice.routemanager.ui.gallery.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPointPhotosBinding
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.ui.gallery.pointlist.PointPhotosAdapter

class PhotoListAdapter(val photoOrder: PhotoOrder, val activity: MainActivity) : RecyclerView.Adapter<PhotoListAdapter.PhotoListViewHolder>() {

    var items: List<PointItem> = listOf()

    class PhotoListViewHolder(val binding: FragmentPointPhotosBinding, val activity: MainActivity) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            pointItem: PointItem,
            photoOrder: PhotoOrder
        ) {
            val repository = RootRepository
            with(binding) {
                pointNameText.text = pointItem.addressName
                // set size for screen elements (-2 - wrap content)
                photoFilesFragment.layoutParams.height = -2
                pointFilesParent.layoutParams.height = -2
                rvPointsPhotos.layoutParams.height = -2
                listOfPointFiles.layoutParams.height = -2
                //settings for recycle view
                val countOfImages = (activity.getDisplayWidth() / 300) // grid size
                rvPointsPhotos.layoutManager =
                    GridLayoutManager(AppClass.appliactionContext(), countOfImages)
                rvPointsPhotos.isVerticalScrollBarEnabled = false
                rvPointsPhotos.isNestedScrollingEnabled = false
                rvPointsPhotos.adapter = PointPhotosAdapter(pointItem,activity)

                repository.getPointFilesForGallery(pointItem,photoOrder) {
                    (rvPointsPhotos.adapter as PointPhotosAdapter).updateItems(it)
                }

                /*viewModel.loadPointFilesList(pointItem)
                    .observe(parentFragment.viewLifecycleOwner, Observer {
                        (rvPointsPhotos.adapter as PointPhotosAdapter).updateItems(it)
                    })*/
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoListViewHolder {
        val binding = FragmentPointPhotosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoListViewHolder(binding, activity)
    }

    override fun onBindViewHolder(holder: PhotoListViewHolder, position: Int) {
        holder.bind(items[position],photoOrder)

    }

    override fun getItemCount() = items.size

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


