package ru.transservice.routemanager.ui.polygon

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.databinding.FragmentPolygonSelectionBinding
import ru.transservice.routemanager.extensions.hideKeyboard

class PolygonSelectionFragment : Fragment() {

    private var _binding: FragmentPolygonSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var polygon: PolygonItem
    lateinit var navController: NavController
    private lateinit var viewModel: PolygonViewModel
    private lateinit var listAdapter: PolygonSelectionAdapter
    private var currentRowItem: PolygonItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        setHasOptionsMenu(true)
        listAdapter = PolygonSelectionAdapter{ polygonItem, mode ->
            when (mode){
                // polygon change
                0 -> {
                    currentRowItem = polygonItem
                    navController.navigate(PolygonSelectionFragmentDirections.actionPolygonSelectionFragmentToPolygonListFragment("test"))
                }
            }
        }

    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonSelectionBinding.inflate(requireActivity().layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)

        with(binding.rvList){
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context,
                    DividerItemDecoration.VERTICAL)
            )
            adapter = listAdapter
        }

        polygon = PolygonItem(0,"1", "1","test",1, true,false)
        binding.ibAdd.setOnClickListener {

        }
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
        viewModel.getDocPolygonList().observe(viewLifecycleOwner, {
            listAdapter.updateItems(it)
        })

        viewModel.getPolygon().observe(viewLifecycleOwner,{
            currentRowItem?.let { currentItem ->
                if (currentItem != it){
                    viewModel.changePolygonRow(currentItem,it)
                }
            }
        })
    }

    fun initViewModel(){
        viewModel  = ViewModelProvider(requireActivity(), PolygonViewModel.PolygonViewModelFactory()).get(
            PolygonViewModel::class.java)
        viewModel.loadCurrentPolygons()
    }

    companion object
}