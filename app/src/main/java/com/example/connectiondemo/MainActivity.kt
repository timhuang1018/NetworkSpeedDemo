package com.example.connectiondemo

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //see ref: https://github.com/bertrandmartel/speed-test-lib/
        val socket = SpeedTestSocket()
        socket.addSpeedTestListener(object :ISpeedTestListener{
            override fun onCompletion(report: SpeedTestReport?) {
                Log.d("SpeedTestSocket","onCompletion,rateBit:${(report?.transferRateBit?.toLong() ?: 0L)  / 1024} kbps")
            }

            override fun onProgress(percent: Float, report: SpeedTestReport?) {
                Log.d("SpeedTestSocket","onProgress:${percent},rateBit:${(report?.transferRateBit?.toLong() ?: 0L)  / 1024} kbps")
            }

            override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                Log.e("SpeedTestSocket","error on:${speedTestError},${errorMessage}")
            }
        })

        Thread{
            //upload filename could be 1M, 10M, 100M to change file size
            //set report interval as 1.5 sec per (seems only work for upload)
//            socket.startDownload("http://ipv4.ikoula.testdebit.info/10M.iso",1500)
            //uncomment to test upload, second parameter is file size
//                    socket.startUpload("http://ipv4.ikoula.testdebit.info/",10000000 ,     1500)
        }.start()


    }


    //use android api to get network config: connection type, assumed download and upload kbps
    private fun getNetworkConfig(){
        val cm = ContextCompat.getSystemService(this,ConnectivityManager::class.java)
        //api >= 21
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            cm?.registerNetworkCallback(NetworkRequest.Builder().build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        Log.e("callback","onAvailable, network id:$network")
                    }
                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Log.e("callback","onLost, network:$network")
                    }
                })

            Log.e("networks","${cm?.allNetworks?.toList()}")
            cm?.allNetworks?.forEach {
                val cap = cm.getNetworkCapabilities(it)

                Log.e("network status","""
                network:${it}, type:${getNetworkType(cap)}
                download:${cap?.linkDownstreamBandwidthKbps}kbps
                upload:${cap?.linkUpstreamBandwidthKbps}kbps
            """.trimIndent())
            }
        }else{
            //api below 21
                val info = cm?.activeNetworkInfo
            if(info != null && info.isConnected){
                //有網路連線
                Log.e("network state","type:${info.type}")
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getNetworkType(cap: NetworkCapabilities?): String {
        return when{
            cap == null -> ""
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> ""
        }
    }
}