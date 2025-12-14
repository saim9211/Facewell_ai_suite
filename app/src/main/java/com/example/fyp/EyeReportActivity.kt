package com.example.fyp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.view.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.models.Report
import com.example.fyp.utils.MLUtils
import com.example.fyp.utils.ModelLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.tensorflow.lite.Interpreter
import java.io.FileOutputStream
import java.util.Locale

class EyeReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val MODEL_NAME = "eye_disease_model.tflite"
        private const val TAG = "EyeReportActivity"
    }

    private lateinit var ivPreview: ImageView
    private lateinit var ivLeftCrop: ImageView
    private lateinit var ivRightCrop: ImageView
    private lateinit var tvLeftLabel: TextView
    private lateinit var tvRightLabel: TextView
    private lateinit var tvSummaryText: TextView
    private lateinit var tvAccuracyLabel: TextView
    private lateinit var tvOverallResult: TextView

    private lateinit var tvRecSummary: TextView
    private lateinit var llTips: LinearLayout

    private lateinit var rvProducts: RecyclerView
    private lateinit var tvProductsTitle: TextView

    private lateinit var btnSeeMoreProducts: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnVisit: MaterialButton
    private lateinit var btnGoHome: MaterialButton
    private lateinit var btnBack: ImageButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var interpreter: Interpreter

    private var previewUriStr: String? = null
    private var leftCropUriStr: String? = null
    private var rightCropUriStr: String? = null

    private var lastOverallAccuracy = 0.0
    private var lastSummary = ""
    private var lastRecommendations = listOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eye_report)

        bindViews()
        setupButtons()

        // Load TFLite model
        try {
            interpreter = ModelLoader.loadModelFromAssets(this, MODEL_NAME)
        } catch (e: Exception) {
            Toast.makeText(this, "Model load failed", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RecommendationProvider.loadFromAssets(this, "eye_recommendations.json")

        val imgUri = intent.getStringExtra(EXTRA_IMAGE_URI)
            ?: intent.getStringExtra("extra_image_uri")

        if (imgUri.isNullOrEmpty()) {
            finish()
            return
        }

        previewUriStr = imgUri
        val bitmap = MLUtils.decodeBitmap(contentResolver, Uri.parse(imgUri))

        if (bitmap == null) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ivPreview.setImageBitmap(bitmap)

        runProcessing(bitmap)

        btnSeeMoreProducts.setOnClickListener {
            val i = Intent(this, SearchProductsActivity::class.java)

            // Optional: send classification/disease to pre-filter results
            i.putExtra("category", "eye")

            startActivity(i)
        }

    }

    private fun bindViews() {
        ivPreview = findViewById(R.id.ivPreview)
        ivLeftCrop = findViewById(R.id.ivLeftCrop)
        ivRightCrop = findViewById(R.id.ivRightCrop)
        tvLeftLabel = findViewById(R.id.tvLeftLabel)
        tvRightLabel = findViewById(R.id.tvRightLabel)
        tvSummaryText = findViewById(R.id.tvSummaryText)
        tvAccuracyLabel = findViewById(R.id.tvAccuracyLabel)
        tvOverallResult = findViewById(R.id.tvOverallResult)

        tvRecSummary = findViewById(R.id.tvRecSummary)
        llTips = findViewById(R.id.llTips)

        rvProducts = findViewById(R.id.rvProducts)
        tvProductsTitle = findViewById(R.id.tvProductsTitle)

        btnSeeMoreProducts = findViewById(R.id.btnSeeMoreProducts)
        btnSave = findViewById(R.id.btnSaveReport)
        btnVisit = findViewById(R.id.btnVisitClinic)
        btnGoHome = findViewById(R.id.btnGoHome)
        btnBack = findViewById(R.id.btnBack)

        findViewById<TextView>(R.id.tvReportTitle).text = "Eye Report"
    }

    private fun setupButtons() {
        btnBack.setOnClickListener { finish() }

        btnVisit.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.putExtra("open_tab", "clinics")
            i.putExtra("category", "EYE")
            startActivity(i)
            finish()
        }

        btnGoHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.putExtra("open_tab", "home")
            startActivity(i)
            finish()
        }

        btnSave.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            saveReport(uid)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runProcessing(bmp: Bitmap) {
        val dlg = Dialog(this)
        val v = layoutInflater.inflate(R.layout.dialog_simple_loader, null)
        dlg.setContentView(v)
        dlg.setCancelable(false)
        dlg.show()

        Thread {
            try {
                val result = MLUtils.detectAndCropEyesBlocking(bmp)
                val leftBmp = result.leftCrop
                val rightBmp = result.rightCrop

                if (leftBmp == null || rightBmp == null) {
                    runOnUiThread {
                        dlg.dismiss()
                        Toast.makeText(this, "Eyes not detected", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val detection = MLUtils.runEyeModel(interpreter, leftBmp, rightBmp)

                val leftPair = detection.first
                val rightPair = detection.second
                val overall = detection.third

                val leftUri = saveBmp(leftBmp, "left_${System.currentTimeMillis()}.jpg")
                val rightUri = saveBmp(rightBmp, "right_${System.currentTimeMillis()}.jpg")

                leftCropUriStr = leftUri?.toString()
                rightCropUriStr = rightUri?.toString()

                runOnUiThread {
                    dlg.dismiss()
                    updateUI(leftBmp, rightBmp, leftPair, rightPair, overall)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    dlg.dismiss()
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveBmp(bmp: Bitmap, name: String): Uri? {
        return try {
            val f = kotlin.io.path.createTempFile(prefix = name, suffix = ".jpg").toFile()
            FileOutputStream(f).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(f)
        } catch (_: Exception) {
            null
        }
    }


    // -----------------------------
// PART 2 — UPDATE UI + RECOMMENDATIONS + PRODUCT FETCH
// -----------------------------

    private fun updateUI(
        leftBmp: Bitmap,
        rightBmp: Bitmap,
        leftPair: Pair<String, Float>,
        rightPair: Pair<String, Float>,
        overallAcc: Float
    ) {
        // Set crops
        ivLeftCrop.setImageBitmap(leftBmp)
        ivRightCrop.setImageBitmap(rightBmp)

        val leftText = "${leftPair.first} (${(leftPair.second * 100).toInt()}%)"
        val rightText = "${rightPair.first} (${(rightPair.second * 100).toInt()}%)"

        tvLeftLabel.text = leftText
        tvRightLabel.text = rightText

        tvAccuracyLabel.text = "Accuracy Level: ${(overallAcc * 100).toInt()}%"
        val summary = "Left: $leftText • Right: $rightText"
        tvSummaryText.text = summary

        val topCondition =
            if (leftPair.second >= rightPair.second) leftPair.first else rightPair.first

        tvOverallResult.text = topCondition.uppercase()

        // Save data for Firestore
        lastOverallAccuracy = overallAcc.toDouble()
        lastSummary = summary

        // 1️⃣ Show organic recommendations
        loadRecommendations(topCondition)

        // 2️⃣ Fetch actual products from Firestore
        fetchMatchingProducts(topCondition)
    }

    /* -------------------------------------------------------
       ORGANIC RECOMMENDATIONS LOADING
     ------------------------------------------------------- */
    private fun loadRecommendations(key: String) {
        val formattedKey = key.replace("\\s+".toRegex(), "").lowercase(Locale.getDefault())

        val rec = RecommendationProvider.getEyeRecommendation(formattedKey)

        if (rec == null) {
            tvRecSummary.text = "No recommendations available."
            llTips.removeAllViews()
            lastRecommendations = emptyList()
            return
        }

        // Summary text
        tvRecSummary.text = rec.summary
        lastRecommendations = rec.tips

        // Tips list
        llTips.removeAllViews()
        for (tip in rec.tips) {
            val tv = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                llTips,
                false
            ) as TextView

            tv.text = "• $tip"
            tv.textSize = 14f
            tv.setTextColor(resources.getColor(R.color.black))
            llTips.addView(tv)
        }
    }

    /* -------------------------------------------------------
       FIRESTORE PRODUCT FETCH
       (FULL LIST, VERTICAL, USES YOUR ProductAdapter)
     ------------------------------------------------------- */
    private fun fetchMatchingProducts(condition: String) {
        val key = condition.lowercase().trim()

        // First: products that match recommendedFor[]
        db.collection("products")
            .whereEqualTo("isActive", true)
            .whereArrayContains("recommendedFor", key)
            .limit(2)
            .get()
            .addOnSuccessListener { snap ->

                if (!snap.isEmpty) {
                    showProducts(snap.toObjects(Product::class.java))
                    return@addOnSuccessListener
                }

                // Fallback: eye category
                db.collection("products")
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("category", "eye")
                    .limit(2)
                    .get()
                    .addOnSuccessListener { fallbackSnap ->
                        showProducts(fallbackSnap.toObjects(Product::class.java))
                    }
            }
    }

    /* -------------------------------------------------------
       SHOW PRODUCTS — VERTICAL + ProductAdapter
     ------------------------------------------------------- */
    private fun showProducts(list: List<Product>) {

        if (list.isEmpty()) {
            rvProducts.visibility = View.GONE
            tvProductsTitle.visibility = View.GONE
            return
        }

        tvProductsTitle.visibility = View.VISIBLE
        rvProducts.visibility = View.VISIBLE

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


    // -----------------------------
// PART 3 — SAVE REPORT + NETWORK CHECK + END
// -----------------------------

    private fun saveReport(uid: String) {

        if (!isOnline()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val newDoc = db.collection("reports").document()
        val reportId = newDoc.id

        val payload = hashMapOf(
            "reportId" to reportId,
            "userId" to uid,
            "type" to "eye",
            "summary" to lastSummary,
            "accuracy" to lastOverallAccuracy,
            "recommendations" to lastRecommendations,
            "imageUrl" to previewUriStr,
            "createdAt" to Timestamp.now(),
            "eye_scan" to hashMapOf(
                "summary" to lastSummary,
                "accuracy" to lastOverallAccuracy,
                "recommendations" to lastRecommendations,
                "leftCrop" to leftCropUriStr,
                "rightCrop" to rightCropUriStr
            )
        )

        newDoc.set(payload)
            .addOnSuccessListener {

                // link to user doc
                db.collection("users")
                    .document(uid)
                    .update("reports", FieldValue.arrayUnion(reportId))
                    .addOnSuccessListener {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Report"

                        Toast.makeText(this, "Report saved.", Toast.LENGTH_SHORT).show()

                        val i = Intent(this, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        i.putExtra("open_tab", "home")
                        startActivity(i)
                        finish()
                    }
                    .addOnFailureListener {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Report"
                        Toast.makeText(this, "Failed to update user profile.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = "Save Report"
                Toast.makeText(this, "Failed to save report.", Toast.LENGTH_SHORT).show()
            }
    }

    /* -------------------------------------------------------
       CHECK INTERNET
     ------------------------------------------------------- */
    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /* -------------------------------------------------------
       END OF CLASS
     ------------------------------------------------------- */
}





