package ru.transservice.routemanager.ui.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import ru.transservice.routemanager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.databinding.FragmentPhotoPriviewBinding
import ru.transservice.routemanager.model.PhotoProcessing
import ru.transservice.routemanager.model.PhotoFacade
import ru.transservice.routemanager.model.PhotoViewModel
import ru.transservice.routemanager.ui.point.PointItemViewModel

class PhotoFragment : Fragment() {
    private var _binding: FragmentPhotoPriviewBinding? = null
    private val binding get() = _binding!!
    private val navController: NavController by lazy { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment) }
    private val args: PhotoFragmentArgs by navArgs()
    private val viewPointModel: PointItemViewModel by navGraphViewModels(R.id.navPoint) { PointItemViewModel.Factory(args.params.lineUID) }
    private val vm: PhotoViewModel by navGraphViewModels(R.id.navPoint)

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

        with(binding) {
            tvConfirm.setOnClickListener { ok() }
            tvCancel.setOnClickListener { cancel() }
        }


        val ctx = requireContext()
        val photoProcessing = PhotoProcessing(ctx,vm.photo)

        photoProcessing.prepare()
        binding.photoPreview.setImageBitmap(photoProcessing.bitmap)

        lifecycleScope.launch {
            val img = withContext(Dispatchers.Default) {
                photoProcessing.execute()
                photoProcessing.bitmap
            }
            binding.photoPreview.setImageBitmap(img)
        }

    }

    fun ok() = lifecycleScope.launch(Dispatchers.Default) {
        PhotoFacade.saveInDb(vm.photo,::okDone)
    }

    fun okDone() {
        val needUpdate = when (vm.photo.photoType) {
            PhotoOrder.PHOTO_AFTER -> true
            PhotoOrder.PHOTO_CANTDONE -> true
            else -> false
        }
        if (needUpdate) viewPointModel.updatePointAndDoneStatus(vm.photo.point.copy())
        navController.popBackStack(R.id.cameraFragment, true)
    }

    fun cancel() {
        vm.photo.file.delete()
        navController.popBackStack()
    }

}
