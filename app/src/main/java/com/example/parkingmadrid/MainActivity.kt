package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private var isFirstFacebookLogin = true // Bandera para controlar el primer inicio de sesión con Facebook

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val btnIniciarSesion: Button = findViewById(R.id.buttonLogin)
        btnIniciarSesion.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        val btnRegistro: Button = findViewById(R.id.buttonRegister)
        btnRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }

        mAuth = FirebaseAuth.getInstance()

        val buttonLoginGoogle = findViewById<ImageButton>(R.id.buttonLoginGoogle)
        buttonLoginGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Inicializar el CallbackManager de Facebook
        callbackManager = CallbackManager.Factory.create()

        // Configurar el botón de inicio de sesión con Facebook
        val buttonLoginFacebook = findViewById<ImageButton>(R.id.buttonLoginFacebook)
        buttonLoginFacebook.setOnClickListener {
            signInWithFacebook()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        // Verificar si ya existe una sesión activa de Facebook
        val accessToken = AccessToken.getCurrentAccessToken()
        if (accessToken != null && !accessToken.isExpired) {
            // Si ya hay una sesión activa, iniciar sesión directamente con Firebase
            firebaseAuthWithFacebook(accessToken)
        } else {
            // Si no hay una sesión activa, solicitar al usuario que inicie sesión
            val permissions = listOf("email", "public_profile")
            LoginManager.getInstance().logInWithReadPermissions(this, permissions)
        }
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso con Facebook
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual si no se desea volver atrás
                } else {
                    // Manejar errores
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }


    private fun handleUserCollision(exception: FirebaseAuthUserCollisionException, credential: AuthCredential) {
        val email = exception.email
        if (email != null) {
            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val signInMethods = task.result?.signInMethods ?: emptyList()
                        if (signInMethods.contains(credential.provider)) {
                            // La dirección de correo electrónico ya está asociada con el proveedor actual,
                            // no es necesario fusionar cuentas
                            // Puedes informar al usuario o continuar con el inicio de sesión normal
                        } else {
                            // La dirección de correo electrónico ya está asociada con otra cuenta,
                            // se debe fusionar las cuentas
                            mergeAccounts(credential)
                        }
                    } else {
                        // Manejar error al obtener los proveedores de autenticación
                    }
                }
        } else {
            // No se proporcionó una dirección de correo electrónico, manejar el error en consecuencia
        }
    }

    private fun mergeAccounts(credential: AuthCredential) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Fusionar cuentas: vincular las credenciales del proveedor actual a la cuenta existente del usuario
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Fusion de cuentas exitosa
                        // Informar al usuario o continuar con el flujo de la aplicación
                    } else {
                        // Manejar error en la fusión de cuentas
                    }
                }
        } else {
            // No hay un usuario actualmente autenticado, manejar el error en consecuencia
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pasar el resultado del inicio de sesión con Facebook al callbackManager
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Autenticar con Google
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Manejar errores
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val user = mAuth.currentUser
                    // Ir a la siguiente actividad
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual si no se desea volver atrás
                } else {
                    // Manejar errores
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        // Manejar la fusión de cuentas
                        handleUserCollision(task.exception as FirebaseAuthUserCollisionException, credential)
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                    }
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "MainActivity"
    }
}


