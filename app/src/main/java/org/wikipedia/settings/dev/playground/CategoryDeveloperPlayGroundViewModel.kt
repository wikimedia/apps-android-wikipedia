package org.wikipedia.settings.dev.playground

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.time.Year
import java.util.UUID

class CategoryDeveloperPlayGroundViewModel : ViewModel() {
    private val categories = arrayOf(
        "Animals", "Science", "Technology", "History", "Geography",
        "Art", "Music", "Literature", "Sports", "Food",
        "Politics", "Religion", "Education", "Health", "Environment", "Fruit",
        "Vegetables", "Philosophy"
    )
    private val languages = arrayOf(
        "en", "zh-cn"
    )

    private val _categoryState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categoryState = _categoryState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val categories = AppDatabase.instance.categoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                _categoryState.value = UiState.Success(categories)
            }
        }
    }

    fun filterBy(year: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val categories = AppDatabase.instance.categoryDao().getCategoriesByTimeRange(
                startYear = year,
                endYear = year + 1
            )
            _categoryState.value = UiState.Success(categories)
        }
    }

    fun addTestData(context: Context, title: String, languageCode: String, year: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            try {
                val finalYear = if (year == Year.now().value) {
                    year - 1
                } else {
                    year
                }
                val category = Category(
                    month = 1,
                    year = finalYear,
                    title = title,
                    lang = languageCode,
                    count = 1
                )
                AppDatabase.instance.categoryDao().upsertAll(listOf(category))
                loadCategories()
            } catch (e: Exception) {
                L.e("Error generating sample data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addTestDataBulk(context: Context, size: Int = 10000, year: Int) {
        val finalYear = if (year == Year.now().value) {
            year - 1
        } else {
            year
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
                            title = "Category:${categories[randomCategoryIndex]}${UUID.randomUUID()}",
                            lang = languages[randomLanguageIndex],
                            year = finalYear,
                            month = 1,
                            count = 1
                        )
                    }

                    AppDatabase.instance.categoryDao().upsertAll(batch)

                    inserted += batch.size
                }
                loadCategories()
            } catch (e: Exception) {
                L.e("Error generating sample data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteAllCategories(context: Context) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            try {
                AppDatabase.instance.categoryDao().deleteOlderThanInBatch(Year.now().value + 1)
                loadCategories()
            } catch (e: Exception) {
                L.e("Error deleting categories", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteBeforeYear(context: Context, yearsAgo: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            try {
                AppDatabase.instance.categoryDao().deleteOlderThanInBatch(yearsAgo)
                loadCategories()
            } catch (e: Exception) {
                L.e("Error deleting categories data of $yearsAgo years ago", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
