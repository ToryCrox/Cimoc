package com.hiroshi.cimoc.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiroshi.cimoc.utils.mmkv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * - Author: tory
 * - Date: 2022/6/3
 * - Description:
 */
class SearchViewModel: ViewModel() {

    private var historyStore by mmkv("historyStore", "")

    private val _historyKeysState = MutableStateFlow<List<String>>(emptyList())
    val historyKeysState = _historyKeysState.asStateFlow()


    init {
        loadHistoryKeys()
    }

    private fun loadHistoryKeys() {
        viewModelScope.launch(Dispatchers.Main) {
            _historyKeysState.value = historyStore.split(",").toList()
        }
    }

    fun addSearchKeyword(keyword: String) {
        val newList = _historyKeysState.value.toMutableList()
        newList.remove(keyword)
        newList.add(0, keyword)
        _historyKeysState.value = newList
        historyStore = newList.joinToString(",")
    }

}