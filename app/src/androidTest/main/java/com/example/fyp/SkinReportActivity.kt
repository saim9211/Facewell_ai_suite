package com.example.fyp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.utils.MLUtils
import com.example.fyp.utils.ModelLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SkinReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val MODEL_NAME = "skin_full_int8.tflite"
        const val LABELS_FILE = "labels.txt"
        const val TAG = "SkinReportActivity"
    }

    // Views
    private lateinit var ivPreview: ImageView
    private lateinit var ivHeatmap: ImageView
    private lateinit var tvSummaryText: TextView
    private lateinit var tvAccuracyLabel: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnVisit: MaterialButton
    private lateinit var btnGoHome: MaterialButton
    private lateinit var tvRecSummary: TextView
    private lateinit var llTips: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var rvPatches: RecyclerView
    private lateinit var tvPatchesTitle: TextView

    // ⭐ NEW PRODUCT VIEW BINDINGS
    private lateinit var rvProducts: RecyclerView
    private lateinit var tvProductsTitle: TextView
    private lateinit var btnSeeMoreProducts: MaterialButton

    // Data
    private lateinit var interpreter: Interpreter
    private var labels: List<String> = emptyList()
    private var inputH = 128
    private var inputW = 128
    private var inputChannels = 3
    private var inputDataType = DataType.FLOAT32
    private var inputScale = 1.0f
    private var inputZeroPoint = 0

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var previewUriStr: String? = null
    private var faceHeatmapUriStr: String? = null

    private val patchResults = mutableListOf<PatchResult>()

    private var lastFinalLabel: String = "unknown"
    private var lastFinalProb: Float = 0f
    private var lastRecommendations: List<String> = emptyList()

    data class PatchResult(val bmp: Bitmap, val label: String, val confidence: Double)

    // ---------------------------------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_report)

        bindViews()
        btnBack.setOnClickListener { finish() }

        RecommendationProvider.loadFromAssets(this, "skin_recommendations.json")

        labels = loadLabels(LABELS_FILE)
        loadModel()

        val imgUriStr = intent.getStringExtra(EXTRA_IMAGE_URI)
            ?: intent.getStringExtra("extra_image_uri")
        if (imgUriStr.isNullOrEmpty()) { finish(); return }
        previewUriStr = imgUriStr

        val imgUri = Uri.parse(imgUriStr)
        val bmp = MLUtils.decodeBitmap(contentResolver, imgUri) ?: run {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        ivPreview.setImageBitmap(bmp)

        rvPatches.layoutManager = GridLayoutManager(this, 2)
        rvPatches.adapter = PatchAdapter(patchResults)

        val loader = showLoader()

        Thread {
            try {
                processSkin(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Skin analysis failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                runOnUiThread { loader.dismiss() }
            }
        }.start()

        btnSave.setOnClickListener { saveReport() }

        btnVisit.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            i.putExtra("open_tab", "clinics")
            startActivity(i)
            finish()
        }

        btnGoHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            i.putExtra("open_tab", "home")
            startActivity(i)
            finish()
        }

        // ⭐ SEE MORE → SearchProductsActivity(category=skin)
        btnSeeMoreProducts.setOnClickListener {
            val i = Intent(this, SearchProductsActivity::class.java)
            i.putExtra("category", "skin")
            startActivity(i)
        }
    }

    private fun bindViews() {
        ivPreview = findViewById(R.id.ivPreview)
        ivHeatmap = findViewById(R.id.ivHeatmap)
        tvSummaryText = findViewById(R.id.tvSummaryText)
        tvAccuracyLabel = findViewById(R.id.tvAccuracyLabel)
        btnSave = findViewById(R.id.btnSaveReport)
        btnVisit = findViewById(R.id.btnVisitClinic)
        btnGoHome = findViewById(R.id.btnGoHome)
        tvRecSummary = findViewById(R.id.tvRecSummary)
        llTips = findViewById(R.id.llTips)
        btnBack = findViewById(R.id.btnBack)
        rvPatches = findViewById(R.id.rvPatches)
        tvPatchesTitle = findViewById(R.id.tvPatchesTitle)

        // ⭐ PRODUCT VIEWS
        rvProducts = findViewById(R.id.rvProducts)
        tvProductsTitle = findViewById(R.id.tvProductsTitle)
        btnSeeMoreProducts = findViewById(R.id.btnSeeMoreProducts)
    }

    private fun loadModel() {
        try {
            interpreter = ModelLoader.loadModelFromAssets(this, MODEL_NAME)
            val t = interpreter.getInputTensor(0)
            val shape = t.shape()
            if (shape.size >= 3) { inputH = shape[1]; inputW = shape[2] }
            inputDataType = t.dataType()
            inputChannels = if (shape.size >= 4) shape[3] else 3
            val q = t.quantizationParams()
            inputScale = q.scale
            inputZeroPoint = q.zeroPoint
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // PROCESS SKIN + SHOW UI
    @RequiresApi(Build.VERSION_CODES.O)
    private fun processSkin(bmp: Bitmap) {
        val summary = runSkinOnBitmap(bmp)

        // Patch + label aggregation using MLUtils
        val (patches, aggProbs) = MLUtils.analyzePatchesFromSelfieBlocking(
            bmp, interpreter, labels, inputW, inputH, inputChannels,
            inputDataType, inputScale, inputZeroPoint
        )

        patchResults.clear()
        patches.forEach { p ->
            patchResults.add(PatchResult(p.bmp, p.label, p.confidence))
        }

        val finalLabel = detectFinalSkinLabel()
        lastFinalLabel = finalLabel

        lastFinalProb =
            if (labels.contains(finalLabel)) {
                aggProbs[labels.indexOf(finalLabel)]
            } else 0f

        val disp = finalLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val recKey = finalLabel.replace("\\s".toRegex(), "").lowercase()
        val rec = RecommendationProvider.getSkinRecommendation(recKey)
        lastRecommendations = rec?.tips ?: emptyList()

        runOnUiThread {
            tvSummaryText.text = disp
            tvAccuracyLabel.text = "Accuracy Level: ${(lastFinalProb * 100).toInt()}%"
            rvPatches.adapter?.notifyDataSetChanged()

            tvRecSummary.text = rec?.summary ?: "Follow general skin care steps."
            llTips.removeAllViews()
            rec?.tips?.forEach { t ->
                val tv = layoutInflater.inflate(android.R.layout.simple_list_item_1, llTips, false) as TextView
                tv.text = "• $t"
                tv.setTextColor(resources.getColor(R.color.black))
                llTips.addView(tv)
            }

            // ⭐ PRODUCT FETCHING
            fetchMatchingProducts(finalLabel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runSkinOnBitmap(fullBmp: Bitmap): String {
        // use MLUtils.detectFacesBlocking for face detection (same as original logic)
        val faces = MLUtils.detectFacesBlocking(fullBmp, accurate = false)
        if (faces.isEmpty()) return "No face detected."
        val face = faces[0]
        val bbox = face.boundingBox
        val pad = (0.15 * max(bbox.width(), bbox.height())).toInt()
        val left = max(0, bbox.left - pad)
        val top = max(0, bbox.top - pad)
        val right = min(fullBmp.width - 1, bbox.right + pad)
        val bottom = min(fullBmp.height - 1, bbox.bottom + pad)
        val faceRect = android.graphics.Rect(left, top, right, bottom)
        val faceBmp = Bitmap.createBitmap(fullBmp, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())

        val heatAcc = Array(faceBmp.height) { FloatArray(faceBmp.width) }
        val heatCount = Array(faceBmp.height) { IntArray(faceBmp.width) }

        val winH = inputH
        val winW = inputW
        val stride = (winW * 0.5).roundToInt().coerceAtLeast(1)

        for (y in 0 until max(1, faceBmp.height - winH + 1) step stride) {
            for (x in 0 until max(1, faceBmp.width - winW + 1) step stride) {
                val w = if (x + winW <= faceBmp.width) winW else (faceBmp.width - x)
                val h = if (y + winH <= faceBmp.height) winH else (faceBmp.height - y)
                val patch = Bitmap.createBitmap(faceBmp, x, y, w, h)
                val resized = Bitmap.createScaledBitmap(patch, winW, winH, true)

                val inputBuffer = if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                    ByteBuffer.allocateDirect(winW * winH * inputChannels).order(ByteOrder.nativeOrder())
                } else {
                    ByteBuffer.allocateDirect(4 * winW * winH * inputChannels).order(ByteOrder.nativeOrder())
                }
                inputBuffer.rewind()

                for (py in 0 until winH) {
                    for (px in 0 until winW) {
                        val p = resized.getPixel(px, py)
                        val r = (p shr 16 and 0xFF)
                        val g = (p shr 8 and 0xFF)
                        val b = (p and 0xFF)
                        if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                            val rf = r / 255f
                            val gf = g / 255f
                            val bf = b / 255f
                            val qr = ((rf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                            val qg = ((gf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                            val qb = ((bf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                            inputBuffer.put(qr.toByte())
                            inputBuffer.put(qg.toByte())
                            inputBuffer.put(qb.toByte())
                        } else {
                            inputBuffer.putFloat(r / 255f)
                            inputBuffer.putFloat(g / 255f)
                            inputBuffer.putFloat(b / 255f)
                        }
                    }
                }
                inputBuffer.rewind()

                // call helper to run model and produce dequantized float outputs
                val raw = MLUtils.runModelGetOutputAsFloatArray(interpreter, inputBuffer)

                val maxv = raw.maxOrNull() ?: 0f
                val exp = raw.map { Math.exp((it - maxv).toDouble()).toFloat() }
                val sum = exp.sum()
                val soft = if (sum > 0f) exp.map { it / sum } else exp.map { 0f }

                var maxVal = soft[0]
                for (i in 1 until soft.size) if (soft[i] > maxVal) maxVal = soft[i]

                val pxLeft = x; val pxTop = y
                val pxRight = min(faceBmp.width - 1, x + winW - 1)
                val pxBottom = min(faceBmp.height - 1, y + winH - 1)
                for (yy in pxTop..pxBottom) {
                    val row = heatAcc[yy]
                    val cntRow = heatCount[yy]
                    for (xx in pxLeft..pxRight) {
                        row[xx] += maxVal
                        cntRow[xx] += 1
                    }
                }
            }
        }

        val heatBmp = Bitmap.createBitmap(faceBmp.width, faceBmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(heatBmp)
        val paint = Paint().apply { style = Paint.Style.FILL }

        var globalMax = 0f
        for (yy in 0 until faceBmp.height) for (xx in 0 until faceBmp.width) {
            val c = heatCount[yy][xx]
            if (c > 0) {
                val v = heatAcc[yy][xx] / c
                if (v > globalMax) globalMax = v
            }
        }

        for (yy in 0 until faceBmp.height) {
            for (xx in 0 until faceBmp.width) {
                val c = heatCount[yy][xx]
                val v = if (c > 0) (heatAcc[yy][xx] / c) / (globalMax.coerceAtLeast(1f)) else 0f
                val alpha = (v * 200).toInt().coerceIn(0, 200)
                val color = Color.argb(alpha, (255 * v).toInt(), (180 * (1 - v)).toInt(), 0)
                paint.color = color
                canvas.drawPoint(xx.toFloat(), yy.toFloat(), paint)
            }
        }

        val dispW = ivPreview.width.takeIf { it > 0 } ?: fullBmp.width
        val dispH = ivPreview.height.takeIf { it > 0 } ?: fullBmp.height
        val displayed = Bitmap.createScaledBitmap(fullBmp, dispW, dispH, true)
        val scaleX = displayed.width.toFloat() / fullBmp.width
        val scaleY = displayed.height.toFloat() / fullBmp.height

        val overlayBmp = Bitmap.createBitmap(displayed.width, displayed.height, Bitmap.Config.ARGB_8888)
        val overlayCanvas = Canvas(overlayBmp)
        overlayCanvas.drawBitmap(displayed, 0f, 0f, null)

        val heatScaled = Bitmap.createScaledBitmap(heatBmp, (faceRect.width() * scaleX).roundToInt(), (faceRect.height() * scaleY).roundToInt(), true)
        val leftOnDisp = (faceRect.left * scaleX).roundToInt()
        val topOnDisp = (faceRect.top * scaleY).roundToInt()
        val heatPaint = Paint().apply { alpha = 200 }
        overlayCanvas.drawBitmap(heatScaled, leftOnDisp.toFloat(), topOnDisp.toFloat(), heatPaint)

        val overlayUri = saveBitmapToCache(overlayBmp, "skin_heat_${System.currentTimeMillis()}.jpg")
        faceHeatmapUriStr = overlayUri?.toString()

        runOnUiThread {
            ivHeatmap.setImageBitmap(overlayBmp)
            ivHeatmap.visibility = View.VISIBLE
        }

        // after heatmap generation, also do final coarse label sampling to produce summary text
        val labelCounts = mutableMapOf<String, Int>()
        val sampleStride = max(1, inputW / 2)
        for (yy in 0 until faceBmp.height step sampleStride) {
            for (xx in 0 until faceBmp.width step sampleStride) {
                val w = min(inputW, faceBmp.width - xx)
                val h = min(inputH, faceBmp.height - yy)
                val patch = Bitmap.createBitmap(faceBmp, xx, yy, w, h)
                val resized = Bitmap.createScaledBitmap(patch, inputW, inputH, true)

                val inputBuffer = if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                    ByteBuffer.allocateDirect(inputW * inputH * inputChannels).order(ByteOrder.nativeOrder())
                } else {
                    ByteBuffer.allocateDirect(4 * inputW * inputH * inputChannels).order(ByteOrder.nativeOrder())
                }
                inputBuffer.rewind()
                for (py in 0 until inputH) for (px in 0 until inputW) {
                    val p = resized.getPixel(px, py)
                    val r = (p shr 16 and 0xFF)
                    val g = (p shr 8 and 0xFF)
                    val b = (p and 0xFF)
                    if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
                        val qr = ((rf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                        val qg = ((gf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                        val qb = ((bf / inputScale).roundToInt() + inputZeroPoint).coerceIn(0, 255)
                        inputBuffer.put(qr.toByte()); inputBuffer.put(qg.toByte()); inputBuffer.put(qb.toByte())
                    } else {
                        inputBuffer.putFloat(r / 255f); inputBuffer.putFloat(g / 255f); inputBuffer.putFloat(b / 255f)
                    }
                }
                inputBuffer.rewind()

                val raw = MLUtils.runModelGetOutputAsFloatArray(interpreter, inputBuffer)

                val maxv = raw.maxOrNull() ?: 0f
                val exp = raw.map { Math.exp((it - maxv).toDouble()).toFloat() }
                val sum = exp.sum()
                val probs = if (sum > 0f) exp.map { it / sum } else exp.map { 0f }

                var maxi = 0; var mv = probs[0]
                for (i in probs.indices) if (probs[i] > mv) { mv = probs[i]; maxi = i }
                val lbl = labels.getOrNull(maxi) ?: "unknown"
                labelCounts[lbl] = (labelCounts[lbl] ?: 0) + 1
            }
        }

        val topEntry = labelCounts.maxByOrNull { it.value }
        val topLabel = topEntry?.key ?: "normal"
        val summaryText = topLabel.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        val recKey = topLabel.replace("\\s".toRegex(), "").lowercase()
        val rec = RecommendationProvider.getSkinRecommendation(recKey)

        runOnUiThread {
            tvRecSummary.text = rec?.summary ?: "Follow general skin care steps."
            llTips.removeAllViews()
            rec?.tips?.forEach { tip ->
                val tv = layoutInflater.inflate(android.R.layout.simple_list_item_1, llTips, false) as TextView
                tv.text = "\u2022  $tip"
                tv.setTextColor(resources.getColor(R.color.black))
                tv.setTextSize(14f)
                llTips.addView(tv)
            }
        }

        return summaryText
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveBitmapToCache(bmp: Bitmap, name: String): Uri? {
        return try {
            val f = kotlin.io.path.createTempFile(prefix = name, suffix = ".jpg").toFile()
            FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            Uri.fromFile(f)
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }



    // ---------------------------------------------------------------------------------------------
    // ⭐ FIRESTORE PRODUCT FETCH — just like Eye Report
    private fun fetchMatchingProducts(label: String) {

        // Model label is already EXACT (acne, eczema, normal, rosacea)
        val formatted = label.lowercase().trim()

        db.collection("products")
            .whereEqualTo("category", "skin")
            .whereEqualTo("isActive", true)
            .whereArrayContains("recommendedFor", formatted)
//            .orderBy("avgRating", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { snap ->

                if (!snap.isEmpty) {
                    // Found exact matches
                    showProductsWithAdapter(snap.toObjects(Product::class.java))
                    return@addOnSuccessListener
                }

                // Fallback → show any top skin products
                db.collection("products")
                    .whereEqualTo("category", "skin")
                    .whereEqualTo("isActive", true)
//                    .orderBy("avgRating", Query.Direction.DESCENDING)
                    .limit(2)
                    .get()
                    .addOnSuccessListener { fallbackSnap ->
                        showProductsWithAdapter(fallbackSnap.toObjects(Product::class.java))
                    }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }



    // ⭐ SHOW PRODUCTS
    private fun showProductsWithAdapter(list: List<Product>) {
        if (list.isEmpty()) {
            rvProducts.visibility = View.GONE
            tvProductsTitle.visibility = View.GONE
            btnSeeMoreProducts.visibility = View.GONE
            return
        }

        tvProductsTitle.visibility = View.VISIBLE
        rvProducts.visibility = View.VISIBLE
        btnSeeMoreProducts.visibility = View.VISIBLE

        rvProducts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val adapter = ProductAdapter(
            ctx = this,
            list = list
        ) { product ->
            ProductDetailBottomSheet.newInstance(product)
                .show(supportFragmentManager, "product_dialog")
        }

        rvProducts.adapter = adapter
    }

    // ---------------------------------------------------------------------------------------------
    private fun detectFinalSkinLabel(): String {
        return if (patchResults.isEmpty()) "unknown"
        else patchResults.groupBy { it.label }
            .maxByOrNull { it.value.size }?.key ?: "unknown"
    }

    // ---------------------------------------------------------------------------------------------
    private fun showLoader(): Dialog {
        val dlg = Dialog(this)
        dlg.setContentView(layoutInflater.inflate(R.layout.dialog_simple_loader, null))
        dlg.setCancelable(false)
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dlg.show()
        return dlg
    }

    // ---------------------------------------------------------------------------------------------
    // (UNCHANGED) — Saves report to Firestore
    private fun saveReport() {
        val uid = auth.currentUser?.uid ?: return
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val summary = tvSummaryText.text.toString()
        val topLabel = lastFinalLabel
        val recsToSave = lastRecommendations

        uploadImagesAndSaveReport(
            uid,
            previewUriStr,
            faceHeatmapUriStr,
            type = "skin",
            summary = summary,
            topLabel = topLabel,
            confidence = lastFinalProb.toDouble(),
            recommendations = recsToSave
        ) { success ->
            runOnUiThread {
                btnSave.isEnabled = true
                btnSave.text = "Save Report"
                if (success) {
                    Toast.makeText(this, "Report saved", Toast.LENGTH_SHORT).show()
                    val i = Intent(this, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    i.putExtra("open_tab", "home")
                    startActivity(i)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save report", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImagesAndSaveReport(
        uid: String,
        previewUriStr: String?,
        heatmapUriStr: String?,
        type: String,
        summary: String,
        topLabel: String,
        confidence: Double,
        recommendations: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        val previewUri = previewUriStr?.let { Uri.parse(it) }
        val heatUri = heatmapUriStr?.let { Uri.parse(it) }

        val storageRef = storage.reference.child("reports").child(uid)
        val uploaded = mutableMapOf<String, String>()

        fun uploadOne(uri: Uri?, name: String, cb: (String?) -> Unit) {
            if (uri == null) { cb(null); return }
            try {
                val ref = storageRef.child("$name.jpg")
                val stream = contentResolver.openInputStream(uri)
                if (stream == null) { cb(null); return }
                val uploadTask = ref.putStream(stream)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("upload failed")
                    ref.downloadUrl
                }.addOnCompleteListener { t ->
                    if (t.isSuccessful) cb(t.result.toString()) else cb(null)
                }
            } catch (e: Exception) {
                e.printStackTrace(); cb(null)
            }
        }

        // Upload preview -> heat -> create top-level report doc -> link to user.reports (with retry)
        uploadOne(previewUri, "preview_${System.currentTimeMillis()}") { pUrl ->
            uploaded["preview"] = pUrl ?: ""
            uploadOne(heatUri, "heat_${System.currentTimeMillis()}") { hUrl ->
                uploaded["heat"] = hUrl ?: ""

                // Build skin_scan payload (ScanResult-like) and include recommendations
                val skinScan = hashMapOf<String, Any>(
                    "summary" to summary,
                    "accuracy" to confidence,
                    "topLabel" to topLabel,
                    "previewUrl" to (uploaded["preview"] ?: ""),
                    "heatUrl" to (uploaded["heat"] ?: ""),
                    "recommendations" to recommendations
                )

                // create report doc in top-level "reports" collection
                try {
                    val reportsCol = db.collection("reports")
                    val newDocRef = reportsCol.document()
                    val reportId = newDocRef.id

                    val reportPayload = hashMapOf<String, Any?>(
                        "reportId" to reportId,
                        "userId" to uid,
                        "type" to type,
                        "summary" to summary,
                        "imageUrl" to (uploaded["preview"] ?: ""),
                        "imageWidth" to null,
                        "imageHeight" to null,
                        "accuracy" to confidence,
                        "recommendations" to recommendations,
                        "eye_scan" to null,
                        "skin_scan" to skinScan,
                        "mood_scan" to null,
                        "tags" to listOf<String>(),
                        "source" to "camera",
                        "meta" to hashMapOf("orientation" to "unknown"),
                        "createdAt" to Timestamp.now()
                    )

                    newDocRef.set(reportPayload)
                        .addOnSuccessListener {
                            // push only the reportId into user's reports array with retry
                            val userDoc = db.collection("users").document(uid)
                            updateUserReportsWithRetry(userDoc, reportId, onComplete)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to create report doc", e)
                            onComplete(false)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onComplete(false)
                }
            }
        }
    }

    private fun updateUserReportsWithRetry(userDocRef: com.google.firebase.firestore.DocumentReference, reportId: String, onComplete: (Boolean) -> Unit, attempt: Int = 0) {
        userDocRef.update("reports", FieldValue.arrayUnion(reportId))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "update reports failed (attempt=$attempt): ${e.message}", e)
                if (attempt < 1) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        updateUserReportsWithRetry(userDocRef, reportId, onComplete, attempt + 1)
                    }, 800)
                } else {
                    val payload = hashMapOf("reports" to listOf(reportId))
                    userDocRef.set(payload, SetOptions.merge())
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { ex -> ex.printStackTrace(); onComplete(false) }
                }
            }
    }



    // ---------------------------------------------------------------------------------------------
    private fun loadLabels(fileName: String): List<String> {
        return try {
            val br = BufferedReader(InputStreamReader(assets.open(fileName)))
            br.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // (UNCHANGED) Patch Adapter
    inner class PatchAdapter(private val items: List<PatchResult>) :
        RecyclerView.Adapter<PatchAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val iv: ImageView = v.findViewById(R.id.ivPatch)
            val tvLbl: TextView = v.findViewById(R.id.tvPatchLabel)
            val card: MaterialCardView = v.findViewById(R.id.cardPatch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patch, parent, false)
            return VH(v)
        }

        @SuppressLint("MissingInflatedId")
        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = items[position]
            holder.iv.setImageBitmap(p.bmp)
            holder.tvLbl.text = "${p.label} (${String.format("%.1f", p.confidence)}%)"

            holder.card.setOnClickListener {
                val d = Dialog(this@SkinReportActivity)
                val vv = layoutInflater.inflate(R.layout.dialog_patch_full, null)
                val ivf = vv.findViewById<ImageView>(R.id.ivFull)
                val tv = vv.findViewById<TextView>(R.id.tvFullLabel)
                ivf.setImageBitmap(p.bmp)
                tv.text = holder.tvLbl.text
                d.setContentView(vv)
                d.window?.setBackgroundDrawableResource(android.R.color.transparent)
                d.show()
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
