package ru.transservice.routemanager.ui.gallery.list

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.extensions.dpToPx
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPointPhotosBinding
import ru.transservice.routemanager.repositories.RootRepository

class PointListAdapter(
    val photoOrder: PhotoOrder,
    private val displayWidth: Int,
    val state: PhotoListViewModel.PhotoListState
    ) : ListAdapter<PointItem, PointListAdapter.PhotoListViewHolder> (PointListAdapter.PhotoListCallback()) {

    class PhotoListViewHolder(
        val binding: FragmentPointPhotosBinding,
        private val displayWidth: Int,
        val state: PhotoListViewModel.PhotoListState) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            pointData: PointItem,
            photoOrder: PhotoOrder
        ) {
            val repository = RootRepository
            with(binding) {
                pointNameText.text = pointData.addressName
                //settings for recycle view
                val countOfImages = (displayWidth / AppClass.appliactionContext().dpToPx(100)).toInt() // grid size
                rvPointsPhotos.layoutManager =
                    GridLayoutManager(AppClass.appliactionContext(), countOfImages)
                rvPointsPhotos.isVerticalScrollBarEnabled = false
                rvPointsPhotos.isNestedScrollingEnabled = false
                rvPointsPhotos.adapter = PointFilesAdapter(pointData,state)

                rvPointsPhotos.addItemDecoration(
                    SpaceItemDecoration(AppClass.appliactionContext().resources.getDimensionPixelSize(R.dimen.margin_tiny)))

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

class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space;
        outRect.right = space;
        outRect.bottom = space;
        outRect.top = space

        // Add top margin only for the first item to avoid double space between items
       /* if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space;
        } else {
            outRect.top = 0;
        }*/
    }

}

