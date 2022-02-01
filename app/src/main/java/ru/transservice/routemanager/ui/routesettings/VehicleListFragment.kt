package ru.transservice.routemanager.ui.routesettings

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentVehicleListBinding
import ru.transservice.routemanager.service.LoadResult


class VehicleListFragment : Fragment() {
    private var _binding: FragmentVehicleListBinding? = null
    private val binding get() = _binding!!
    private lateinit var vehicleAdapter: VehicleListAdapter
    private val viewModel: RouteSettingsViewModel by navGraphViewModels(R.id.navRouteSettings)
    lateinit var navController: NavController
    private val args: VehicleListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
        setHasOptionsMenu(true)

        vehicleAdapter = VehicleListAdapter {
            viewModel.setVehicle(it)
            navController.navigate(VehicleListFragmentDirections.actionVehicleListFragmentToRouteSettingsFragment())
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.queryHint = "Введите наименование"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearchQuery(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearchQuery(newText!!)
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        with(binding.rvVehicleList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context,
                    DividerItemDecoration.VERTICAL)
            )
            adapter = vehicleAdapter
        }
        viewModel.mediatorListVehicleResult.observe(viewLifecycleOwner, {
            when (it) {
                is LoadResult.Loading -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = true
                }
                is LoadResult.Success -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                    vehicleAdapter.updateItems(it.data ?: listOf())
                    if (vehicleAdapter.selectedPos == RecyclerView.NO_POSITION) {
                        args.vehicle?.let {
                            val pos = vehicleAdapter.getItemPosition(it)
                            vehicleAdapter.selectedPos = pos
                            with(binding.rvVehicleList) {
                                scrollToPosition(pos)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initViewModel(){
        viewModel.loadVehicle()
        viewModel.removeSources()
        viewModel.addSourcesRegion()
        viewModel.addSourcesVehicle()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            VehicleListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}