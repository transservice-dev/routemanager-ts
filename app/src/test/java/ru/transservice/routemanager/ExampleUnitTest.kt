package ru.transservice.routemanager

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

import org.junit.Assert.*
import ru.transservice.routemanager.repositories.RootRepository

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    class test1() {
        @ExperimentalCoroutinesApi
        @Test
        fun coroutinesTest() = runBlockingTest{

            println("start")
            val scope = CoroutineScope(Job() + Dispatchers.IO)

            scope.launch {
                test1()
                test2()
                delay(5000)
            }
            println("stop")
        }


        suspend fun test1(){
            delay(1000)
            println( "test1 is done")
        }

        suspend fun test2(){
            delay(200)
            println( "test2 is done")
            //Log.d("test2", "test2 is done")
        }
    }




}