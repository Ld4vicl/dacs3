package com.app.chefshare

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object UserManager {
    val currentUser: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser

    val displayName: String
        get() = currentUser?.displayName ?: "Ẩn danh"

    val email: String
        get() = currentUser?.email ?: "Không rõ"

    val photoUrl: String?
        get() = currentUser?.photoUrl?.toString()
}


fun bindUserInfo(
    context: Context,
    avatarView: ImageView?,
    nameView: TextView?,
    emailView: TextView?
) {
    val user = UserManager.currentUser

    nameView?.text = user?.displayName ?: "Ẩn danh"
    emailView?.text = user?.email ?: "Không rõ"

    val photoUrl = user?.photoUrl?.toString()
    if (!photoUrl.isNullOrEmpty() && avatarView != null) {
        Glide.with(context)
            .load(photoUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(avatarView)
    }
}