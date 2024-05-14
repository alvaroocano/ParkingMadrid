package com.example.parkingmadrid
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Spinner
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
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerSearchCriteria: Spinner
    private lateinit var dataList: List<Any> // Almacena todos los datos de la API

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        textView = findViewById(R.id.mostrarApi)
        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria)

        madridAPI = retrofit.create(MadridAPI::class.java)

        // Obtener los datos de la API y almacenarlos
        fetchData()

        // Manejar el cambio de texto en el EditText para el filtrado automático
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchCriteria = spinnerSearchCriteria.selectedItem.toString()
                val searchText = s.toString()
                performSearch(searchCriteria, searchText)
            }
        })
    }

    // Método para obtener los datos de la API y almacenarlos
    private fun fetchData() {
        val call = madridAPI.getParkingInfo("ES")
        call.enqueue(object : Callback<List<ParkingInfo>> {
            override fun onResponse(call: retrofit2.Call<List<ParkingInfo>>, response: retrofit2.Response<List<ParkingInfo>>) {
                if (response.isSuccessful) {
                    dataList = response.body() ?: emptyList()
                    handleResponse(dataList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: retrofit2.Call<List<ParkingInfo>>, t: Throwable) {
                // Maneja el fallo de la solicitud aquí
            }
        })
    }

    // Método para realizar el filtrado y mostrar los resultados
    private fun performSearch(criteria: String, searchText: String) {
        val filteredList = when (criteria) {
            "Nombre" -> dataList.filterIsInstance<ParkingInfo>().filter { it.name?.contains(searchText, ignoreCase = true) ?: false }
            "Calle" -> dataList.filterIsInstance<ParkingInfo>().filter { it.address?.contains(searchText, ignoreCase = true) ?: false }
            else -> emptyList() // Manejar criterio desconocido o por defecto
        }
        handleResponse(filteredList)
    }

    // Método para manejar la respuesta y mostrar los datos en el TextView
    private fun handleResponse(dataList: List<Any>) {
        val stringBuilder = StringBuilder()

        dataList.forEachIndexed { index, item ->
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

