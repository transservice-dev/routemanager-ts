package ru.transservice.routemanager.ui.routesettings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentRegionListBinding
import ru.transservice.routemanager.service.LoadResult


class RegionListFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var regionAdapter: RegionListAdapter
    private lateinit var viewModel: RouteSettingsViewModel
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)

        regionAdapter = RegionListAdapter {
            viewModel.setRegion(it)
            navController.navigate(RegionListFragmentDirections.actionRegionListFragmentToRouteSettingsFragment())
        }


        viewModel.loadRegions().observe(this, Observer {
            when (it) {
                is LoadResult.Loading -> {
                    //TODO splash screen loading
                }
                is LoadResult.Success ->{
                    regionAdapter.updateItems(it.data ?: listOf())
                }
            }
        })

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
        with(binding.rvRegionsList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
            adapter = regionAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RegionListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun initViewModel(){
        viewModel  = ViewModelProvider(this, RouteSettingsViewModel.RouteSettingsViewModelFactory()).get(
        RouteSettingsViewModel::class.java)
    }
}