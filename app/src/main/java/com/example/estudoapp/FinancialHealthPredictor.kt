package com.example.estudoapp

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FinancialHealthPredictor(private val context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 6 // renda, gastos, dívidas, poupança, idade, investimentos
    private val outputSize = 3 // 3 classes (baixa, média, alta)

    private var scalerMean: FloatArray
    private var scalerScale: FloatArray

    init {
        interpreter = Interpreter(loadModelFile("edufin_model.tflite"))
        scalerMean = loadNpyFromAssets("scaler_mean.npy")
        scalerScale = loadNpyFromAssets("scaler_scale.npy")

        Log.d("EduFinAI", "✅ Carregado ${scalerMean.size} valores de scaler_mean.npy")
        Log.d("EduFinAI", "✅ Carregado ${scalerScale.size} valores de scaler_scale.npy")
        Log.d("EduFinAI", "Scaler mean: ${scalerMean.joinToString()}")
        Log.d("EduFinAI", "Scaler scale: ${scalerScale.joinToString()}")
    }

    // Carrega modelo .tflite
    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Lê arquivos .npy (float32)
    private fun loadNpyFromAssets(fileName: String): FloatArray {
        return try {
            val inputStream = context.assets.open(fileName)
            val bytes = inputStream.readBytes()
            inputStream.close()

            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            // Valida cabeçalho
            val magic = ByteArray(6)
            buffer.get(magic)
            if (!magic.contentEquals(byteArrayOf(0x93.toByte(), 'N'.code.toByte(), 'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte()))) {
                throw IOException("Arquivo .npy inválido: cabeçalho incorreto.")
            }

            val majorVersion = buffer.get().toInt()
            val minorVersion = buffer.get().toInt()
            val headerLen = if (majorVersion == 1) buffer.short.toInt() else buffer.int

            val headerBytes = ByteArray(headerLen)
            buffer.get(headerBytes)
            val header = String(headerBytes)
            Log.d("EduFinAI", "📄 Cabeçalho do arquivo $fileName: $header")

            if (!header.contains("'descr': '<f4'")) {
                throw IOException("Tipo de dado não suportado (esperado float32).")
            }

            val dataBytes = bytes.size - (10 + headerLen)
            val numFloats = dataBytes / 4
            val floats = FloatArray(numFloats)
            for (i in 0 until numFloats) {
                floats[i] = buffer.float
            }
            floats
        } catch (e: Exception) {
            Log.e("EduFinAI", "❌ Erro ao ler $fileName: ${e.message}")
            FloatArray(inputSize) { 1.0f } // fallback
        }
    }

    // Normalização idêntica ao StandardScaler
    private fun normalizeInput(input: FloatArray): FloatArray {
        val normalized = FloatArray(inputSize)
        for (i in 0 until inputSize) {
            normalized[i] = (input[i] - scalerMean[i]) / scalerScale[i]
        }
        return normalized
    }

    // Predição do modelo
    fun predict(
        renda: Float,
        gastos: Float,
        dividas: Float,
        poupanca: Float,
        idade: Float,
        investimentos: Float
    ): String {
        val input = floatArrayOf(renda, gastos, dividas, poupanca, idade, investimentos)
        val normalizedInput = normalizeInput(input)

        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize)
        inputBuffer.order(ByteOrder.nativeOrder())
        normalizedInput.forEach { inputBuffer.putFloat(it) }

        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize)
        outputBuffer.order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val outputs = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            outputs[i] = outputBuffer.float
        }

        val predictedClass = outputs.indices.maxByOrNull { outputs[it] } ?: -1
        Log.d("EduFinAI", "📊 Saída do modelo: ${outputs.joinToString()}")

        return when (predictedClass) {
            0 -> "🔴 Baixa Saúde Financeira"
            1 -> "🟡 Média Saúde Financeira"
            2 -> "🟢 Alta Saúde Financeira"
            else -> "Indefinido"
        }
    }
}
