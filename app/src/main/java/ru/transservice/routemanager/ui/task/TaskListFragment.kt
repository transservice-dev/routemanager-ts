package ru.transservice.routemanager.ui.task

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.transition.MaterialElevationScale
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.MainNavigationDirections
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PointStatuses
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.databinding.FragmentTaskListBinding
import ru.transservice.routemanager.extensions.hideKeyboard
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.ui.polygon.PolygonSelectionDialog
import java.util.*


class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    lateinit var navController: NavController
    private val viewModel: TaskListViewModel by viewModels()
    private lateinit var taskListAdapter: TaskListAdapter
    private lateinit var btsBehavior: BottomSheetBehavior<View>

    private var uloadingAvailable = false

    private val requestKeyPolygon: String = "polygonForList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).setDuration(1000L)
        reenterTransition = MaterialElevationScale(true).setDuration(1000L)
        setHasOptionsMenu(true)
        initViewModel()

        taskListAdapter = TaskListAdapter {
            viewModel.setCurrentPoint(it)
            if (btsBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                btsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            with (taskListAdapter){
                notifyItemChanged(currentList.indexOf(selectedItem))
                selectedItem = it
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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

        val polygonItem = menu.findItem(R.id.action_polygon)
        polygonItem.isVisible = uloadingAvailable

        val fullListItem = menu.findItem(R.id.action_fulllist)
        fullListItem.isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_polygon -> {
                val dialog = PolygonSelectionDialog.newInstance()
                dialog.show(childFragmentManager, "polygonDialog")
            }

            R.id.action_fulllist -> {
                val result = viewModel.changeFullList()
                item.title = if (result) "Невыполненные" else getString(R.string.fullList)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initViewModel() {
        //viewModel = ViewModelProvider(requireActivity(),TaskListViewModel.TaskListViewModelFactory(requireActivity().application)).get(TaskListViewModel::class.java)
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
        stopNavService()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).navMenu.visibility = View.GONE
        (requireActivity() as MainActivity).supportActionBar?.show()
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        initViews()
        initBottomSheetActions()
        initLiveDataObservers()
        initPolygonSelectionDialogListener()
    }

    private fun initLiveDataObservers() {

        viewModel.mediatorListResult.removeObservers(viewLifecycleOwner)
        viewModel.mediatorListResult.observe(viewLifecycleOwner, { list->
            taskListAdapter.submitList(list)
        })

        viewModel.getCurrentPoint().observe(viewLifecycleOwner,  {
            with(binding.btsPointList) {
                tvCurrentPointName.text = it.addressName
            }
            setBottomSheetButtonsVisibility(it)
        })

        viewModel.unloadingAvailable.observe(viewLifecycleOwner,{
            uloadingAvailable = it
            requireActivity().invalidateOptionsMenu()
        })
    }

    private fun updateCurrentPointInVM(point: PointItem){
        viewModel.setCurrentPoint(point)
        initBottomSheetActions()
    }

    private fun initViews() {
        with(binding.rvTaskList) {
            layoutManager = LinearLayoutManager(context)
            adapter = taskListAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
        }

        viewModel.setFullList(binding.btsPointList.listSwitcher.isChecked)

        btsBehavior = BottomSheetBehavior.from(binding.btsPointList.bottomSheetRoute)
        setBottomSheetButtonsVisibility(viewModel.getCurrentPoint().value)

        taskListAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                scrollToItem(positionStart)
            }
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                scrollToItem(fromPosition)
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                scrollToItem(positionStart)
            }
        })

    }

    private fun scrollToItem(position: Int) {
        val listCount = taskListAdapter.currentList.count()
        val position_to = if (position >= listCount) 0 else {
            position
        }
        binding.rvTaskList.scrollToPosition(position_to)
        if (taskListAdapter.currentList.isNotEmpty())
            updateCurrentPointInVM(taskListAdapter.currentList[position_to])
        selectItemList()
    }

    private fun selectItemList(){
        if (btsBehavior.state  != BottomSheetBehavior.STATE_COLLAPSED && viewModel.getCurrentPoint().value!=null) {
            with(taskListAdapter) {
                val currentSelected = selectedItem
                selectedItem = viewModel.getCurrentPoint().value
                if (selectedItem != currentSelected) {
                    currentSelected?.let {
                        binding.rvTaskList.post {
                            notifyItemChanged(currentList.indexOf(currentSelected))
                        }
                    }
                    binding.rvTaskList.post {
                        notifyItemChanged(currentList.indexOf(selectedItem))
                    }
                }
            }
        }
    }

    private fun setBottomSheetButtonsVisibility(point: PointItem?){
        if (point == null) {
            btsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            with(binding.btsPointList) {
                //ibtnCannotDone.visibility = if (point.polygon) View.GONE else View.VISIBLE
                ibtnCannotDone.visibility = View.GONE
                ibtnPhotos.visibility = if (point.polygon) View.GONE else View.VISIBLE
                ibtnEdit.visibility = if (point.polygon) View.VISIBLE else View.GONE
                ibtnDelete.visibility = if (point.polygon && !point.done) View.VISIBLE else View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initBottomSheetActions() {
        setBottomSheetButtonsVisibility(viewModel.getCurrentPoint().value)
        with(binding.btsPointList){
            ibtnCanDone.setOnClickListener {
                if (viewModel.getCurrentPoint().value!!.polygon) {
                    val resultPoint = viewModel.getCurrentPoint().value!!.copy(done = true, timestamp = Date(), status = PointStatuses.DONE)
                    viewModel.updateCurrentPoint(resultPoint)
                }else{
                    startNavService()
                    viewModel.getCurrentPoint().value?.let{
                        navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPointFragment(it))
                    }
                }
                btsBehavior.state  = BottomSheetBehavior.STATE_COLLAPSED
            }


            ibtnHome.setOnClickListener {
                navController.navigate(MainNavigationDirections.actionGlobalStartScreenFragment())
            }

            ibtnNavigation.setOnClickListener {
                viewModel.getCurrentPoint().value?.let{
                    buildRoute(it,"2gis")
                }
            }

            ibtnPhotos.setOnClickListener {
                navController.navigate(MainNavigationDirections.actionGlobalPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            ibtnDelete.setOnClickListener {
                viewModel.deletePolygonFromList()
            }

            ibtnEdit.setOnClickListener {
                viewModel.getCurrentPoint().value?.let {
                    if (it.polygon) {
                        navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPolygonListFragment(requestKeyPolygon))
                    }
                }
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
    }

    private fun stopNavService(){
        if (NavigationServiceConnection.isActive()) {
            NavigationServiceConnection.stopTracking()
        }
    }

    private fun buildRoute(point: PointItem, naviApp: String) {
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
    }

    private fun initPolygonSelectionDialogListener() {
        // for Dialog fragments (add polygon)
        childFragmentManager.setFragmentResultListener(
            "polygonResult",
            this
        ) { _, _ ->

        }
        // for Fragment (edit polygon)
        setFragmentResultListener(requestKeyPolygon
        ) { _, bundle ->
            viewModel.getCurrentPoint().value?.let {
                val polygon = bundle.get("polygon") as PolygonItem
                viewModel.updateCurrentPoint(it.copy(polygonUID = polygon.uid, addressName = polygon.name, polygonName = polygon.name))
            }
        }
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