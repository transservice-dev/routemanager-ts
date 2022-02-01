package ru.transservice.routemanager.ui.polygon

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentPolygonListBinding
import ru.transservice.routemanager.extensions.hideKeyboard

class PolygonListFragment : Fragment()  {
    private var _binding: FragmentPolygonListBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    val args: PolygonListFragmentArgs by navArgs()
    private lateinit var listAdapter: PolygonListAdapter
    private lateinit var viewModel: PolygonViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        setHasOptionsMenu(true)

        listAdapter = PolygonListAdapter {
            viewModel.setPolygon(it)
            setFragmentResult(args.requestKey, bundleOf("polygon" to it))
            navController.popBackStack()
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
                requireActivity().hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearchQuery(newText!!)
                return true
            }
        })

        val polygonItem = menu.findItem(R.id.action_polygon)
        polygonItem.setVisible(false)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_polygon -> {
                val dialog = PolygonSelectionDialog.newInstance()
                dialog.show(childFragmentManager, "polygonDialog")
                //navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPolygonSelectionFragment())
            }
        }


        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)

        binding.swtShowAll.setOnCheckedChangeListener { _: CompoundButton, fullList: Boolean ->
            viewModel.setFullList(fullList)
        }

        with(binding.rvList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context,
                    DividerItemDecoration.VERTICAL)
            )
            adapter = listAdapter
        }
        viewModel.mediatorListResult.observe(viewLifecycleOwner, {
            listAdapter.updateItems(it)
        })

        viewModel.fullList.observe(viewLifecycleOwner, {
            binding.swtShowAll.isChecked = it
        })
    }


    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(),
            PolygonViewModel.Factory()).get(
            PolygonViewModel::class.java)
        viewModel.removeSources()
        viewModel.addSources()
        viewModel.loadAvailablePolygons()
    }
    companion object {

        private const val TAG = "${AppClass.TAG}: PolygonListFragment"

        @JvmStatic
        fun newInstance() =
            PolygonListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}
