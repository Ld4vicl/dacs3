package com.app.chefshare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var edtFullName: EditText
    private lateinit var imgAvatar: CircleImageView
    private lateinit var edtUserId: EditText
    private lateinit var edtEmail: EditText
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        edtFullName = findViewById(R.id.edtFullName)
        imgAvatar = findViewById(R.id.imgAvatar)
        edtUserId = findViewById(R.id.edtUserId)
        edtEmail = findViewById(R.id.edtEmail)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            edtFullName.setText(user.displayName)
            edtEmail.setText(user.email)
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar)

            // Lấy ID từ Firestore và bỏ tiền tố @ce_
            usersRef.document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val fullId = doc.getString("id")
                    val shortId = fullId?.removePrefix("@ce_") ?: ""
                    edtUserId.setText(shortId)
                }
        }

        imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val name = edtFullName.text.toString().trim()
            val rawId = edtUserId.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!rawId.matches(Regex("^[a-zA-Z0-9]{1,10}$"))) {
                Toast.makeText(this, "ID phải gồm 1–10 ký tự chữ hoặc số", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val customId = "@ce_$rawId"

            user?.let { currentUser ->
                // Kiểm tra ID đã tồn tại chưa
                usersRef.whereEqualTo("id", customId).get()
                    .addOnSuccessListener { documents ->
                        val isDuplicate = documents.any { it.id != currentUser.uid }
                        if (isDuplicate) {
                            Toast.makeText(this, "ID này đã được sử dụng!", Toast.LENGTH_SHORT).show()
                        } else {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .apply {
                                    selectedImageUri?.let { setPhotoUri(it) }
                                }
                                .build()

                            currentUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        usersRef.document(currentUser.uid)
                                            .update("id", customId)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Lỗi khi lưu ID", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(this, "Lỗi khi cập nhật hồ sơ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi kiểm tra ID", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imgAvatar.setImageURI(selectedImageUri)
        }
    }
}
