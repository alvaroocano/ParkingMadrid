package com.example.parkingmadrid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.example.parkingmadrid.Clases.ParkingInfoWithoutOccupation
import com.facebook.login.LoginManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NavigationActivity : AppCompatActivity() {

    private lateinit var madridAPI: MadridAPI
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        textView = findViewById(R.id.mostrarApi) // Reemplaza "textView" con el ID correcto de tu TextView

        madridAPI = retrofit.create(MadridAPI::class.java)

        val buttonLogoutFacebook = findViewById<Button>(R.id.buttonLogoutFacebook)
        buttonLogoutFacebook.setOnClickListener {
            signOutFromFacebook()
        }

        val call = madridAPI.getParkingInfo("ES")
        call.enqueue(object : Callback<List<ParkingInfo>> {
            override fun onResponse(call: Call<List<ParkingInfo>>, response: Response<List<ParkingInfo>>) {
                if (response.isSuccessful) {
                    val parkingInfoList = response.body()
                    handleResponse(parkingInfoList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: Call<List<ParkingInfo>>, t: Throwable) {
                // Maneja el fallo de la solicitud aquí
            }
        })

        val callWithoutOccupation = madridAPI.getParkingInfoWithoutOccupation("ES")
        callWithoutOccupation.enqueue(object : Callback<List<ParkingInfoWithoutOccupation>> {
            override fun onResponse(call: Call<List<ParkingInfoWithoutOccupation>>, response: Response<List<ParkingInfoWithoutOccupation>>) {
                if (response.isSuccessful) {
                    val parkingInfoWithoutOccupationList = response.body()
                    handleResponse(parkingInfoWithoutOccupationList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: Call<List<ParkingInfoWithoutOccupation>>, t: Throwable) {
                // Maneja el fallo de la solicitud aquí
            }
        })
    }

    // Método para manejar la respuesta y mostrar los datos en el TextView
    private fun handleResponse(dataList: List<Any>?) {
        val stringBuilder = StringBuilder()
        dataList?.forEach { item ->
            stringBuilder.append(item.toString()) // Aquí puedes personalizar cómo quieres mostrar cada elemento
            stringBuilder.append("\n") // Agrega un salto de línea después de cada elemento
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
