package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.facebook.login.LoginManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AlertDialog

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
            override fun onResponse(call: Call<List<ParkingInfo>>, response: Response<List<ParkingInfo>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    dataList = response.body() ?: emptyList()
                    handleResponse(dataList)
                } else {
                    // Maneja la respuesta no exitosa aquí
                }
            }

            override fun onFailure(call: Call<List<ParkingInfo>>, t: Throwable) {
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

    private fun createCardForParking(context: Context, item: ParkingInfo): View {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.card_layout, null) as MaterialCardView

        val titleTextView = cardView.findViewById<TextView>(R.id.tittle)
        val secondaryTextView = cardView.findViewById<TextView>(R.id.seccondary)
        val supportingTextView = cardView.findViewById<TextView>(R.id.supporting)
        val buttonGoTo = cardView.findViewById<MaterialButton>(R.id.button_go_to)

        // Llena los campos con los datos del ParkingInfo
        titleTextView.text = item.name ?: "Nombre no disponible"
        secondaryTextView.text = item.address ?: "Dirección no disponible"
        val occupationText = item.occupations?.firstOrNull()?.free?.let { freeSpaces ->
            "Sitios libres: $freeSpaces"
        } ?: "Ocupación no disponible"
        supportingTextView.text = occupationText

        // Configura bordes y sombras
        cardView.apply {
            cardElevation = 8f
            radius = 16f
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke_color)
        }

        // Configura el botón "Ir a" para abrir Google Maps con las coordenadas
        buttonGoTo.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:${item.latitude},${item.longitude}?q=parking ${item.address}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }

        return cardView
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        showLogoutConfirmationDialog()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro de que desea cerrar sesión?")
            .setPositiveButton("Sí") { dialog, which ->
                logoutAndRedirectToLogin()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Dismiss the dialog and stay in the activity
            }
            .show()
    }

    private fun logoutAndRedirectToLogin() {
        // Código para cerrar la sesión del usuario.
        // Por ejemplo:
        // val sharedPreferences = getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE)
        // sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Método para cerrar sesión en Facebook
    private fun signOutFromFacebook() {
        // Cerrar sesión con Facebook
        LoginManager.getInstance().logOut()

        // Redirigir a la pantalla de inicio de sesión o a donde corresponda en tu aplicación
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
