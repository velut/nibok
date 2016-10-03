package com.nibokapp.nibok.extension

import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.nibokapp.nibok.R

/**
 * View extensions file.
 *
 * Here are located extension functions defined over views.
 *
 */


/**
 * Inflate a layout directly in a view.
 *
 * @param layoutId the layout to inflate
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * Load an image, given its location, in this ImageView.
 *
 * @param imageLocation the location of the image, it can be an url a uri or a path
 * @param placeholderResourceId the id of the resource to be shown as a placeholder
 * @param errorResourceId the id of the resource to be shown if there was an error loading the image
 * @param animate true if animation should be used when loading, false otherwise. Default = true
 */
fun ImageView.loadImg(imageLocation: String,
                      placeholderResourceId: Int = R.drawable.book_placeholder_image,
                      errorResourceId: Int = R.drawable.book_placeholder_image,
                      animate: Boolean = true) {
    if (animate) {
        Glide.with(context)
                .load(imageLocation)
                .placeholder(placeholderResourceId)
                .error(errorResourceId)
                .into(this)
    } else {
        Glide.with(context)
                .load(imageLocation)
                .placeholder(placeholderResourceId)
                .error(errorResourceId)
                .dontAnimate()
                .into(this)
    }
}

/**
 * Animate a scaling with the given parameters.
 *
 */
fun View.animateScaling(xStart: Float, xEnd: Float, yStart: Float, yEnd: Float, duration: Long = 250) {
    val animation = ScaleAnimation(
            xStart, xEnd,
            yStart, yEnd,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f)
    animation.apply {
        fillAfter = true
        this.duration = duration
        interpolator = DecelerateInterpolator(0.5f)
    }
    this.startAnimation(animation)
}

fun View.animateScaleUp() {
    this.animateScaling(1f, 1.618f, 1f, 1.618f)
}

fun View.animateScaleDown() {
    this.animateScaling(1.618f, 1f, 1.618f, 1f)
}

/**
 * A scale up followed by a scale down animation to be called directly by a View.
 */
fun View.animateBounce() {
    this.apply {
        animateScaleUp()
        animateScaleDown()
    }
}

/**
 * Get the short name of a view.
 */
fun View.getName() = this.toString().substringAfter("app:id/").substringBefore('}')

/**
 * Hide the soft keyboard in this view.
 *
 * @param context the context needed to get system services
 */
fun View.hideSoftKeyboard(context: Context) {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

/**
 * Listener for keyboard hiding on tap.
 *
 * @param motionEvent
 * @param view the view that was tapped
 * @param context
 * @param eventHandled true if the motion event was completely handled,
 * false if it should be handled further. Default is false
 *
 * @return a Boolean signaling if the event should be further handled
 */
fun hideKeyboardListener(motionEvent: MotionEvent?, view: View,
                         context: Context,
                         eventHandled: Boolean = false): Boolean {
    val action = MotionEventCompat.getActionMasked(motionEvent)

    // If the view was tapped
    if (action == MotionEvent.ACTION_DOWN) {
        view.hideSoftKeyboard(context)
    }

    return eventHandled
}