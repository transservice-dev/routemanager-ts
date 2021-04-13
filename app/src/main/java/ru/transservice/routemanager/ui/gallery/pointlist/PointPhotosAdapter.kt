package ru.transservice.routemanager.ui.gallery.pointlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPointPhotosBinding
import ru.transservice.routemanager.databinding.ItemPointPhotoBinding
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.ui.gallery.list.PhotoListAdapter
import ru.transservice.routemanager.ui.gallery.list.PhotoListFragmentDirections
import ru.transservice.routemanager.ui.gallery.list.PhotoListViewModel

class PointPhotosAdapter(val pointItem: PointItem, val activity: MainActivity) : RecyclerView.Adapter<PointPhotosAdapter.PointPhotoViewHolder>() {

    var items: List<PointFile> = listOf()
    val viewModel = ViewModelProvider(activity, PhotoListViewModel.PhotoListViewModelFactory(pointItem)).get(
            PhotoListViewModel::class.java)

    class PointPhotoViewHolder(val binding: ItemPointPhotoBinding, val context: Context) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pointFile: PointFile){
            with(binding){
                fileStatus.text = pointFile.photoOrder.title
                Glide.with(context).load(pointFile.filePath).into(ivPointPhoto)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointPhotoViewHolder {
        val binding = ItemPointPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PointPhotoViewHolder(binding,AppClass.appliactionContext())
    }

    override fun onBindViewHolder(holder: PointPhotoViewHolder, position: Int) {
        holder.bind(items[position])

        with(holder.binding){
            ivPointPhoto.setOnClickListener {
               showPhotosOrSelect(root,position)
            }
            ivPointPhoto.setOnLongClickListener {
                setSelectionMode(root,position)

            }
            root.setOnClickListener {
                showPhotosOrSelect(root, position)
            }
            root.setOnLongClickListener {
                setSelectionMode( it, position)
            }
        }
    }


    private fun setSelectionMode(view: View,position: Int): Boolean {
        viewModel.selectionMode.value = true
        view.isSelected = true
        viewModel.selectedItems.add(items[position])
        return viewModel.selectionMode.value ?: false
    }

    private fun showPhotosOrSelect(view: View, position: Int){
        if (viewModel.selectionMode.value == true) {
            view.isSelected = !view.isSelected
            if (view.isSelected){
                viewModel.selectedItems.add(items[position])
            }else{
                viewModel.selectedItems.remove(items[position])
            }
            viewModel.selectionMode.value = viewModel.selectedItems.isNotEmpty()
        }else{
            viewModel.setPointFilesList(items)
            val navController = Navigation.findNavController(activity, R.id.nav_host_fragment)
            navController.navigate(PhotoListFragmentDirections.actionPhotoListFragmentToGalleryFragment(position,pointItem))
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(data: List<PointFile>) {

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