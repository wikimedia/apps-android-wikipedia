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
import org.wikipedia.extensions.endOfYearInMillis
import org.wikipedia.extensions.startOfYearInMillis
import java.util.Calendar
import java.util.UUID

object CategoryTestUtil {
    private const val TAG = "CategoryTestUtil"
    private const val MILLION = 1000000
    private val years = arrayOf(System.currentTimeMillis(), getPreviousYearMillis(1))
    private val categories = arrayOf(
        "Animals", "Science", "Technology", "History", "Geography",
        "Art", "Music", "Literature", "Sports", "Food",
        "Politics", "Religion", "Education", "Health", "Environment", "Fruit",
        "Vegetables", "Philosophy"
    )
    val languages = arrayOf(
        "en", "ar", "zh-cn", "ja", "de"
    )

    fun addTestData(context: Context, size: Int = Int.MAX_VALUE) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val generatedCategories = (0 until size).map {
                    val randomCategoryIndex = (categories.indices).random()
                    val randomLanguageIndex = (languages.indices).random()
                    val randomYearIndex = (years.indices).random()
                    Category(
                        title = "Category:${categories[randomCategoryIndex]}",
                        lang = languages[randomLanguageIndex],
                        timeStamp = years[randomYearIndex]
                    )
                }
                generatedCategories.forEach {
                    AppDatabase.instance.categoryDao().insert(it)
                }
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

    fun addTestDataBulk(context: Context, size: Int = 10000) { // Use a reasonable default
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val batchSize = 4000
                var inserted = 0

                repeat(size / batchSize + if (size % batchSize > 0) 1 else 0) { batchNum ->
                    val currentBatchSize = minOf(batchSize, size - inserted)
                    if (currentBatchSize <= 0) return@repeat

                    val batch = (0 until currentBatchSize).map {
                        val randomCategoryIndex = (categories.indices).random()
                        val randomLanguageIndex = (languages.indices).random()
                        val randomYearIndex = (years.indices).random()
                        val uniqueId = UUID.randomUUID().toString().substring(0, 8)
                        Category(
                            title = "Category:${categories[randomCategoryIndex]}$uniqueId",
                            lang = languages[randomLanguageIndex],
                            timeStamp = System.currentTimeMillis()
                        )
                    }

                    println("orange --> inserted ${batch.size} records to Category}")
                    AppDatabase.instance.categoryDao().insertAll(batch)

                    inserted += batch.size
                }

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

    fun getCategoriesByYearRange(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val year = 2025
                val startOfYear = year.startOfYearInMillis()
                val endOfYear = year.endOfYearInMillis()
                val categories = AppDatabase.instance.categoryDao().getCategoriesByYearRange(
                    startOfYear = startOfYear,
                    endOfYear = endOfYear
                )
                println("orange --> year $categories")
                println("orange --> year size ${categories.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving CategoriesByYearRange", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getPreviousYearMillis(yearsAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -yearsAgo)
        return calendar.timeInMillis
    }
}
