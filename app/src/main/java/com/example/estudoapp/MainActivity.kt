package com.example.estudoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var predictor: FinancialHealthPredictor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        predictor = FinancialHealthPredictor(this)

        val userEmail = auth.currentUser?.email
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAtualizar = findViewById<Button>(R.id.btnAtualizar)
        val textViewResultado = findViewById<TextView>(R.id.textViewResultado)

        val rendaInput = findViewById<EditText>(R.id.inputRenda)
        val gastosInput = findViewById<EditText>(R.id.inputGastos)
        val dividasInput = findViewById<EditText>(R.id.inputDividas)
        val poupancaInput = findViewById<EditText>(R.id.inputPoupanca)
        val idadeInput = findViewById<EditText>(R.id.inputIdade)
        val investimentosInput = findViewById<EditText>(R.id.inputInvestimentos)

        if (userEmail == null) {
            Toast.makeText(this, "Nenhum usuário logado!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 🔹 Carregar dados do usuário logado
        db.collection("usuarios")
            .document("usuarios")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    rendaInput.setText(doc.getLong("renda")?.toString() ?: "")
                    gastosInput.setText(doc.getLong("gastos")?.toString() ?: "")
                    dividasInput.setText(doc.getLong("dividas")?.toString() ?: "")
                    poupancaInput.setText(doc.getLong("poupanca")?.toString() ?: "")
                    idadeInput.setText(doc.getLong("idade")?.toString() ?: "")
                    investimentosInput.setText(doc.getLong("investimentos")?.toString() ?: "")
                    textViewResultado.text = "Resultado atual: ${doc.getString("resultado") ?: "N/A"}"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados: ${it.message}", Toast.LENGTH_SHORT).show()
            }

        // 🔹 Atualizar dados e salvar no Firestore
        btnAtualizar.setOnClickListener {
            val renda = rendaInput.text.toString().toFloatOrNull() ?: 0f
            val gastos = gastosInput.text.toString().toFloatOrNull() ?: 0f
            val dividas = dividasInput.text.toString().toFloatOrNull() ?: 0f
            val poupanca = poupancaInput.text.toString().toFloatOrNull() ?: 0f
            val idade = idadeInput.text.toString().toFloatOrNull() ?: 0f
            val investimentos = investimentosInput.text.toString().toFloatOrNull() ?: 0f

            val resultadoIA = predictor.predict(
                renda, gastos, dividas, poupanca, idade, investimentos
            )

            textViewResultado.text = "Resultado IA: $resultadoIA"

            val userDoc = db.collection("usuarios").document("usuarios")
            val dadosAtualizados = hashMapOf(
                "renda" to renda.toInt(),
                "gastos" to gastos.toInt(),
                "dividas" to dividas.toInt(),
                "poupanca" to poupanca.toInt(),
                "idade" to idade.toInt(),
                "investimentos" to investimentos.toInt(),
                "resultado" to resultadoIA
            )

            userDoc.update(dadosAtualizados as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar dados: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 🔹 Botão de logout
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
