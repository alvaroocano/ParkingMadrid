package com.example.parkingmadrid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private lateinit var sharedPref: SharedPreferences
    private var favoriteParkings: MutableList<ParkingInfo> = mutableListOf()
    private val CHANNEL_ID = "favoritos_channel"
    private val notificationId = 1234
    private val CHANNEL_NAME = "Favoritos"
    private val CHANNEL_DESCRIPTION = "Notificaciones de guardado en favoritos"
    private val PERMISSION_REQUEST_CODE = 1001



    private val PREFS_NAME = "favorite_parkings_"
    private val API_LOADED_KEY = "api_loaded"

    private lateinit var userId: String
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

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
        val navHeaderImage = headerView.findViewById<ImageView>(R.id.nav_header_image)
        val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
        val themeToggle = headerView.findViewById<ImageView>(R.id.theme_toggle)

        loadUserDetails(navHeaderName)

        val currentUser: FirebaseUser? = mAuth.currentUser
        currentUser?.let {
            userId = it.uid
            val email = it.email ?: "Correo no disponible"
            navHeaderEmail.text = email

                    it.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.defaultuser) // Un drawable de recurso placeholder
                    .into(navHeaderImage)
            } ?: navHeaderImage.setImageResource(R.drawable.defaultuser)
        }

        themeToggle.setOnClickListener {
            toggleNightMode()
        }

        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria)
        cardContainer = findViewById(R.id.cardContainer)
        progressBar = findViewById(R.id.progressBar)

        madridAPI = retrofit.create(MadridAPI::class.java)

        sharedPref = getSharedPreferences("$PREFS_NAME$userId", Context.MODE_PRIVATE)
        favoriteParkings = loadFavoritesFromPreferences()

        val apiLoaded = sharedPref.getBoolean(API_LOADED_KEY, false)

        if (!apiLoaded) {
            fetchData()
            val editor = sharedPref.edit()
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
                R.id.nav_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_map -> { // Añadir un nuevo item en el menú para el mapa
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onResume() {
        super.onResume()
        favoriteParkings = loadFavoritesFromPreferences()
        handleResponse(dataList)
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
                    Toast.makeText(this@NavigationActivity, "Error al obtener los datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ParkingInfo>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@NavigationActivity, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveDataToCache(dataList: List<ParkingInfo>) {
        try {
            val cacheFile = File(cacheDir, "parking_data_$userId")
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
        val cacheFile = File(cacheDir, "parking_data_$userId")
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

        if (favoriteParkings.contains(item)) {
            favoriteButton.setImageResource(R.drawable.estrella2)
            isFavorite = true
        } else {
            favoriteButton.setImageResource(R.drawable.estrella)
            isFavorite = false
        }

        favoriteButton.setOnClickListener {
            isFavorite = !isFavorite
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.estrella2)
                addFavorite(item)
            } else {
                favoriteButton.setImageResource(R.drawable.estrella)
                removeFavorite(item)
            }
        }

        return cardView
    }


    private fun addFavorite(item: ParkingInfo) {
        if (!favoriteParkings.contains(item)) {
            favoriteParkings.add(item)
            saveFavoritesToPreferences()
            showNotification(this,"Se ha guardado en favoritos el parking ${item.name}")
        }
    }

    private fun removeFavorite(item: ParkingInfo) {
        if (favoriteParkings.contains(item)) {
            favoriteParkings.remove(item)
            saveFavoritesToPreferences()
        }
    }

    private fun showNotification(context: Context, message: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icono2)
                .setContentTitle("Favoritos")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notificationBuilder.build())
            }
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun saveFavoritesToPreferences() {
        val sharedPref = getSharedPreferences("$PREFS_NAME$userId", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val jsonString = Gson().toJson(favoriteParkings)
        editor.putString("favorite_parkings_$userId", jsonString)
        editor.apply()
    }

    private fun loadFavoritesFromPreferences(): MutableList<ParkingInfo> {
        val sharedPref = getSharedPreferences("$PREFS_NAME$userId", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("favorite_parkings_$userId", "")
        if (jsonString.isNullOrEmpty()) return mutableListOf()

        val type = object : TypeToken<List<ParkingInfo>>() {}.type
        val favoriteList: List<ParkingInfo> = Gson().fromJson(jsonString, type)
        return favoriteList.toMutableList()
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

            R.id.nav_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.nav_nearby_parking -> {
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.nav_logout -> {
                showLogoutConfirmationDialog()
                return true
            }

            else -> {
                return false
            }
        }
    }

    private fun loadUserDetails(navUsername: TextView) {
        val currentUser = mAuth.currentUser
        currentUser?.let {
            navUsername.text = it.displayName

            database.reference.child("users").child(it.uid).child("username").get()
                .addOnSuccessListener { snapshot ->
                    val nickname = snapshot.getValue(String::class.java)
                    if (!nickname.isNullOrEmpty()) {
                        navUsername.text =
                            nickname
                    }
                }.addOnFailureListener {

                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FAVORITES && resultCode == RESULT_OK) {
            favoriteParkings = loadFavoritesFromPreferences()
            updateFavoriteIcons()
        }
    }

    private fun updateFavoriteIcons() {
        cardContainer.removeAllViews()
        handleResponse(dataList)
    }

    companion object {
        const val REQUEST_CODE_FAVORITES = 1
    }

}
