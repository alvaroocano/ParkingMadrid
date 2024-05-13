package com.example.parkingmadrid

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.example.parkingmadrid.Clases.ParkingInfoWithoutOccupation
import com.facebook.login.LoginManager
import retrofit2.Callback

class NavigationActivity : AppCompatActivity() {

    private lateinit var madridAPI: MadridAPI
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        textView = findViewById(R.id.mostrarApi) // Reemplaza "textView" con el ID correcto de tu TextView

        madridAPI = retrofit.create(MadridAPI::class.java)

        val call = madridAPI.getParkingInfo("ES")
        call.enqueue(object : Callback<List<ParkingInfo>> {
            override fun onResponse(call: retrofit2.Call<List<ParkingInfo>>, response: retrofit2.Response<List<ParkingInfo>>) {
                if (response.isSuccessful) {
                    val parkingInfoList = response.body()
                    handleResponse(parkingInfoList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: retrofit2.Call<List<ParkingInfo>>, t: Throwable) {
                // Maneja el fallo de la solicitud aquí
            }
        })

        val callWithoutOccupation = madridAPI.getParkingInfoWithoutOccupation("ES")
        callWithoutOccupation.enqueue(object : Callback<List<ParkingInfoWithoutOccupation>> {
            override fun onResponse(call: retrofit2.Call<List<ParkingInfoWithoutOccupation>>, response: retrofit2.Response<List<ParkingInfoWithoutOccupation>>) {
                if (response.isSuccessful) {
                    val parkingInfoWithoutOccupationList = response.body()
                    handleResponse(parkingInfoWithoutOccupationList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: retrofit2.Call<List<ParkingInfoWithoutOccupation>>, t: Throwable) {
                // Maneja el fallo de la solicitud aquí
            }
        })
    }

    // Método para manejar la respuesta y mostrar los datos en el TextView
    private fun <T : Any> handleResponse(dataList: List<T>?) {
        val stringBuilder = StringBuilder()

        dataList?.forEachIndexed { index, item ->
            val name: String = when (item) {
                is ParkingInfo -> item.name ?: "No hay información del nombre del parking"
                is ParkingInfoWithoutOccupation -> item.name ?: "No hay información del nombre del parking"
                else -> "Nombre del parking no disponible"
            }

            val address: String = when (item) {
                is ParkingInfo -> item.address ?: "No hay información de la calle"
                is ParkingInfoWithoutOccupation -> item.address ?: "No hay información de la calle"
                else -> "Dirección no disponible"
            }

            val occupation: String = when (item) {
                is ParkingInfo -> item.occupations?.firstOrNull()?.free.toString() ?: "No hay información de ocupación"
                is ParkingInfoWithoutOccupation -> "No hay información de ocupación pero si"
                else -> "Ocupación no disponible"
            }

            stringBuilder.append("Elemento ${index + 1}:\n")
            stringBuilder.append("Calle: $address\n")
            stringBuilder.append("Nombre del parking: $name\n")
            stringBuilder.append("Ocupación: $occupation\n\n")
        }

        val combinedData = stringBuilder.toString()
        textView.text = combinedData
    }




    // Método para cerrar sesión en Facebook
    private fun signOutFromFacebook() {
        // Cerrar sesión con Facebook
        LoginManager.getInstance().logOut()

        // Redirigir a la pantalla de inicio de sesión o a donde corresponda en tu aplicación
        // Por ejemplo:
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finalizar la actividad actual si no se desea volver atrás
    }
}
