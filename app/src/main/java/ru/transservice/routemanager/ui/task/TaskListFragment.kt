package ru.transservice.routemanager.ui.task

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.transition.MaterialElevationScale
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PointStatuses
import ru.transservice.routemanager.databinding.FragmentTaskListBinding
import ru.transservice.routemanager.extensions.hideKeyboard
import ru.transservice.routemanager.location.NavigationServiceConnection
import java.util.*

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: TaskListViewModel
    private lateinit var taskListAdapter: TaskListAdapter
    private lateinit var btsBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).setDuration(1000L)
        reenterTransition = MaterialElevationScale(true).setDuration(1000L)
        setHasOptionsMenu(true)
        initViewModel()

        taskListAdapter = TaskListAdapter {
            viewModel.setCurrentPoint(it)
            with(binding.btsPointList) {
                ibtnCannotDone.visibility = if (it.polygon) View.GONE else View.VISIBLE
                ibtnPhotos.visibility = if (it.polygon) View.GONE else View.VISIBLE
            }
            if (btsBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                btsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //requireActivity().menuInflater.inflate(R.menu.menu_search, menu)
        inflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.queryHint = "Введите наименование точки"
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
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(),TaskListViewModel.TaskListViewModelFactory(requireActivity().application)).get(TaskListViewModel::class.java)
        viewModel.removeSources()
        viewModel.addSources()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        /*if (AppClass.gps!!.trackingIsOn) {
            AppClass.gps!!.stopUsingGPS()
        }*/
        stopNavService()
        viewModel.loadPointList()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).navMenu.visibility = View.GONE
        (requireActivity() as MainActivity).supportActionBar?.show()
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        initViews()
        initBottomSheetActions()
        initLiveDataObservers()
    }

    private fun initLiveDataObservers() {
        /*viewModel.loadPointList().observe(viewLifecycleOwner, Observer {
             taskListAdapter.updateItems(it)
         })*/

        viewModel.mediatorListResult.removeObservers(viewLifecycleOwner)
        viewModel.mediatorListResult.observe(viewLifecycleOwner, {
            taskListAdapter.updateItems(it)
        })

        viewModel.getCurrentPoint().observe(viewLifecycleOwner,  {
            with(binding.btsPointList) {
                tvCurrentPointName.text = it.addressName
            }
        })
    }

    private fun initViews() {
        with(binding.rvTaskList) {
            layoutManager = LinearLayoutManager(context)
            adapter = taskListAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
        }

        viewModel.setFullList(binding.btsPointList.listSwitcher.isChecked)

        btsBehavior = BottomSheetBehavior.from(binding.btsPointList.bottomSheetRoute)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initBottomSheetActions() {
        with(binding.btsPointList){
            ibtnCanDone.setOnClickListener {
                if (viewModel.getCurrentPoint().value!!.polygon) {
                    val resultPoint = viewModel.getCurrentPoint().value!!.copy()
                    resultPoint.done = true
                    resultPoint.timestamp = Date()
                    viewModel.updateCurrentPoint(resultPoint)
                    viewModel.loadPointList().value?.let {
                        taskListAdapter.updateItems(it)
                    }
                }else{
                    startNavService()
                    navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPointFragment(viewModel.getCurrentPoint().value!!,PointStatuses.DONE))
                    btsBehavior.state  = BottomSheetBehavior.STATE_COLLAPSED
                }

            }

            ibtnCannotDone.setOnClickListener {
                NavigationServiceConnection.startTracking()
                navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPointFragment(viewModel.getCurrentPoint().value!!,PointStatuses.CANNOT_DONE))
            }

            ibtnHome.setOnClickListener {
                navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToStartScreenFragment())
            }

            ibtnNavigation.setOnClickListener {
                viewModel.getCurrentPoint().value?.let{
                    buildRoute(it,"2gis")
                }
            }

            ibtnPhotos.setOnClickListener {
                navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            val onSwitchChanged = { fullList: Boolean ->
                viewModel.setFullList(fullList)
                btsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            switcherText.setOnClickListener {
                listSwitcher.isChecked = !listSwitcher.isChecked
                onSwitchChanged(listSwitcher.isChecked)
            }

            listSwitcher.setOnCheckedChangeListener { _: CompoundButton, fullList: Boolean ->
                onSwitchChanged(fullList)
            }

            upperComponentOfBottomSheet.setOnClickListener {
                when(btsBehavior.state){
                    BottomSheetBehavior.STATE_EXPANDED -> btsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        if (viewModel.getCurrentPoint().value != null)
                            btsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        else
                            Toast.makeText(context, "Выбирете позицию в списке!",Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startNavService(){
        (requireActivity() as MainActivity).startForegroundService((requireActivity() as MainActivity).locationServiceIntent)
        //NavigationServiceConnection.startTracking()
    }

    private fun stopNavService(){
        if (NavigationServiceConnection.isActive()) {
            NavigationServiceConnection.stopTracking()
        }
    }

    private fun buildRoute(point: PointItem, naviApp: String) {
        //if (location != null && location!!.latitude != 0.0 && location!!.longitude != 0.0) {
        // val startlat = location!!.latitude
        // val startlon = location!!.longitude
        val endlat = point.addressLat
        val endlon = point.addressLon
        if (endlat == 0.0 || endlon == 0.0) {
            Toast.makeText(
                    activity,
                    "Отмена.Для данной точки не заданы координаты",
                    Toast.LENGTH_LONG
            ).show()
            return
        }

        var uriString = ""
        var packageName = ""

        when (naviApp) {
            "yandex" -> {
                uriString = "yandexnavi://build_route_on_map?lat_to=$endlat&lon_to=$endlon"
                packageName = "ru.yandex.yandexnavi"
            }
            "2gis" -> {
                uriString = "dgis://2gis.ru/routeSearch/rsType/car/to/$endlon,$endlat"
                packageName = "ru.dublgis.dgismobile"
            }
        }

        if (uriString.isEmpty() || packageName.isEmpty()) {
            Toast.makeText(activity,"Отмена. Неизвестное приложение навигации", Toast.LENGTH_LONG).show()
            return
        }

        val uri = Uri.parse(uriString)
        var intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        val packageManager: PackageManager = requireContext().packageManager
        val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val isIntentSafe: Boolean = activities.isNotEmpty()
        if (isIntentSafe) {
            startActivity(intent)
        } else {
            // Открываем страницу приложения Яндекс.Карты в Google Play.
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
            startActivity(intent)
        }

        /*} else {
            Toast.makeText(
                requireContext(),
                "Текущее местоположение не определено",
                Toast.LENGTH_LONG
            ).show()
        }*/

    }

    companion object {

        private const val TAG = "${AppClass.TAG}: TaskListFragment"

        @JvmStatic
        fun newInstance() =
            TaskListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}