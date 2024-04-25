package com.example.parkingmadrid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Fragmento1

class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Reemplaza el contenido del contenedor de fragments con tu primer fragmento
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, Fragmento1())
            .commit()
    }
}