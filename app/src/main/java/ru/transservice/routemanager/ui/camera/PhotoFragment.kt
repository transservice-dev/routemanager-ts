package ru.transservice.routemanager.ui.camera

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import ru.transservice.routemanager.R
import com.bumptech.glide.Glide
import ru.transservice.routemanager.databinding.FragmentPhotoPriviewBinding
import ru.transservice.routemanager.extensions.tag
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.model.Photo
import ru.transservice.routemanager.ui.point.PointItemViewModel
import ru.transservice.routemanager.utils.ImageFileProcessing
import java.io.File

class PhotoFragment : Fragment() {

    private var _binding: FragmentPhotoPriviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var photo: Photo

    private val navController: NavController by lazy { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment) }
    private val args: PhotoFragmentArgs by navArgs()
    private val viewPointModel: PointItemViewModel by navGraphViewModels(R.id.navPoint) { PointItemViewModel.Factory(args.params.lineUID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photo = Photo.unpack(args.fileName)
        Log.d(tag(), "current file: ${photo.file.absolutePath}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoPriviewBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val location: Location? = NavigationServiceConnection.getLocation()
        val resource = photo.file
        if (location != null) {
            Log.d(
                tag(),
                "location successfully requested lat: ${location.latitude} lon: ${location.longitude}"
            )
            ImageFileProcessing().createResultImageFile(
                photo.file.absolutePath,
                location.latitude,
                location.longitude,
                args.params,
                requireContext()
            )
        }
        else {
            ImageFileProcessing().createResultImageFile(
                photo.file.absolutePath,
                0.toDouble(),
                0.toDouble(),
                args.params,
                requireContext(),
                false
            )
        }
        Glide.with(requireContext()).load(resource).into(binding.photoPreview)
        with(binding) {
            tvConfirm.setOnClickListener {
                viewPointModel.savePointFile(photo.file, location, args.params.fileOrder)
                navController.popBackStack(R.id.cameraFragment, true)
            }
            tvCancel.setOnClickListener {
                photo.file.delete()
                navController.popBackStack()
            }
        }
    }
}
