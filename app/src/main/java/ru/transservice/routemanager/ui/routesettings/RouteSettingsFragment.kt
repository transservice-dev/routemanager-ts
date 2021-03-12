package ru.transservice.routemanager.ui.routesettings

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentRouteSettingsBinding
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.ui.startscreen.StartScreenFragmentDirections
import java.text.SimpleDateFormat
import java.util.*

class RouteSettingsFragment : Fragment() {

    private var _binding: FragmentRouteSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RouteSettingsViewModel
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        initViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRouteSettingsBinding.inflate(inflater,container,false)
        return binding.root    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRegion.setOnClickListener {
           navController.navigate(RouteSettingsFragmentDirections.actionRouteSettingsFragmentToRegionListFragment(null))
        }

        binding.tvVehicle.setOnClickListener {
            navController.navigate(RouteSettingsFragmentDirections.actionRouteSettingsFragmentToVehicleListFragment())
        }


        var cal = Calendar.getInstance()
        cal.time = viewModel.getDate().value ?: Date()

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.setDate(cal.time)

                val myFormat = "yyyy.MM.dd" // mention the format you need
                val sdf = SimpleDateFormat(myFormat, Locale("ru"))
                binding.tvDate.text = sdf.format(cal.time)

            }

        binding.tvDate.setOnClickListener {
            DatePickerDialog(requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        viewModel.getRegion().observe(viewLifecycleOwner, Observer {
           binding.tvRegion.text = it?.name
        })

        viewModel.getVehicle().observe(viewLifecycleOwner, Observer {
            binding.tvVehicle.text = it?.name
        })

        viewModel.getDate().observe(viewLifecycleOwner, Observer {
            val myFormat = "yyyy.MM.dd" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale("ru"))
            binding.tvDate.text = sdf.format(cal.time)
        })
    }

    fun initViewModel(){
        viewModel  = ViewModelProvider(this, RouteSettingsViewModel.RouteSettingsViewModelFactory()).get(
            RouteSettingsViewModel::class.java)
    }
    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RouteSettingsFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}