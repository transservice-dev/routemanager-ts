package ru.transservice.routemanager.ui.routesettings

import android.graphics.Region
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Adapter
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.databinding.FragmentRegionListBinding
import ru.transservice.routemanager.service.LoadResult


class RegionListFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var regionAdapter: RegionListAdapter
    private lateinit var viewModel: RouteSettingsViewModel
    lateinit var navController: NavController
    private val args: RegionListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
        setHasOptionsMenu(true)

        regionAdapter = RegionListAdapter {
            viewModel.setRegion(it)
            navController.navigate(RegionListFragmentDirections.actionRegionListFragmentToRouteSettingsFragment())
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
    ): View? {
        _binding = FragmentRegionListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        with(binding.rvRegionsList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
            adapter = regionAdapter
        }

        viewModel.mediatorListRegionResult.observe(viewLifecycleOwner, {
            when (it) {
                is LoadResult.Loading -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = true
                }
                is LoadResult.Success -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                    regionAdapter.updateItems(it.data ?: listOf())
                    if (regionAdapter.selectedPos == RecyclerView.NO_POSITION) {
                        args.region?.let {
                            val pos = regionAdapter.getItemPosition(it)
                            regionAdapter.selectedPos = pos
                            with(binding.rvRegionsList) {
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

    companion object {

        @JvmStatic
        fun newInstance() =
            RegionListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun initViewModel(){
        viewModel  = ViewModelProvider(requireActivity(), RouteSettingsViewModel.RouteSettingsViewModelFactory()).get(
        RouteSettingsViewModel::class.java)
        viewModel.loadRegions()
        viewModel.removeSources()
        viewModel.addSourcesRegion()
        viewModel.addSourcesVehicle()
    }
}