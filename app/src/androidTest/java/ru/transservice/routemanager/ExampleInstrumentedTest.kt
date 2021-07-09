package ru.transservice.routemanager

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.transservice.routemanager.repositories.RootRepository

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("ru.transservice.routemanager", appContext.packageName)
    }

    @Test
    fun loadRegions(){
        RootRepository.getRegions {
            it.forEach{
                Log.i("TEST", "Region $it")
            }
        }
    }

    @Test
    fun loadVehicles(){
        RootRepository.getAllVehicles {
            it.forEach{
                Log.i("TEST", "Vehicle $it")
            }
        }
    }

    @Test
    fun loadVehicleByRegion(){
        RootRepository.getRegions {
            val region = it.filter { it.uid == "a702bc3c-6cdb-11e1-ad52-0080483a9890" }.lastOrNull()
            if (region != null){
                RootRepository.getVehicleByRegion(region){
                    it.forEach{
                        Log.i("TEST", "Vehicle $it")
                    }
                }
            }
        }

    }
}