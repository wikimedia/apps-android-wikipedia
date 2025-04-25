package org.wikipedia.settings.dev.playground

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.categories.db.Category
import org.wikipedia.categories.db.CategoryCount
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.endOfYearInMillis
import org.wikipedia.extensions.startOfYearInMillis
import org.wikipedia.util.UiState
import java.time.Year
import java.util.Calendar

class CategoryDeveloperPlayGroundViewModel : ViewModel() {
    private val TAG = "CategoryTable"

    private val categories = arrayOf(
        "Animals", "Science", "Technology", "History", "Geography",
        "Art", "Music", "Literature", "Sports", "Food",
        "Politics", "Religion", "Education", "Health", "Environment", "Fruit",
        "Vegetables", "Philosophy"
    )
    private val languages = arrayOf(
        "en", "zh-cn"
    )

    // UI state exposed to Compose
    private val _categoryCountState = MutableStateFlow<UiState<List<CategoryCount>>>(UiState.Success(listOf()))
    val categoryCountState = _categoryCountState.asStateFlow()

    private val _categoryState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categoryState = _categoryState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = AppDatabase.instance.categoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                _categoryState.value = UiState.Success(categories)
            }
        }
    }

    fun filterBy(year: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryCounts = AppDatabase.instance.categoryDao().getCategoriesByYearRange(
                startOfYear = year.startOfYearInMillis(),
                endOfYear = year.endOfYearInMillis()
            )
            _categoryCountState.value = UiState.Success(categoryCounts)
        }
    }

    fun addTestData(context: Context, title: String, languageCode: String, year: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentYear = Year.now().value
                val timeStamp = if (year == currentYear) {
                    System.currentTimeMillis()
                } else {
                    getPreviousYearMillis(currentYear - year)
                }
                AppDatabase.instance.categoryDao().insert(
                    Category(title, languageCode, timeStamp)
                )
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating sample data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addTestDataBulk(context: Context, size: Int = 10000, year: Int) {
        val currentYear = Year.now().value
        val timeStamp = if (year == currentYear) {
            System.currentTimeMillis()
        } else {
            getPreviousYearMillis(currentYear - year)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val batchSize = 4000
                var inserted = 0

                repeat(size / batchSize + if (size % batchSize > 0) 1 else 0) { batchNum ->
                    val currentBatchSize = minOf(batchSize, size - inserted)
                    if (currentBatchSize <= 0) return@repeat

                    val batch = (0 until currentBatchSize).map {
                        val randomCategoryIndex = (categories.indices).random()
                        val randomLanguageIndex = (languages.indices).random()
                        Category(
                            title = "Category:${categories[randomCategoryIndex]}",
                            lang = languages[randomLanguageIndex],
                            timeStamp = timeStamp
                        )
                    }

                    println("orange --> inserted ${batch.size} records to Category}")
                    AppDatabase.instance.categoryDao().insertAll(batch)

                    inserted += batch.size
                }
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating sample data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteAllCategories(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.instance.categoryDao().deleteAll()
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting categories", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteBeforeYear(context: Context, yearsAgo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.instance.categoryDao().deleteOlderThan(getPreviousYearMillis(yearsAgo))
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting categories data of $yearsAgo years ago", e)
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
