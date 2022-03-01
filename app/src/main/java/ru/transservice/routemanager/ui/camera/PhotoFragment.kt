/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import ru.transservice.routemanager.ui.point.PointItemViewModel
import ru.transservice.routemanager.utils.ImageFileProcessing
import java.io.File

class PhotoFragment : Fragment() {

    private var _binding: FragmentPhotoPriviewBinding? = null
    private val binding get() = _binding!!

    private var currentFile: File? = null

    private val navController: NavController by lazy { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment) }
    private val args: PhotoFragmentArgs by navArgs()
    private val viewPointModel: PointItemViewModel by navGraphViewModels(R.id.navPoint) { PointItemViewModel.Factory(args.params.lineUID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentFile = File(args.fileName)
        if (currentFile == null) {
            Toast.makeText(requireContext(), "Ошибка получения фото, неверное имя файла",Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
        Log.d(tag(), "current file: ${currentFile?.absolutePath}")
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
        val resource = currentFile ?: R.drawable.ic_photo
        if (currentFile != null && location != null) {
            Log.d(tag(), "location successfully requested lat: ${location.latitude} lon: ${location.longitude}")
            ImageFileProcessing().createResultImageFile(
                currentFile!!.absolutePath,
                location.latitude,
                location.longitude,
                args.params,
                requireContext()
            )
        }
        Glide.with(requireContext()).load(resource).into(binding.photoPreview)
        with(binding) {
            tvConfirm.setOnClickListener {
                currentFile?.let {
                    viewPointModel.savePointFile(it, location, args.params.fileOrder)
                }
                navController.popBackStack(R.id.cameraFragment, true)
            }
            tvCancel.setOnClickListener {
                currentFile?.delete()
                navController.popBackStack()
            }
        }
    }
}

