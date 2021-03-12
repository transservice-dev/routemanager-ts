package ru.transservice.routemanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.navigation.NavController
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.databinding.ActivityMainBinding
import ru.transservice.routemanager.repositories.RootRepository
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}