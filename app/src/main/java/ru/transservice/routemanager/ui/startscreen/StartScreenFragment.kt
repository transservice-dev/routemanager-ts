package ru.transservice.routemanager.ui.startscreen

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.animation.AnimateView
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.databinding.FragmentStartScreenBinding
import ru.transservice.routemanager.extensions.shortFormat
import ru.transservice.routemanager.service.BackPressedCallback
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.ui.splashscreen.SplashScreenFragmentDirections

class StartScreenFragment : Fragment() {

    private var _binding: FragmentStartScreenBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: StartScreenViewModel
    private var isBtnCloseHidden = true
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationId: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        initViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, BackPressedCallback.callbackBlock)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartScreenBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        viewModel.getTaskParams().observe(viewLifecycleOwner, Observer {
            with(binding) {
                dateOfRoute.text = it.routeDate.shortFormat()
                vehicleNumber.text = it.vehicle?.number ?: ""
                updateRouteInfo(true)
            }
        })


        viewModel.getUploadResult().observe(requireActivity(), {
            when (it) {
                    is LoadResult.Loading -> {
                        navController.navigate(R.id.splashScreenFragment)
                    }
                    is LoadResult.Success -> {
                        navController.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToStartScreenFragment())
                        with(NotificationManagerCompat.from(requireActivity())) {
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setContentTitle("Выгрузка завершена")
                            notify(notificationId, notificationBuilder.build())
                        }

                    /*Snackbar.make(binding.root,
                            "Данные выгружены успешно!",
                            Snackbar.LENGTH_SHORT
                        ).show()*/
                    }
                    is LoadResult.Error -> {
                        navController.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToStartScreenFragment())
                        with(NotificationManagerCompat.from(requireActivity())) {
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setContentTitle("Данные НЕ выгружены")
                            notify(notificationId, notificationBuilder.build())
                        }
                        Snackbar.make(
                            binding.root,
                            "Ошибка загрузки ${it.errorMessage}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

        })


    }

    private fun initViewModel(){
        viewModel  = ViewModelProvider(this, StartScreenViewModel.StartScreenViewModelFactory()).get(
            StartScreenViewModel::class.java)
    }

    private fun initViews(){
        updateRouteInfo(false)
        binding.closeLayout.visibility = View.GONE
       //isBtnCloseHidden = false
        //showHideButtonClose()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initActionButtons(){
        binding.photoLayout.setOnClickListener {
            /*val test: Boolean? = null
            if (test!!) {
                Toast.makeText(context, "test", Toast.LENGTH_SHORT).show()
            }*/
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToPhotoListFragment(null,PhotoOrder.DONT_SET))
        }

        binding.vehicleLayout.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToRouteSettingsFragment())
        }

        binding.routeGroup.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToTaskListFragment())
        }

        binding.btnLoad.setOnClickListener{
            viewModel.syncTaskData().observe(viewLifecycleOwner, Observer {
                when (it) {
                    is LoadResult.Loading -> {
                        //TODO splash screen loading
                    }
                    is LoadResult.Success -> {
                        viewModel.getTaskParams().value?.let {
                            binding.atAllCount.text = it.countPoint.toString()
                        }
                    }
                    is LoadResult.Error -> {
                        Snackbar.make(binding.root,"Ошибка загрузки ${it.errorMessage}",Snackbar.LENGTH_LONG).show()
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
                setTitle("Вы уверены, что хотите завершить маршрут?")
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
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
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

        @JvmStatic
        fun newInstance() =
            StartScreenFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

}