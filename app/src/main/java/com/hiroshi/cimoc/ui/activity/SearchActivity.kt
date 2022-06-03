package com.hiroshi.cimoc.ui.activity

import com.hiroshi.cimoc.ui.activity.BackActivity
import android.widget.TextView.OnEditorActionListener
import butterknife.BindView
import com.hiroshi.cimoc.R
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.AppCompatCheckBox
import android.widget.ArrayAdapter
import com.hiroshi.cimoc.presenter.SearchPresenter
import com.hiroshi.cimoc.misc.Switcher
import com.hiroshi.cimoc.presenter.BasePresenter
import android.view.View.OnFocusChangeListener
import android.text.TextWatcher
import android.text.Editable
import com.hiroshi.cimoc.ui.adapter.AutoCompleteAdapter
import com.hiroshi.cimoc.ui.fragment.dialog.MultiDialogFragment
import com.hiroshi.cimoc.ui.activity.SearchActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import com.hiroshi.cimoc.component.DialogCaller
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import butterknife.OnClick
import com.hiroshi.cimoc.databinding.ActivitySearchBinding
import com.hiroshi.cimoc.manager.PreferenceManager
import com.hiroshi.cimoc.model.Source
import com.hiroshi.cimoc.utils.HintUtils
import com.hiroshi.cimoc.ui.activity.ResultActivity
import com.hiroshi.cimoc.ui.view.SearchView
import com.hiroshi.cimoc.utils.CollectionUtils
import com.hiroshi.cimoc.utils.StringUtils
import java.util.ArrayList

/**
 * Created by Hiroshi on 2016/10/11.
 */
class SearchActivity : BackActivity(), SearchView, OnEditorActionListener {


    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var mPresenter: SearchPresenter? = null
    private val mSourceList: MutableList<Switcher<Source>> = ArrayList()
    private var mAutoComplete = false
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
        binding.searchKeywordInput.setOnEditorActionListener(this)
        if (mAutoComplete) {
            mArrayAdapter = AutoCompleteAdapter(this)
            binding.searchKeywordInput.setAdapter(mArrayAdapter)
        }
        binding.searchActionButton.setOnClickListener {
            onSearchButtonClick()
        }
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
                fragment.show(fragmentManager, null)
                break
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

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            binding.searchActionButton.performClick()
            return true
        }
        return false
    }

    fun onSearchButtonClick() {
        val keyword = binding.searchKeywordInput.text.toString()
        val strictSearch = binding.searchStrictCheckbox.isChecked
        if (StringUtils.isEmpty(keyword)) {
            binding.searchTextLayout.error = getString(R.string.search_keyword_empty)
        } else {
            val list = ArrayList<Int>()
            for (switcher in mSourceList) {
                if (switcher.isEnable) {
                    list.add(switcher.element.type)
                }
            }
            if (list.isEmpty()) {
                HintUtils.showToast(this, R.string.search_source_none)
            } else {
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