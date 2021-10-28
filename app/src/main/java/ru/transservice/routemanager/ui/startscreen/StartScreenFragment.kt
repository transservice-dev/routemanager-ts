package ru.transservice.routemanager.ui.startscreen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.animation.AnimateView
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.SearchType
import ru.transservice.routemanager.databinding.FragmentStartScreenBinding
import ru.transservice.routemanager.extensions.shortFormat
import ru.transservice.routemanager.service.ErrorAlert
import ru.transservice.routemanager.service.LoadResult
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class StartScreenFragment : Fragment() {

    private var _binding: FragmentStartScreenBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: StartScreenViewModel
    private var isBtnCloseHidden = true
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationId: Int = 100
    private var backPressedTime:Long = 0

    private val callbackExit = object : OnBackPressedCallback(true) {
        lateinit var backToast:Toast
        @SuppressLint("ShowToast")
        override fun handleOnBackPressed() {
            backToast = Toast.makeText(AppClass.appliactionContext(), "Нажмите еще раз для выхода из приложения.", Toast.LENGTH_LONG)
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel()
                requireActivity().moveTaskToBack(true)
                requireActivity().finish()
            } else {
                backToast.show()
            }
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //Log.d(TAG, "onAttach ${this::class.java}")
        requireActivity().onBackPressedDispatcher.addCallback(this, callbackExit)
    }

    override fun onResume() {
        super.onResume()
        //Log.d(TAG, "onResume ${this::class.java}")
        //viewModel.updateTaskParams()
        requireActivity().onBackPressedDispatcher.addCallback(this, callbackExit)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Log.d(TAG, "onCreateView ${this::class.java}")
        _binding = FragmentStartScreenBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        //Log.d(TAG, "onDestroyView ${this::class.java}")
        _binding = null
        Log.d(TAG, "onDestroyView")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        (requireActivity() as MainActivity).navMenu.visibility = View.VISIBLE
        (requireActivity() as MainActivity).supportActionBar?.show()

        initViews()
        initActionButtons()
        initNotificationManager()
        Log.d(TAG, "OnViewCreated")

        viewModel.getTaskParams().observe(viewLifecycleOwner, {
            with(binding) {
                dateOfRoute.text = it.routeDate.shortFormat()
                vehicleNumber.text = if (it.search_type == SearchType.BY_VEHICLE) it.vehicle?.number ?: "" else it.route?.name ?: ""
                updateRouteInfo(true)
            }
        })

        viewModel.getUploadResult().observe(requireActivity(), {
            Log.d(TAG, "start observing getUploadResult ${this::class.java}")
            when (it) {
                    is LoadResult.Loading -> {
                        navController.navigate(R.id.splashScreenFragment)
                    }
                    is LoadResult.Success -> {
                        navController.navigate(R.id.startScreenFragment)
                        with(NotificationManagerCompat.from(requireActivity())) {
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setContentTitle("Выгрузка завершена")
                            notify(notificationId, notificationBuilder.build())
                        }
                        Toast.makeText(context, "Данные выгружены успешно!",Toast.LENGTH_LONG).show()
                        //viewModel.updateCurrentTask()
                    }
                    is LoadResult.Error -> {
                        navController.navigate(R.id.startScreenFragment)
                        with(NotificationManagerCompat.from(requireActivity())) {
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setContentTitle("Данные НЕ выгружены")
                            notify(notificationId, notificationBuilder.build())
                        }
                        val errorDescription = errorDescription(it)
                        ErrorAlert.showAlert("$errorDescription Отправить отчет об ошибке?", requireContext())
                        Toast.makeText(context, "Ошибка загрузки ${it.errorMessage}",Toast.LENGTH_LONG).show()
                    }
                }
        })
    }

    private fun <T>errorDescription(loadResult: LoadResult<T>): String {
        val errorDescription = when (loadResult.e) {
            is UnknownHostException -> "Ошибка: неизвестное имя сервера. Проверьте наличие интернета на устройстве."
            is SocketTimeoutException -> "Ошибка соединения. Сервер не отвечает. Проверьте наличие интернета на устройстве."
            is SecurityException -> "Ошибка авторизации. Проверьте правильность ввода пароля."
            else -> "При выгрузке/загрузке данных произошла ошибка."
        }
        return errorDescription
    }

    private fun initViewModel(){
        viewModel  = ViewModelProvider(this, StartScreenViewModel.StartScreenViewModelFactory()).get(
            StartScreenViewModel::class.java)
    }

    private fun initViews(){
        updateRouteInfo(false)
        binding.closeLayout.visibility = View.GONE
        binding.tvVersion.text = "Версия ${viewModel.version}"
       //isBtnCloseHidden = false
        //showHideButtonClose()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initActionButtons(){
        binding.photoLayout.setOnClickListener {
            //throw IllegalArgumentException()
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToPhotoListFragment(null,PhotoOrder.DONT_SET))
        }

        binding.vehicleLayout.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToRouteSettingsFragment())
        }

        binding.routeGroup.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToTaskListFragment())
        }

        binding.btnLoad.setOnClickListener{
            viewModel.syncTaskData().observe(viewLifecycleOwner, {
                when (it) {
                    is LoadResult.Loading -> {
                        (requireActivity() as MainActivity).swipeLayout.isRefreshing = true
                    }
                    is LoadResult.Success -> {
                        (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                        Toast.makeText(context, "Добавлено новых строк: ${it.data} ",Toast.LENGTH_LONG).show()
                        //Snackbar.make(binding.root,"Добавлено новых строк: ${it.data} ",Snackbar.LENGTH_LONG).show()
                        viewModel.getTaskParams().value?.let { task->
                            binding.atAllCount.text = task.countPoint.toString()
                        }
                    }
                    is LoadResult.Error -> {
                        (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                        if (it.e != null) {
                            val errorDescription = errorDescription(it)
                            ErrorAlert.showAlert(
                                "$errorDescription Отправить отчет об ошибке?",
                                requireContext()
                            )
                        }
                        Toast.makeText(context, "Ошибка загрузки ${it.errorMessage}",Toast.LENGTH_LONG).show()
                        //Snackbar.make(binding.root,"Ошибка загрузки ${it.errorMessage}",Snackbar.LENGTH_LONG).show()
                    }
                }
            })
        }

        binding.imageOpenCloseRoute.setOnClickListener {
            showHideButtonClose()
        }

        binding.btnFinishRoute.setOnClickListener {
            val alertBuilder = AlertDialog.Builder(context).apply {
                setTitle("Подтвердите действие")
                setMessage("Вы уверены, что хотите завершить маршрут?")
                setPositiveButton("Да, заврешить") { _,_ ->
                    with(NotificationManagerCompat.from(requireActivity())) {
                        notificationBuilder.setProgress(100, 0, true)
                        notify(notificationId, notificationBuilder.build())
                    }
                    viewModel.finishRoute()
                }
                setNegativeButton("Нет, отменить") { _,_ ->
                    showHideButtonClose()
                    Toast.makeText(context, "Действие отменено пользователем",Toast.LENGTH_SHORT).show()
                }
            }
            val alert = alertBuilder.create()
            alert.show()
        }

    }

    private fun initNotificationManager(){
        val intent = Intent(requireActivity(), MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            requireActivity(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        //создание builder'а (что будет отображаться)
        notificationBuilder = NotificationCompat.Builder(requireActivity(), (requireActivity() as MainActivity).getNotificationChannel().id)
            .setContentTitle("Выгрузка данных")
            .setContentText("Выгрузка данных маршрута и фотографий")
            .setSmallIcon(R.drawable.ic_logo_mini)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    private fun updateRouteInfo(animate: Boolean){
        val task = viewModel.getTaskParams().value
        val routeIsLoaded = task?.docUid != ""
        if (routeIsLoaded) {
            /*if (animate){
                val animation = AnimateView(binding.routeGroup, requireContext(), animate)
                animation.showHeight()
            }else{
               binding.routeGroup.visibility = View.VISIBLE
            }*/
            binding.routeGroup.visibility = View.VISIBLE
            binding.btnLoad.setImageResource(R.drawable.ic_replay_24)
        }else{
            /*if (animate) {
                val animation = AnimateView(binding.routeGroup, requireContext(), animate)
                animation.hideHeight()
            }else{
                binding.routeGroup.visibility = View.GONE
            }*/
            binding.routeGroup.visibility = View.GONE
            binding.btnLoad.setImageResource(R.drawable.ic_add_24)
        }
        with(binding){
            atAllCount.text = task?.countPoint.toString()
            doneCount.text = task?.countPointDone.toString()
        }
    }

    private fun showHideButtonClose(){
        with(binding.closeLayout){
            val animateView = this.context?.let {
                context -> AnimateView(this, context, true)
            }
            if (!isBtnCloseHidden) {
                animateView!!.hideHeight()
            }else{
                animateView!!.showHeight()
            }

        }

        with(binding.imageOpenCloseRoute) {
            val animateView = this.context?.let {
                context -> AnimateView(this, context, true)
            }

            if (!isBtnCloseHidden) {
                animateView!!.rotateBack()
            }else{
                animateView!!.rotate()
            }
        }

        isBtnCloseHidden = !isBtnCloseHidden
    }

    companion object {

        private const val TAG = "${AppClass.TAG}: StartScreenFragment"

        @JvmStatic
        fun newInstance() =
            StartScreenFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

}