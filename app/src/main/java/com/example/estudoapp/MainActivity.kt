package com.example.estudoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val textView = findViewById<TextView>(R.id.textViewDados)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Pega o e-mail do usuário logado
        val userEmail = auth.currentUser?.email

        if (userEmail != null) {
            // 🔹 Agora acessamos direto o documento "usuarios"
            db.collection("usuarios")
                .document("usuarios")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val renda = doc.getLong("renda") ?: 0
                        val gastos = doc.getLong("gastos") ?: 0
                        val dividas = doc.getLong("dividas") ?: 0
                        val investimentos = doc.getLong("investimentos") ?: 0
                        val poupanca = doc.getLong("poupanca") ?: 0
                        val idade = doc.getLong("idade") ?: 0
                        val resultado = doc.getString("resultado") ?: "N/A"
                        val email = doc.getString("email") ?: "N/A"

                        textView.text = """
                            Email: $email
                            Idade: $idade
                            Renda: R$$renda
                            Gastos: R$$gastos
                            Dívidas: R$$dividas
                            Investimentos: R$$investimentos
                            Poupança: R$$poupanca
                            Resultado: $resultado
                        """.trimIndent()
                    } else {
                        textView.text = "Nenhum dado encontrado no Firestore."
                    }
                }
                .addOnFailureListener {
                    textView.text = "Erro ao carregar dados: ${it.message}"
                }
        } else {
            textView.text = "Nenhum usuário logado."
        }

        // Botão de logout
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
