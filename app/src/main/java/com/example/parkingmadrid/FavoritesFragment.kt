package com.example.parkingmadrid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.parkingmadrid.Clases.ParkingInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesFragment : Fragment() {

    private lateinit var cardContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private val favoriteParkings: MutableList<ParkingInfo> = mutableListOf()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        cardContainer = view.findViewById(R.id.cardContainer)
        progressBar = view.findViewById(R.id.progressBar)

        loadFavoritesFromDatabase()
        return view
    }

    private fun loadFavoritesFromDatabase() {
        progressBar.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        currentUser?.let {
            val userFavoritesRef = database.reference.child("users").child(it.uid).child("favorites")
            userFavoritesRef.get().addOnSuccessListener { dataSnapshot ->
                progressBar.visibility = View.GONE
                favoriteParkings.clear()
                for (document in dataSnapshot.children) {
                    val favorite = document.getValue(ParkingInfo::class.java)
                    favorite?.let { favoriteParkings.add(it) }
                }
                handleResponse(favoriteParkings)
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleResponse(dataList: List<ParkingInfo>) {
        cardContainer.removeAllViews()
        dataList.forEach { item ->
            val card = createCardForParking(requireContext(), item)
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
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                startActivity(mapIntent)
            }
        }

        favoriteButton.setImageResource(R.drawable.estrella) // Suponiendo que todos son favoritos en este fragmento

        return cardView
    }
}
