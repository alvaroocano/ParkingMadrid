package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var editTextUsernameOrEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var database: FirebaseDatabase

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance("https://parking-madrid-fc293-default-rtdb.europe-west1.firebasedatabase.app")

        editTextUsernameOrEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)

        val btnIniciarSesion: Button = findViewById(R.id.buttonLogin)
        btnIniciarSesion.setOnClickListener {
            signIn()
        }

        val btnRegistro: Button = findViewById(R.id.buttonRegister)
        btnRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }

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

    private fun signIn() {
        val usernameOrEmail = editTextUsernameOrEmail.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            // Validación de campos
            Toast.makeText(this, "Por favor, ingrese tanto el nombre de usuario/correo electrónico como la contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidInput(usernameOrEmail) || !isValidInput(password)) {
            // Validación de inyección de código
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the input is an email
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            signInWithEmailAndPassword(usernameOrEmail, password)
        } else {
            // Assume input is a nickname and try to find corresponding email
            findUserByNick(usernameOrEmail, password)
        }
    }

    private fun isValidInput(input: String): Boolean {
        // Validar que la entrada no contenga caracteres especiales potencialmente peligrosos
        val regex = "^[a-zA-Z0-9@.]+$"
        return input.matches(regex.toRegex())
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual si no se desea volver atrás
                } else {
                    // Manejar errores
                    Log.w(TAG, "signInWithEmailAndPassword:failure", task.exception)
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "signInWithEmailAndPassword:error", exception)
                Toast.makeText(this, "Error al iniciar sesión. Intente nuevamente.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findUserByNick(nickname: String, password: String) {
        val usersRef = database.reference.child("users")
        val query = usersRef.orderByChild("nickname").equalTo(nickname)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val email = userSnapshot.child("email").getValue(String::class.java)
                        if (email != null) {
                            signInWithEmailAndPassword(email, password)
                            return
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "findUserByNick:onCancelled", databaseError.toException())
                Toast.makeText(this@MainActivity, "Error en la base de datos. Intente nuevamente.", Toast.LENGTH_SHORT).show()
            }
        })
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
                    Toast.makeText(this, "Error de autenticación con Facebook. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "firebaseAuthWithFacebook:error", exception)
                Toast.makeText(this, "Error al iniciar sesión con Facebook. Intente nuevamente.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "Correo electrónico ya asociado con este proveedor.", Toast.LENGTH_SHORT).show()
                        } else {
                            // La dirección de correo electrónico ya está asociada con otra cuenta,
                            // se debe fusionar las cuentas
                            mergeAccounts(credential)
                        }
                    } else {
                        // Manejar error al obtener los proveedores de autenticación
                        Log.e(TAG, "fetchSignInMethodsForEmail:failure", task.exception)
                        Toast.makeText(this, "Error al verificar el método de autenticación. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "fetchSignInMethodsForEmail:error", exception)
                    Toast.makeText(this, "Error al verificar el método de autenticación. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No se proporcionó una dirección de correo electrónico, manejar el error en consecuencia
            Log.e(TAG, "FirebaseAuthUserCollisionException: email is null")
            Toast.makeText(this, "Error de autenticación. Intente nuevamente.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mergeAccounts(credential: AuthCredential) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Fusionar cuentas: vincular las credenciales del proveedor actual a la cuenta existente del usuario
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Fusión de cuentas exitosa
                        Toast.makeText(this, "Cuentas fusionadas exitosamente.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Manejar error en la fusión de cuentas
                        Log.e(TAG, "mergeAccounts:failure", task.exception)
                        Toast.makeText(this, "Error al fusionar cuentas. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "mergeAccounts:error", exception)
                    Toast.makeText(this, "Error al fusionar cuentas. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No hay un usuario actualmente autenticado, manejar el error en consecuencia
            Log.e(TAG, "mergeAccounts: no current user")
            Toast.makeText(this, "Error de autenticación. Intente nuevamente.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Error al iniciar sesión con Google. Intente nuevamente.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Error de autenticación con Google. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "firebaseAuthWithGoogle:error", exception)
                Toast.makeText(this, "Error al iniciar sesión con Google. Intente nuevamente.", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "MainActivity"
    }
}
