package com.example.Movify

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.Movify.databinding.ActivitySplashBinding
import com.example.Movify.view.LoginActivity // Your LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDisplayTimeMs: Long = 3000 // Total time splash is visible (adjust as needed)
    private val animationDurationMs: Long = 800 // Duration of slide animations (adjust as needed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide views initially to prepare for animation
        binding.imageView.visibility = View.INVISIBLE
        binding.splashTitle.visibility = View.INVISIBLE

        // Start animations after a short delay to ensure layout is complete
        // and views have their dimensions.
        binding.root.post {
            startSlideInAnimation()
        }

        // Navigate to LoginActivity after the defined splashDisplayTimeMs
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finish SplashActivity so user can't navigate back to it
        }, splashDisplayTimeMs)
    }

    private fun startSlideInAnimation() {
        // Get screen width for calculating off-screen positions
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels.toFloat()

        // --- Animate ImageView (from left) ---
        // Initially position ImageView off-screen to the left
        binding.imageView.translationX = -screenWidth
        binding.imageView.visibility = View.VISIBLE // Make it visible before animation starts

        val imageAnimator = ObjectAnimator.ofFloat(binding.imageView, "translationX", -screenWidth, 0f).apply {
            duration = animationDurationMs
            interpolator = AccelerateDecelerateInterpolator()
        }

        // --- Animate TextView (from right) ---
        // Initially position TextView off-screen to the right
        binding.splashTitle.translationX = screenWidth
        binding.splashTitle.visibility = View.VISIBLE // Make it visible before animation starts

        val titleAnimator = ObjectAnimator.ofFloat(binding.splashTitle, "translationX", screenWidth, 0f).apply {
            duration = animationDurationMs
            interpolator = AccelerateDecelerateInterpolator()
        }

        // --- Play animations together ---
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(imageAnimator, titleAnimator)
        animatorSet.start()
    }
}
