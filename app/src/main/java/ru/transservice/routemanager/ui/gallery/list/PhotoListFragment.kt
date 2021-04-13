package ru.transservice.routemanager.ui.gallery.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPhotoListBinding


class PhotoListFragment : Fragment() {

    private var _binding: FragmentPhotoListBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoListAdapter: PhotoListAdapter
    private lateinit var viewModel: PhotoListViewModel
    private var point: PointItem? = null
    private lateinit var photoOrder: PhotoOrder

    private val args: PhotoListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        point = args.point
        photoOrder = args.photoOrder
        initViewModel()
        photoListAdapter = PhotoListAdapter(photoOrder, (requireActivity() as MainActivity))
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_share, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                val imageUris: ArrayList<Uri> = arrayListOf()
                for (pointFile in viewModel.selectedItems) {
                    imageUris.add(Uri.parse(pointFile.filePath))
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                    type = "image/*"
                }

                requireActivity().startActivity(Intent.createChooser(shareIntent, "Отправка фото"))
                viewModel.selectedItems.clear()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotoListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).navMenu.visibility = View.GONE
        (requireActivity() as MainActivity).supportActionBar?.show()

        with(binding){
            recyclerviewAllFiles.adapter = photoListAdapter
            recyclerviewAllFiles.layoutManager = LinearLayoutManager(context)
        }

        viewModel.loadPointList().observe(viewLifecycleOwner, Observer {
            photoListAdapter.updateItems(it)
        })

        viewModel.selectionMode.observe(viewLifecycleOwner,{
            //binding.floatingActionButton.visibility = if (it) View.VISIBLE else View.GONE
            setMenuVisibility(it)
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(), PhotoListViewModel.PhotoListViewModelFactory(point)).get(
            PhotoListViewModel::class.java)
        viewModel.pointItem = point
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            PhotoListFragment().apply {
            }
    }
}