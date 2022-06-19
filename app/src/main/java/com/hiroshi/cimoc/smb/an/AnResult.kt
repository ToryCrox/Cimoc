package com.hiroshi.cimoc.smb.an

/**
 * - Author: tory
 * - Date: 2022/6/3
 * - Description:
 */
sealed class LoadResult<out R> {
    data class Success<out T>(
        val data: T,
    ) : LoadResult<T>()

    data class Error(
        val code: Int = -1,
        val msg: String? = null,
        val exception: Exception? = null
    ) : LoadResult<Nothing>()

    object Loading : LoadResult<Nothing>()
}

val LoadResult<*>?.succeeded
    get() = this is LoadResult.Success && data != null
val <T> LoadResult<T>?.successData
    get() = if (this is LoadResult.Success) data else null

val LoadResult<*>?.error: Boolean
    get() = this is LoadResult.Error
val LoadResult<*>?.loading: Boolean
    get() = this == null || this == LoadResult.Loading
val LoadResult<*>?.errorMsg: String?
    get() = if (this is LoadResult.Error) msg else null

inline val LoadResult<*>?.isLoading: Boolean
    get() = this == null || this == LoadResult.Loading
inline val LoadResult<*>?.isError: Boolean
    get() = this is LoadResult.Error
val LoadResult<*>?.isSucceeded
    get() = this is LoadResult.Success && data != null