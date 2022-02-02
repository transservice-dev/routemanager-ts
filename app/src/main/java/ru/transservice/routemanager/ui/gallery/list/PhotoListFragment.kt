package ru.transservice.routemanager.ui.gallery.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentPhotoListBinding


class PhotoListFragment : Fragment() {

    private var _binding: FragmentPhotoListBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoListAdapter: PointListAdapter
    lateinit var navController: NavController

    private var point: PointItem? = null
    private lateinit var photoOrder: PhotoOrder

    private val args: PhotoListFragmentArgs by navArgs()
    private val viewModel: PhotoListViewModel by navGraphViewModels(R.id.navGallery)  { PhotoListViewModel.Factory(args.point) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        point = args.point
        photoOrder = args.photoOrder
        photoListAdapter = PointListAdapter(photoOrder, (requireActivity() as MainActivity).getDisplayWidth(),viewModel.state)
        setHasOptionsMenu(true)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navParams.collectLatest{
                    if (it.isRequired ) {
                        navController.navigate(PhotoListFragmentDirections.actionPhotoListFragmentToGalleryFragment(it.position,args.point))
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_share, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                if (viewModel.selectedItems.value != null) {
                    val imageUris: ArrayList<Uri> = arrayListOf()
                    for (pointFile in viewModel.selectedItems.value!!) {
                        imageUris.add(Uri.parse(pointFile.filePath))
                    }

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                        type = "image/*"
                    }

                    requireActivity().startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Отправка фото"
                        )
                    )
                    true
                } else {
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)

        with(binding){
            recyclerviewAllFiles.adapter = photoListAdapter
            recyclerviewAllFiles.layoutManager = LinearLayoutManager(context)
        }

        viewModel.loadPointList().observe(viewLifecycleOwner, {
            photoListAdapter.submitList(it)
        })

        viewModel.selectedItems.observe(viewLifecycleOwner,{
            setMenuVisibility(it.isNotEmpty())
        })

    }

    companion object {
        @JvmStatic
        fun newInstance() =
            PhotoListFragment().apply {
            }
    }
}