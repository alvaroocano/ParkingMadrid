package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class NavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var madridAPI: MadridAPI
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerSearchCriteria: Spinner
    private lateinit var cardContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private var dataList: List<ParkingInfo> = emptyList()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var isFavorite = false
    private lateinit var mAuth: FirebaseAuth

    private val PREFS_NAME = "MyPrefsFile"
    private val API_LOADED_KEY = "api_loaded"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        mAuth = FirebaseAuth.getInstance()

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navigationView.getHeaderView(0)
        val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
        val themeToggle = headerView.findViewById<ImageView>(R.id.theme_toggle)

        navHeaderName.text = "Nombre del Usuario"
        navHeaderEmail.text = "usuario@correo.com"

        themeToggle.setOnClickListener {
            toggleNightMode()
        }

        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria)
        cardContainer = findViewById(R.id.cardContainer)
        progressBar = findViewById(R.id.progressBar)

        madridAPI = retrofit.create(MadridAPI::class.java)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiLoaded = prefs.getBoolean(API_LOADED_KEY, false)

        if (!apiLoaded) {
            fetchData()
            val editor = prefs.edit()
            editor.putBoolean(API_LOADED_KEY, true)
            editor.apply()
        } else {
            dataList = loadDataFromCache()
            if (dataList.isNotEmpty()) {
                handleResponse(dataList)
            }
        }

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

    private fun toggleNightMode() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
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


                    saveDataToCache(dataList)
                } else {

                }
            }

            override fun onFailure(call: Call<List<ParkingInfo>>, t: Throwable) {
                progressBar.visibility = View.GONE
                // Maneja el fallo de la solicitud aquí
            }
        })
    }

    private fun saveDataToCache(dataList: List<ParkingInfo>) {
        try {
            val cacheFile = File(cacheDir, "parking_data")
            val fos = FileOutputStream(cacheFile)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(dataList)
            oos.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadDataFromCache(): List<ParkingInfo> {
        val cacheFile = File(cacheDir, "parking_data")
        if (!cacheFile.exists()) {
            Toast.makeText(this, "Archivo de caché no encontrado", Toast.LENGTH_SHORT).show()
            return emptyList()
        }

        return try {
            val fis = FileInputStream(cacheFile)
            val ois = ObjectInputStream(fis)
            val dataList = ois.readObject() as? List<ParkingInfo>
            ois.close()
            fis.close()

            if (dataList == null) {
                Toast.makeText(this, "Error al leer la caché: dataList es nulo", Toast.LENGTH_SHORT).show()
                emptyList()
            } else {
                dataList
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al leer la caché: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            emptyList()
        }
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
        val favoriteButton = cardView.findViewById<ImageButton>(R.id.button_favorite)

        titleTextView.text = item.name ?: "Nombre no disponible"
        secondaryTextView.text = item.address ?: "Dirección no disponible"
        val occupationText = item.occupations?.firstOrNull()?.free?.let { freeSpaces ->
            "Sitios libres: $freeSpaces"
        } ?: "Ocupación no disponible"
        supportingTextView.text = occupationText

        cardView.apply {
            cardElevation = 8f
            radius = 16f
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke_color)
        }

        buttonGoTo.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:${item.latitude},${item.longitude}?q=parking ${item.address}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }
        favoriteButton.setImageResource(R.drawable.estrella)
        favoriteButton.setOnClickListener {
            isFavorite = !isFavorite
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.estrella)
                // Realiza la lógica para marcar como favorito
            } else {
                favoriteButton.setImageResource(R.drawable.estrella2)
                // Realiza la lógica para desmarcar como favorito
            }
        }

        return cardView
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro de que desea cerrar sesión?")
            .setPositiveButton("Sí") { dialog, which ->
                logoutAndRedirectToLogin()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logoutAndRedirectToLogin() {
        mAuth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return true
            }
            R.id.nav_logout -> {
                showLogoutConfirmationDialog()
                return true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

}
