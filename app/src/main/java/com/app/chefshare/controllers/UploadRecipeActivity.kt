package com.app.chefshare

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.databinding.ActivityUploadRecipeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class UploadRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadRecipeBinding
    private var selectedMainImageUri: Uri? = null
    private val stepImageUris = mutableListOf<MutableList<Uri>>() // << nhiều ảnh mỗi bước
    private var currentStepIndex = -1
    private lateinit var mainImagePicker: ActivityResultLauncher<Intent>
    private lateinit var stepImagePicker: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImagePickers()
        setupEventListeners()

        addIngredientField()
        addStepView()

        updateOldRecipesToAddLikesField()
    }

    private fun setupEventListeners() {
        binding.imageContainer.setOnClickListener { pickMainImage() }
        binding.btnAddIngredient.setOnClickListener { addIngredientField() }
        binding.btnAddStep.setOnClickListener { addStepView() }
        binding.btnBack.setOnClickListener { finish() }
//        binding.btnSaveDraft.setOnClickListener { uploadRecipe(isDraft = true) }
        binding.btnPublish.setOnClickListener { uploadRecipe(isDraft = false) }
    }

    private fun setupImagePickers() {
        mainImagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri ->
                selectedMainImageUri = uri
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgRecipe.setImageBitmap(bitmap)
                binding.imgRecipe.visibility = ImageView.VISIBLE
            }
        }

        stepImagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val clipData = result.data?.clipData
            val uriData = result.data?.data

            if (currentStepIndex in stepImageUris.indices) {
                val container = binding.stepsContainer.getChildAt(currentStepIndex * 2 + 1) as FrameLayout
                container.removeAllViews()

                val imageContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }

                stepImageUris[currentStepIndex].clear()

                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        stepImageUris[currentStepIndex].add(uri)
                        imageContainer.addView(createImageView(uri))
                    }
                } else if (uriData != null) {
                    stepImageUris[currentStepIndex].add(uriData)
                    imageContainer.addView(createImageView(uriData))
                }

                container.addView(imageContainer)
            }
        }
    }

    private fun pickMainImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        mainImagePicker.launch(intent)
    }

    private fun pickStepImage(index: Int) {
        currentStepIndex = index
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        stepImagePicker.launch(Intent.createChooser(intent, "Chọn ảnh cho bước"))
    }

    private fun addIngredientField() {
        val editText = EditText(this).apply {
            hint = "Ví dụ: 2 quả trứng gà"
            setBackgroundResource(R.drawable.bg_rounded_border_grey)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 0) }
        }
        binding.ingredientContainer.addView(editText)
    }

    private fun addStepView() {
        val stepIndex = stepImageUris.size + 1

        val stepLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 0) }
        }

        val stepLabel = TextView(this).apply {
            text = stepIndex.toString()
            setTextColor(resources.getColor(android.R.color.white, null))
            setBackgroundResource(R.drawable.bg_circle_red)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(90, 90).apply {
                setMargins(0, 0, 16, 0)
            }
        }

        val stepEditText = EditText(this).apply {
            hint = "Ví dụ: Trộn đều các nguyên liệu"
            setBackgroundResource(R.drawable.bg_rounded_border_grey)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                weight = 1f
            }
        }

        stepLayout.addView(stepLabel)
        stepLayout.addView(stepEditText)

        val stepImageFrame = FrameLayout(this).apply {
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
            ).apply { setMargins(0, 8, 0, 16) }
        }

        val chooseImageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#EEEEEE"))

            val icon = ImageView(this@UploadRecipeActivity).apply {
                setImageResource(R.drawable.ic_add_a_photo)
                setColorFilter(Color.parseColor("#555555"))
                layoutParams = LinearLayout.LayoutParams(64, 64)
            }

            val label = TextView(this@UploadRecipeActivity).apply {
                text = "Chọn ảnh"
                setTextColor(Color.parseColor("#555555"))
                textSize = 14f
                setPadding(0, 8, 0, 0)
                gravity = Gravity.CENTER
            }

            addView(icon)
            addView(label)
        }

        stepImageFrame.addView(chooseImageLayout)

        val indexToPick = stepImageUris.size
        stepImageFrame.setOnClickListener { pickStepImage(indexToPick) }

        stepImageUris.add(mutableListOf())

        binding.stepsContainer.addView(stepLayout)
        binding.stepsContainer.addView(stepImageFrame)
    }

    private fun createImageView(uri: Uri): ImageView {
        val imageView = ImageView(this)
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        imageView.setImageBitmap(bitmap)
        imageView.layoutParams = LinearLayout.LayoutParams(300, 300).apply {
            setMargins(8, 8, 8, 8)
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
    }

    private fun uploadRecipe(isDraft: Boolean) {
        val name = binding.etRecipeName.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val portion = binding.etPortion.text.toString().trim()
        val time = binding.etCookingTime.text.toString().trim()

        if (name.isEmpty() || selectedMainImageUri == null) {
            Toast.makeText(this, "Vui lòng nhập tên món và chọn ảnh đại diện!", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredients = mutableListOf<String>()
        for (i in 0 until binding.ingredientContainer.childCount) {
            val view = binding.ingredientContainer.getChildAt(i)
            if (view is EditText) {
                val text = view.text.toString().trim()
                if (text.isNotEmpty()) ingredients.add(text)
            }
        }

        // Upload main image
        uploadImageToCloudinary(selectedMainImageUri!!, { mainImageUrl ->

            val allStepsData = mutableListOf<Map<String, Any>>()

            fun uploadStepImages(index: Int) {
                if (index >= stepImageUris.size) {
                    // DONE
                    saveRecipeToFirestore(name, desc, portion, time, ingredients, allStepsData, mainImageUrl, isDraft)
                    return
                }

                val stepLayout = binding.stepsContainer.getChildAt(index * 2) as LinearLayout
                val stepEdit = stepLayout.getChildAt(1) as EditText
                val stepText = stepEdit.text.toString().trim()

                val uriList = stepImageUris[index]
                val urlList = mutableListOf<String>()

                fun uploadEachImage(i: Int) {
                    if (i >= uriList.size) {
                        val stepMap = mapOf(
                            "text" to stepText,
                            "images" to urlList
                        )
                        allStepsData.add(stepMap)
                        uploadStepImages(index + 1)
                        return
                    }

                    uploadImageToCloudinary(uriList[i], { url ->
                        urlList.add(url)
                        uploadEachImage(i + 1)
                    }, {
                        Toast.makeText(this, "Lỗi upload ảnh bước ${index + 1}", Toast.LENGTH_SHORT).show()
                    })
                }

                uploadEachImage(0)
            }

            uploadStepImages(0)

        }, {
            Toast.makeText(this, "Lỗi upload ảnh chính", Toast.LENGTH_SHORT).show()
        })
    }

    private fun saveRecipeToFirestore(
        name: String,
        desc: String,
        portion: String,
        time: String,
        ingredients: List<String>,
        steps: List<Map<String, Any>>,
        mainImageUrl: String,
        isDraft: Boolean
    ) {
        val collection = if (isDraft) "drafts" else "recipes"
        val docId = FirebaseFirestore.getInstance().collection(collection).document().id
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val data = hashMapOf(
            "id" to docId,
            "name" to name,
            "description" to desc,
            "portion" to portion,
            "cookingTime" to time,
            "ingredients" to ingredients,
            "steps" to steps,
            "mainImage" to mainImageUrl,
            "userId" to userId,
            "createdAt" to createdAt
        )

        FirebaseFirestore.getInstance().collection(collection).document(docId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Lưu công thức thành công!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, RecipeDetailActivity::class.java)
                intent.putExtra("recipeId", docId)
                startActivity(intent)

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi lưu công thức!", Toast.LENGTH_SHORT).show()
            }
    }


    private fun uploadImageToCloudinary(uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File.createTempFile("temp_image", ".jpg", cacheDir)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", "chefshare") // đã chỉnh
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/degs4hlsq/image/upload")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cloudinary", "❌ Upload ảnh thất bại: ${e.message}")
                runOnUiThread { onError(e.message ?: "Upload failed") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody ?: "")
                    val imageUrl = json.getString("secure_url")
                    Log.d("Cloudinary", "✅ Upload thành công: $imageUrl")
                    runOnUiThread { onSuccess(imageUrl) }
                } else {
                    Log.e("Cloudinary", "❌ Upload lỗi code ${response.code}: $responseBody")
                    runOnUiThread { onError("Upload failed with code ${response.code}") }
                }
            }
        })
    }

    private fun updateOldRecipesToAddLikesField() {
        val db = FirebaseFirestore.getInstance()
        db.collection("recipes")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val data = doc.data
                    if (data != null && !data.containsKey("likes")) {
                        doc.reference.update("likes", emptyList<String>())
                            .addOnSuccessListener {
                                Log.d("UpdateLikesField", "✅ Updated recipe: ${doc.id}")
                            }
                            .addOnFailureListener {
                                Log.e("UpdateLikesField", "❌ Failed to update: ${doc.id}", it)
                            }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("UpdateLikesField", "❌ Failed to fetch recipes", it)
            }
    }

}
