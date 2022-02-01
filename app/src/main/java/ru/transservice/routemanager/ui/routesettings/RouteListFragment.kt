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
import ru.transservice.routemanager.databinding.FragmentRouteListBinding
import ru.transservice.routemanager.service.LoadResult


class RouteListFragment : Fragment() {
    private var _binding: FragmentRouteListBinding? = null
    private val binding get() = _binding!!
    private lateinit var routeAdapter: RouteListAdapter
    private val viewModel: RouteSettingsViewModel by navGraphViewModels(R.id.navRouteSettings)
    lateinit var navController: NavController
    private val args: RouteListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
        setHasOptionsMenu(true)

        routeAdapter = RouteListAdapter {
            viewModel.setRoute(it)
            navController.navigate(RouteListFragmentDirections.actionRouteListFragmentToRouteSettingsFragment())
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
        _binding = FragmentRouteListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        with(binding.rvRouteList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context,
                    DividerItemDecoration.VERTICAL)
            )
            adapter = routeAdapter
        }
        viewModel.mediatorListRouteResult.observe(viewLifecycleOwner, {
            when (it) {
                is LoadResult.Loading -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = true
                }
                is LoadResult.Success -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                    routeAdapter.updateItems(it.data ?: listOf())
                    if (routeAdapter.selectedPos == RecyclerView.NO_POSITION) {
                        args.route?.let {
                            val pos = routeAdapter.getItemPosition(it)
                            routeAdapter.selectedPos = pos
                            with(binding.rvRouteList) {
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
        viewModel.loadRoutes()
        viewModel.removeSources()
        viewModel.addSourcesRegion()
        viewModel.addSourcesRoute()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            RouteListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}