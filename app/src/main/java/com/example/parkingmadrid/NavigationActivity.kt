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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import retrofit2.Callback
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.example.parkingmadrid.R

class NavigationActivity : AppCompatActivity() {

    private lateinit var madridAPI: MadridAPI
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerSearchCriteria: Spinner
    private lateinit var cardContainer: MaterialCardView
    private lateinit var dataList: List<ParkingInfo> // Almacena todos los datos de la API

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria)
        cardContainer = findViewById(R.id.card)

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

    // Método para manejar la respuesta y mostrar los datos en tarjetas (cards)
    private fun handleResponse(dataList: List<ParkingInfo>) {
        cardContainer.removeAllViews() // Limpiar las tarjetas existentes antes de agregar nuevas

        var topMargin = 0 // Margen superior para el primer card

        dataList.forEachIndexed { index, item ->
            val card = createCardForParking(this, item)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, topMargin, 0, 0) // Establecer los márgenes
            card.layoutParams = layoutParams
            cardContainer.addView(card)

            // Actualizar el margen superior para el siguiente card
            topMargin += resources.getDimensionPixelSize(R.dimen.card_height)
        }
    }


    // Método para crear una tarjeta (card) para un elemento de parking
    private fun createCardForParking(context: Context, item: ParkingInfo): View {
        val cardView = findViewById<MaterialCardView>(R.id.card)

        val titleTextView = cardView.findViewById<TextView>(R.id.tittle)
        val secondaryTextView = cardView.findViewById<TextView>(R.id.seccondary)
        val supportingTextView = cardView.findViewById<TextView>(R.id.supporting)

        // Llena los campos con los datos del ParkingInfo
        titleTextView.text = item.name ?: "Nombre no disponible"
        secondaryTextView.text = item.address ?: "Dirección no disponible"
        supportingTextView.text = (item.occupations ?: "Ocupación no disponible").toString()

        return cardView
    }

//    private fun populateCards(dataList: List<ParkingInfo>) {
//        for (item in dataList) {
//            val cardView = createCardForParking(this, item)
//            // Agrega la tarjeta al contenedor adecuado en tu diseño
//            cardContainer.addView(cardView)
//        }
//    }



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
