package com.example.parkingmadrid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.parkingmadrid.Clases.ParkingInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesContainer: LinearLayout
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userId: String

    private val PREFS_NAME = "favorite_parkings_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        favoritesContainer = findViewById(R.id.favoritesContainer)

        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid.orEmpty()

        val favorites = loadFavoritesFromPreferences()
        handleResponse(favorites)
    }

    override fun onResume() {
        super.onResume()
        val favorites = loadFavoritesFromPreferences()
        handleResponse(favorites)
    }


    private fun loadFavoritesFromPreferences(): List<ParkingInfo> {
        val sharedPref = getSharedPreferences("$PREFS_NAME$userId", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("favorite_parkings_$userId", "")
        if (jsonString.isNullOrEmpty()) return emptyList()

        val type = object : TypeToken<List<ParkingInfo>>() {}.type
        return Gson().fromJson(jsonString, type)
    }


    private fun handleResponse(favorites: List<ParkingInfo>) {
        favoritesContainer.removeAllViews()
        favorites.forEach { item ->
            val card = createCardForParking(this, item)
            favoritesContainer.addView(card)
        }
    }

    private fun createCardForParking(context: Context, item: ParkingInfo): View {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.card_layout, null) as MaterialCardView

        val titleTextView = cardView.findViewById<TextView>(R.id.tittle)
        val secondaryTextView = cardView.findViewById<TextView>(R.id.seccondary)
        val supportingTextView = cardView.findViewById<TextView>(R.id.supporting)
        val buttonGoTo = cardView.findViewById<MaterialButton>(R.id.button_go_to)

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

        return cardView
    }
}
