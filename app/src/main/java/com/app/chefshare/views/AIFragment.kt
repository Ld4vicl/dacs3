package com.app.chefshare.views

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.app.chefshare.BuildConfig
import com.app.chefshare.R
import com.app.chefshare.api.OpenAIApi
import com.app.chefshare.models.ChatMessage
import com.app.chefshare.models.ChatRequest
import com.app.chefshare.models.ChatResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


class AIFragment : Fragment(R.layout.fragment_ai) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        val btnGenerate = view.findViewById<Button>(R.id.btnGenerate)
        val tvRecipeResult = view.findViewById<TextView>(R.id.tvRecipeResult)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        val edtIngredients = view.findViewById<EditText>(R.id.edtIngredients)
        val edtDiet = view.findViewById<EditText>(R.id.edtDiet)
        val spinnerPortion = view.findViewById<Spinner>(R.id.spinnerPortion)
        val spinnerCuisine = view.findViewById<Spinner>(R.id.spinnerCuisine)
        val spinnerLevel = view.findViewById<Spinner>(R.id.spinnerLevel)

        btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Setup spinner data
        val portionOptions = listOf("1 người", "2 người", "4 người", "6 người")
        val cuisineOptions = listOf("Việt Nam", "Trung Quốc", "Hàn Quốc", "Nhật Bản", "Âu", "Mỹ")
        val levelOptions = listOf("Dễ", "Trung bình", "Khó")

        spinnerPortion.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, portionOptions)
        spinnerCuisine.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cuisineOptions)
        spinnerLevel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, levelOptions)

        btnGenerate.setOnClickListener {
            val ingredients = edtIngredients.text.toString()
            val diet = edtDiet.text.toString()
            val portion = spinnerPortion.selectedItem.toString()
            val cuisine = spinnerCuisine.selectedItem.toString()
            val level = spinnerLevel.selectedItem.toString()

            if (ingredients.isBlank()) {
                Toast.makeText(requireContext(), "Vui lòng nhập nguyên liệu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvRecipeResult.text = "⏳ Đang tạo công thức..."
            tvRecipeResult.visibility = View.VISIBLE

            callAIRecipeGenerator(ingredients, portion, cuisine, level, diet) { recipe ->
                tvRecipeResult.text = recipe
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun callAIRecipeGenerator(
        ingredients: String,
        portion: String,
        cuisine: String,
        level: String,
        diet: String,
        onResult: (String) -> Unit
    ) {
        val userPrompt = """
        Hãy tạo công thức nấu ăn chi tiết dựa trên:
        - Nguyên liệu: $ingredients
        - Khẩu phần: $portion
        - Ẩm thực: $cuisine
        - Mức độ: $level
        - Chế độ ăn: $diet
    """.trimIndent()

        val messages = listOf(
            ChatMessage("system", "Bạn là đầu bếp AI tạo công thức nấu ăn chi tiết."),
            ChatMessage("user", userPrompt)
        )

        val request = ChatRequest("openai/gpt-3.5-turbo", messages)

        val gson = Gson()
        val json = gson.toJson(request)
        val mediaType = "application/json".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val retrofit = Retrofit.Builder()
            //.baseUrl("https://api.openai.com/") //openAI
            .baseUrl("https://openrouter.ai/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenAIApi::class.java)
        //openrouter
        val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"

        val call = api.generateRecipe(apiKey, requestBody)

        call.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content
                    onResult(reply ?: "Không có phản hồi từ AI.")
                } else {
                    onResult("❌ Lỗi API: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                onResult("❌ Lỗi kết nối: ${t.localizedMessage}")
            }
        })
    }
}
