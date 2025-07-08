package com.example.Movify.view.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.Movify.R
import com.example.Movify.view.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val btnSignOut = view.findViewById<AppCompatButton>(R.id.btnSignOut)

        // Show current user's email
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "Unknown"
        tvUserEmail.text = "Email: $email"

        // Fetch and show current user's username from Firestore
        val userId = user?.uid
        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username")
                    if (!username.isNullOrBlank()) {
                        tvUserName.text = "Username: $username"
                    } else {
                        tvUserName.text = "Username not set"
                    }
                }
                .addOnFailureListener {
                    tvUserName.text = "Username: Unknown"
                }
        } else {
            tvUserName.text = "Username: Unknown"
        }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}