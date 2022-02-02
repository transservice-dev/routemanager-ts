
package ru.transservice.routemanager.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.view.doOnAttach
import ru.transservice.routemanager.extensions.padWithDisplayCutout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.databinding.FragmentGalleryBinding
import ru.transservice.routemanager.ui.gallery.list.PhotoListViewModel


class GalleryFragment internal constructor() : Fragment() {

    private val args: GalleryFragmentArgs by navArgs()

    private var mediaList: List<PointFile> = listOf()
    lateinit var navController: NavController
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoListViewModel by navGraphViewModels(R.id.navGallery) {PhotoListViewModel.Factory(args.point)}

    /** Adapter class used to present a fragment containing one photo or video as a page */
    private inner class MediaPagerAdapter(fm: Fragment) : FragmentStateAdapter(fm) {
        override fun getItemCount(): Int = mediaList.size
        override fun createFragment(position: Int): Fragment =
            PhotoShowFragment.create(mediaList[position])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        //retainInstance = true
        // Get root directory of media from navigation arguments
        mediaList = viewModel.getPointFilesList().value?: listOf()
        // Walk through all files in the root directory
        // We reverse the order of the list to present the last photos first
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        with(binding){
            shareButton.isEnabled = mediaList.isNotEmpty()
            // Populate the ViewPager and implement a cache of two media items
            photoViewPager.apply {
                offscreenPageLimit = 2
                adapter = MediaPagerAdapter(this@GalleryFragment)
                setCurrentItem(args.currentItem,false) //margarita-dev doesn't work with smoothScroll = true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use extension method to pad "inside" view containing UI using display cutout's bounds
               cutoutSafeArea.padWithDisplayCutout()
            }

            backButton.setOnClickListener {
                requireActivity().onBackPressed()
            }


            shareButton.setOnClickListener {
                mediaList.getOrNull(photoViewPager.currentItem)?.let { pointFile ->
                    // Create a sharing intent
                    val imageUris : ArrayList<Uri> = arrayListOf()
                    imageUris.add(Uri.parse(pointFile.filePath))

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM,imageUris)
                        type = "image/*"
                    }

                    startActivity(Intent.createChooser(shareIntent,"Отправка фото"))
                }
            }

        }

    }
}
