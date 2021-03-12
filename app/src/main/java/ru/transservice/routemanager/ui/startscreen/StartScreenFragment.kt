package ru.transservice.routemanager.ui.startscreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentRegionListBinding
import ru.transservice.routemanager.databinding.FragmentRouteSettingsBinding
import ru.transservice.routemanager.databinding.FragmentStartScreenBinding

class StartScreenFragment : Fragment() {

    private var _binding: FragmentStartScreenBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartScreenBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vehicleLayout.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToRouteSettingsFragment())
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StartScreenFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}