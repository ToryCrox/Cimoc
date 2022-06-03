package com.hiroshi.cimoc.ui.activity

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import com.hiroshi.cimoc.R
import android.widget.ArrayAdapter
import com.hiroshi.cimoc.presenter.SearchPresenter
import com.hiroshi.cimoc.misc.Switcher
import com.hiroshi.cimoc.presenter.BasePresenter
import android.view.View.OnFocusChangeListener
import android.text.TextWatcher
import android.text.Editable
import com.hiroshi.cimoc.ui.adapter.AutoCompleteAdapter
import com.hiroshi.cimoc.ui.fragment.dialog.MultiDialogFragment
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import com.hiroshi.cimoc.component.DialogCaller
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.hiroshi.cimoc.databinding.ActivitySearchBinding
import com.hiroshi.cimoc.manager.PreferenceManager
import com.hiroshi.cimoc.model.Source
import com.hiroshi.cimoc.utils.HintUtils
import com.hiroshi.cimoc.ui.view.SearchView
import com.hiroshi.cimoc.utils.CollectionUtils
import com.hiroshi.cimoc.utils.StringUtils
import com.hiroshi.cimoc.vm.SearchViewModel
import com.tory.module_adapter.utils.dp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.ArrayList

/**
 * Created by Hiroshi on 2016/10/11.
 */
class SearchActivity : BackActivity(), SearchView {

    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var mPresenter: SearchPresenter? = null
    private val mSourceList: MutableList<Switcher<Source>> = ArrayList()
    private var mAutoComplete = false

    private val viewModel: SearchViewModel by viewModels()

    override fun initPresenter(): BasePresenter<*> {
        val presenter = SearchPresenter()
        mPresenter = presenter
        presenter.attachView(this)
        return presenter
    }

    lateinit var binding: ActivitySearchBinding

    override fun createContentView(layoutId: Int) {
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initView() {
        mAutoComplete = mPreference.getBoolean(PreferenceManager.PREF_SEARCH_AUTO_COMPLETE, false)
        binding.searchKeywordInput.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (!binding.searchActionButton.isShown) {
                binding.searchActionButton.show()
            }
        }
        binding.searchKeywordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                binding.searchTextLayout.error = null
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (mAutoComplete) {
                    val keyword = binding.searchKeywordInput.text.toString()
                    if (!StringUtils.isEmpty(keyword)) {
                        mPresenter!!.loadAutoComplete(keyword)
                    }
                }
            }
        })
        binding.searchKeywordInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.searchActionButton.performClick()
                true
            } else false
        }
        if (mAutoComplete) {
            mArrayAdapter = AutoCompleteAdapter(this)
            binding.searchKeywordInput.setAdapter(mArrayAdapter)
        }
        binding.searchActionButton.setOnClickListener {
            val keyword = binding.searchKeywordInput.text.toString().trim()
            doSearch(keyword)
        }

        initHistory()
    }

    private fun initHistory() {
        binding.searchHistory.lineSpacing = 10.dp()
        binding.searchHistory.itemSpacing = 10.dp()
        viewModel.historyKeysState.onEach {
            updateHistoryUI(it)
        }.launchIn(lifecycleScope)
    }

    private fun updateHistoryUI(keys: List<String>) {

        binding.searchHistory.removeAllViews()
        keys.forEach { key->
            val textView = createItem()
            textView.text = key
            textView.setOnClickListener {
                doSearch(key)
            }
            binding.searchHistory.addView(textView)
        }
    }

    private fun createItem(): TextView {
        val textView = AppCompatTextView(this)
        textView.background = GradientDrawable().also {
            it.setColor(0xfff1f2f3.toInt())
            it.cornerRadius = 4.dp().toFloat()
        }
        textView.setPadding(6.dp())
        textView.setTextColor(Color.BLACK)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        return textView
    }

    override fun initData() {
        mPresenter?.loadSource()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_menu_source -> if (!mSourceList.isEmpty()) {
                val size = mSourceList.size
                val arr1 = arrayOfNulls<String>(size)
                val arr2 = BooleanArray(size)
                var i = 0
                while (i < size) {
                    arr1[i] = mSourceList[i].element.title
                    arr2[i] = mSourceList[i].isEnable
                    ++i
                }
                val fragment = MultiDialogFragment.newInstance(
                    R.string.search_source_select,
                    arr1,
                    arr2,
                    DIALOG_REQUEST_SOURCE
                )
                fragment.show(supportFragmentManager, null)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_SOURCE -> {
                val check = bundle.getBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
                if (check != null) {
                    val size = mSourceList.size
                    var i = 0
                    while (i < size) {
                        mSourceList[i].isEnable = check[i]
                        ++i
                    }
                }
            }
        }
    }

    fun doSearch(keyword: String) {
        val strictSearch = binding.searchStrictCheckbox.isChecked
        if (keyword.isBlank()) {
            binding.searchTextLayout.error = getString(R.string.search_keyword_empty)
        } else {
            val list = mSourceList.filter { it.isEnable }.map { it.element.type }
            if (list.isEmpty()) {
                HintUtils.showToast(this, R.string.search_source_none)
            } else {
                viewModel.addSearchKeyword(keyword)
                startActivity(
                    ResultActivity.createIntent(
                        this, keyword, strictSearch,
                        CollectionUtils.unbox(list), ResultActivity.LAUNCH_MODE_SEARCH
                    )
                )
            }
        }
    }

    override fun onAutoCompleteLoadSuccess(list: List<String>) {
        mArrayAdapter!!.clear()
        mArrayAdapter!!.addAll(list)
    }

    override fun onSourceLoadSuccess(list: List<Source>) {
        hideProgressBar()
        for (source in list) {
            mSourceList.add(Switcher(source, true))
        }
    }

    override fun onSourceLoadFail() {
        hideProgressBar()
        HintUtils.showToast(this, R.string.search_source_load_fail)
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.comic_search)
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_search
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    companion object {
        private const val DIALOG_REQUEST_SOURCE = 0
    }
}