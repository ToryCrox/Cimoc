package com.hiroshi.cimoc.smb.an

import android.util.Log
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * - Author: tory
 * - Date: 2022/6/3
 * - Description:
 */
class AnSmb(val config: Config) {

    private val client = SMBClient(
        SmbConfig.builder()
            // 设置读取超时
            .withTimeout(config.readTimeOut, TimeUnit.SECONDS)
            // 设置写入超时
            .withWriteTimeout(config.writeTimeOut, TimeUnit.SECONDS)
            // 设置Socket链接超时
            .withSoTimeout(config.soTimeOut, TimeUnit.SECONDS)
            .build()
    )

    private var _smbRoot: SmbRoot? = null

    suspend fun connectRoot(): LoadResult<SmbRoot> {
        return withContext(Dispatchers.IO) {
            try {
                val connection: Connection? = client.connect(config.ip)
                checkNotNull(connection)
                Log.i("AnSmb", "connectRoot serverName:${connection.connectionInfo.serverName}")

                val authContext = AuthenticationContext(config.userName, config.password.toCharArray(), null)
                val session = connection.authenticate(authContext)
                val connectShare = session.connectShare(config.folderName) as DiskShare
                Log.i("AnSmb", "connectRoot success")
                LoadResult.Success(SmbRoot(connection, session, connectShare))
            } catch (e: Exception) {
                Log.e("AnSmb", "connectRoot error", e)
                LoadResult.Error(exception = e)
            }
        }
    }

    suspend fun getOrConnectRoot(): SmbRoot? {
        return _smbRoot ?: connectRoot().successData?.also {
            _smbRoot = it
        }
    }

    suspend fun list(path: String): LoadResult<List<SmbFile>> {
        val smbRoot = getOrConnectRoot() ?: return LoadResult.Error()
        return try {
            val list = withContext(Dispatchers.IO) {
                smbRoot.connectShare.list(path)?.map {
                    SmbFile(info = it.fileAttributes, fileName = it.fileName, absPath = it.fileName,
                        EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY))
                }.orEmpty()
            }
            Log.d("AnSmb", "list: $list")
            LoadResult.Success(list)
        } catch (e: Exception) {
            Log.e("AnSmb", "list error", e)
            LoadResult.Error()
        }
    }

    fun close() {
        _smbRoot?.connection?.close()
        _smbRoot = null
    }

    class SmbRoot(
        val connection: Connection,
        val session: Session,
        val connectShare: DiskShare
    )

    data class SmbFile(
        val info: Any,
        val fileName: String,
        val absPath: String,
        val isDir: Boolean
    )

    class Config(
        val ip: String,
        val userName: String,
        val password: String,
        val folderName: String,
        val readTimeOut: Long = 10,
        val writeTimeOut: Long = 60,
        val soTimeOut: Long = 0
    )
}