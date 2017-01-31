package com.nibokapp.nibok.ui.adapter

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.nibokapp.nibok.R
import com.nibokapp.nibok.domain.model.BookInsertionModel
import com.nibokapp.nibok.extension.*
import com.nibokapp.nibok.ui.App
import kotlinx.android.synthetic.main.card_book.view.*
import kotlin.properties.Delegates

/**
 * Updatable adapter holding BookInsertionModel items.
 */
class InsertionAdapter() : RecyclerView.Adapter<InsertionAdapter.ViewHolder>(), UpdatableAdapter {

    companion object {
        private val TAG = InsertionAdapter::class.java.simpleName

        private const val KEY_IS_SAVED = "InsertionAdapter.KEY_IS_SAVED"
    }

    var items: List<BookInsertionModel> by Delegates.observable(emptyList()) {
        prop, oldItems, newItems ->
        Log.d(TAG, "Updating adapter items")
        update(oldItems, newItems,
                { o, n -> areItemsTheSame(o, n) },
                { o, n -> getChangePayload(o, n) })
    }

    private fun areItemsTheSame(old: BookInsertionModel, new: BookInsertionModel): Boolean {
        return old.getItemId() == new.getItemId()
    }

    private fun getChangePayload(old: BookInsertionModel, new: BookInsertionModel): Bundle? {
        val diffBundle = Bundle()
        if (old.savedByUser != new.savedByUser) {
            diffBundle.putBoolean(KEY_IS_SAVED, new.savedByUser)
        }
        return if (diffBundle.size() != 0) diffBundle else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.card_book))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {

        if (payloads.isEmpty()) {
            Log.d(TAG, "Full view holder binding required")
            onBindViewHolder(holder, position)
            return
        }

        val updateBundle = payloads[0] as? Bundle ?: return

        Log.d(TAG, "Updating view holder through payload: $updateBundle")
        val keys = updateBundle.keySet()
        keys.forEach {
            when (it) {
                KEY_IS_SAVED -> {
                    val isSaved = updateBundle.getBoolean(KEY_IS_SAVED)
                    holder.updateSaveButton(isSaved)
                }
            }
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "Binding view holder")
        holder.bind(items[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        companion object {
            const val MAX_AUTHORS = 2
        }

        fun updateSaveButton(isSaved: Boolean) {
            with(itemView) {
                val (colorFilter, imageResource) = if (isSaved) {
                    Pair(ContextCompat.getColor(context, R.color.primary),
                            R.drawable.ic_bookmark_black_24dp)
                } else {
                    Pair(ContextCompat.getColor(context, R.color.secondary_text),
                            R.drawable.ic_bookmark_border_black_24dp)
                }
                saveButton.apply {
                    setColorFilter(colorFilter)
                    setImageResource(imageResource)
                    if (isSaved) animateBounce()
                }
            }
        }

        fun bind(item: BookInsertionModel) {
            loadThumbnail(item.bookPictureSources.firstOrNull())
            bindTextData(item)
            addClickListeners()
        }

        private fun loadThumbnail(imageSource: String?) = with(itemView) {
            val source = imageSource ?: App.PLACEHOLDER_IMAGE_URL
            bookThumbnail.loadImage(source)
        }

        private fun bindTextData(item: BookInsertionModel) = with(itemView) {
            with(item) {

                with(bookInfo) {
                    Log.d(TAG, "Binding book data")
                    bookTitle.text = title
                    bookAuthor.text = authors.take(MAX_AUTHORS).joinToString("\n")
                    bookYear.text = year.toString()
                }

                bookCondition.toBookWearCondition(context).let {
                    Log.d(TAG, "Binding book wear condition")
                    bookQuality.text = it
                }

                Log.d(TAG, "Binding book price")
                bookPriceValue.text = bookPrice.toCurrency()
            }
        }

        private fun addClickListeners() {
            Log.d(TAG, "Adding click listeners")
            itemView.setOnClickListener {
                Log.d(TAG, "Card clicked")
            }
            itemView.bookThumbnail.setOnClickListener {
                Log.d(TAG, "Thumbnail clicked")
            }
        }
    }
}
