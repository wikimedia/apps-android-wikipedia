package org.wikipedia.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import java.time.Year

object CategoryTestUtil {
    private const val TAG = "CategoryTestUtil"
    private val currentYear = Year.now().value.toLong()
    private val categories = arrayOf(
        "Animals", "Science", "Technology", "History", "Geography",
        "Art", "Music", "Literature", "Sports", "Food",
        "Politics", "Religion", "Education", "Health", "Environment", "Fruit",
        "Vegetables", "Philosophy"
    )
    val languages = arrayOf(
        "en", "ar", "zh-cn", "ja", "de"
    )

    fun addTestData(context: Context, size: Int = 10) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val generatedCategories = (0 until size).map {
                    val randomCategoryIndex = (categories.indices).random()
                    val randomLanguageIndex = (languages.indices).random()
                    val randomCount = (1..50).random().toLong()
                    Category(
                        title = "Category:${categories[randomCategoryIndex]}",
                        lang = languages[randomLanguageIndex],
                        count = randomCount,
                        year = currentYear
                    )
                }
                generatedCategories.forEach {
                    AppDatabase.instance.categoryDao().upsert(it)
                }
                storeCategoriesToPreference()
                // Show success message on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Added $size categories", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating sample data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteAllCategories(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.instance.categoryDao().deleteAll()
                Prefs.yearInReviewData = ""
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Deleted all categories", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting categories", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun storeCategoriesToPreference() {
        val categories = AppDatabase.instance.categoryDao().getCategoriesByYear(currentYear)
        Prefs.yearInReviewData = categories.toString()
    }
}
