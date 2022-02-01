package ru.transservice.routemanager.ui.routesettings

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import com.google.android.material.snackbar.Snackbar
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentRouteSettingsBinding
import java.text.SimpleDateFormat
import java.util.*

class RouteSettingsFragment : Fragment() {

    private var _binding: FragmentRouteSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RouteSettingsViewModel by navGraphViewModels(R.id.navRouteSettings)
    lateinit var navController: NavController
    private var snackbarMessage: Snackbar? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteSettingsBinding.inflate(inflater, container, false)
        return binding.root    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        binding.tvRegion.setOnClickListener {
           navController.navigate(RouteSettingsFragmentDirections.actionRouteSettingsFragmentToRegionListFragment(viewModel.getRegion().value))
        }

        binding.tvVehicle.setOnClickListener {
            navController.navigate(RouteSettingsFragmentDirections.actionRouteSettingsFragmentToVehicleListFragment(viewModel.getVehicle().value))
        }

        binding.tvRoute.setOnClickListener {
            navController.navigate(RouteSettingsFragmentDirections.actionRouteSettingsFragmentToRouteListFragment(viewModel.getRoute().value))
        }

        binding.tvRoute.visibility = if (viewModel.searchByRoute) View.VISIBLE else View.GONE
        binding.tvVehicle.visibility = if (viewModel.searchByRoute) View.GONE else View.VISIBLE


        val cal = Calendar.getInstance()
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
                    cal.get(Calendar.DAY_OF_MONTH))
                .show()
        }

        viewModel.getRegion().observe(viewLifecycleOwner, {
            binding.tvRegion.text = it?.name
        })

        viewModel.getVehicle().observe(viewLifecycleOwner, {
            binding.tvVehicle.text = it?.name
        })

        viewModel.getRoute().observe(viewLifecycleOwner, {
            binding.tvRoute.text = it?.name
        })

        viewModel.getDate().observe(viewLifecycleOwner, {
            val myFormat = "yyyy.MM.dd" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale("ru"))
            binding.tvDate.text = sdf.format(cal.time)
        })

        viewModel.getEditingIsAvailable().observe(viewLifecycleOwner, {
            with(binding.root) {
                isClickable = it
                isEnabled = it
                if (this is ViewGroup) {
                    this.children.forEach { childView ->
                        childView.isClickable = it
                        childView.isEnabled = it
                    }
                }
            }
            if (!binding.root.isClickable) {
                if (snackbarMessage?.isShown != true) {
                    snackbarMessage = Snackbar.make(
                        binding.root,
                        "Существует активное задание, редактирование настроек запрещено. Завершите маршрут для смены настроек",
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackbarMessage?.let { snackbar ->
                        val snackTextView =
                            snackbar.view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                        snackTextView.maxLines = 10
                        snackbar.setAction(
                            resources.getString(R.string.ok)
                        ) { snackbar.dismiss() }
                        snackbar.show()
                    }
                }
            } else {
                snackbarMessage?.dismiss()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        snackbarMessage?.dismiss()
    }


    companion object {

        @JvmStatic
        fun newInstance() =
            RouteSettingsFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}