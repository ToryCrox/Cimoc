package com.hiroshi.cimoc.smb

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hiroshi.cimoc.databinding.FragmentSmbListBinding
import com.hiroshi.cimoc.smb.an.AnSmb
import com.hiroshi.cimoc.smb.an.succeeded
import kotlinx.coroutines.launch
import me.jingbin.smb.BySMB

/**
 * - Author: tory
 * - Date: 2022/6/3
 * - Description:
 */
class SmbListFragment: Fragment() {

    lateinit var binding: FragmentSmbListBinding

    private var anSmb: AnSmb? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSmbListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        anSmb = AnSmb(AnSmb.Config(
            ip = "192.168.2.193",
            userName = "tory",
            password = " ",
            folderName = "E"
        ))
        viewLifecycleOwner.lifecycleScope.launch {
            val r = anSmb?.list("BaiduNetdiskDownload")?.succeeded
            Log.i("AnSmb", "onCreate list: $r")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        anSmb?.close()
    }
}