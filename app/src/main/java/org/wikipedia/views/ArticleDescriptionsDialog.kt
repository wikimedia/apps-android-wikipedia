package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.databinding.DialogArticleDescriptionsBinding

class ArticleDescriptionsDialog constructor(context: Context) : AlertDialog(context) {

    private val binding = DialogArticleDescriptionsBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.closeButton.setOnClickListener { dismiss() }
    }
}
