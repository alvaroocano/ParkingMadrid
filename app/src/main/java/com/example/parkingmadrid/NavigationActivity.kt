package com.example.parkingmadrid

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import androidx.core.view.isVisible
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
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.example.parkingmadrid.R
import retrofit2.Response

class NavigationActivity : AppCompatActivity() {

    private lateinit var madridAPI: MadridAPI
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerSearchCriteria: Spinner
    private lateinit var cardContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var dataList: List<ParkingInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        //setSupportActionBar(toolbar)

        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria)
        cardContainer = findViewById(R.id.cardContainer)
        progressBar = findViewById(R.id.progressBar)

        madridAPI = retrofit.create(MadridAPI::class.java)

        fetchData()

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchCriteria = spinnerSearchCriteria.selectedItem.toString()
                val searchText = s.toString()
                performSearch(searchCriteria, searchText)
            }
        })

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search -> {
                    toggleSearchVisibility()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleSearchVisibility() {
        val isVisible = editTextSearch.isVisible
        editTextSearch.isVisible = !isVisible
        spinnerSearchCriteria.isVisible = !isVisible
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        val call = madridAPI.getParkingInfo("ES")
        call.enqueue(object : Callback<List<ParkingInfo>> {
            override fun onResponse(call: retrofit2.Call<List<ParkingInfo>>, response: Response<List<ParkingInfo>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    dataList = response.body() ?: emptyList()
                    handleResponse(dataList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: retrofit2.Call<List<ParkingInfo>>, t: Throwable) {
                progressBar.visibility = View.GONE
                // Maneja el fallo de la solicitud aquí
            }
        })
    }

    private fun performSearch(criteria: String, searchText: String) {
        val filteredList = when (criteria) {
            "Nombre" -> dataList.filter { it.name?.contains(searchText, ignoreCase = true) == true }
            "Calle" -> dataList.filter { it.address?.contains(searchText, ignoreCase = true) == true }
            else -> emptyList()
        }
        handleResponse(filteredList)
    }

    private fun handleResponse(dataList: List<ParkingInfo>) {
        cardContainer.removeAllViews()
        dataList.forEach { item ->
            val card = createCardForParking(this, item)
            cardContainer.addView(card)
        }
    }


    // Método para crear una tarjeta (card) para un elemento de parking
    private fun createCardForParking(context: Context, item: ParkingInfo): View {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.card_layout, null) as MaterialCardView

        val titleTextView = cardView.findViewById<TextView>(R.id.tittle)
        val secondaryTextView = cardView.findViewById<TextView>(R.id.seccondary)
        val supportingTextView = cardView.findViewById<TextView>(R.id.supporting)

        // Llena los campos con los datos del ParkingInfo
        titleTextView.text = item.name ?: "Nombre no disponible"
        secondaryTextView.text = item.address ?: "Dirección no disponible"
        // Formatea y asigna el texto para la ocupación
        val occupationText = item.occupations?.firstOrNull()?.free?.let { freeSpaces ->
            "Sitios libres: $freeSpaces"
        } ?: "Ocupación no disponible"

        supportingTextView.text = occupationText

        // Configura bordes y sombras
        cardView.apply {
            cardElevation = 8f // Elevación para la sombra
            radius = 16f // Radio de las esquinas
            strokeWidth = 2 // Ancho del borde
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke_color) // Color del borde
        }

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
