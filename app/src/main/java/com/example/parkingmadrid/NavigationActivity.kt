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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.example.parkingmadrid.R

class NavigationActivity : AppCompatActivity() {

    private lateinit var madridAPI: MadridAPI
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerSearchCriteria: Spinner
    private lateinit var cardContainer: MaterialCardView
    private lateinit var dataList: List<Any> // Almacena todos los datos de la API

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
    private fun handleResponse(dataList: List<Any>) {
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
    private fun createCardForParking(context: Context, item: Any): MaterialCardView {
        val card = MaterialCardView(context)
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(
            resources.getDimensionPixelSize(R.dimen.card_margin),
            resources.getDimensionPixelSize(R.dimen.card_margin),
            resources.getDimensionPixelSize(R.dimen.card_margin),
            0
        )
        card.layoutParams = layoutParams
        card.cardElevation = resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
        card.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
        card.strokeColor = ContextCompat.getColor(context, R.color.card_stroke_color)

        val padding = resources.getDimensionPixelSize(R.dimen.card_content_padding)

        val textViewName = MaterialTextView(context)
        textViewName.text = when (item) {
            is ParkingInfo -> item.name ?: "No hay información del nombre del parking"
            is ParkingInfoWithoutOccupation -> item.name ?: "No hay información del nombre del parking"
            else -> "Nombre del parking no disponible"
        }
        textViewName.setPadding(padding, padding, padding, padding)
        textViewName.setTextAppearance(android.R.style.TextAppearance_Material_Subhead)
        card.addView(textViewName)

        val textViewAddress = MaterialTextView(context)
        textViewAddress.text = when (item) {
            is ParkingInfo -> item.address ?: "No hay información de la calle"
            is ParkingInfoWithoutOccupation -> item.address ?: "No hay información de la calle"
            else -> "Dirección no disponible"
        }
        textViewAddress.setPadding(padding, 0, padding, padding)
        textViewAddress.setTextAppearance(android.R.style.TextAppearance_Material_Small)
        card.addView(textViewAddress)

        val textViewOccupation = MaterialTextView(context)
        textViewOccupation.text = when (item) {
            is ParkingInfo -> item.occupations?.firstOrNull()?.free.toString() ?: "No hay información de ocupación"
            is ParkingInfoWithoutOccupation -> "No hay información de ocupación pero si"
            else -> "Ocupación no disponible"
        }
        textViewOccupation.setPadding(padding, 0, padding, padding)
        textViewOccupation.setTextAppearance(android.R.style.TextAppearance_Material_Small)
        card.addView(textViewOccupation)

        return card
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
