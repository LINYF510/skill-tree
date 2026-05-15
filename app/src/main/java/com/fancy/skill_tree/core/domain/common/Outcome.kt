package com.fancy.skill_tree.core.domain.common

/**
 * 统一的操作结果封装
 * 替代 kotlin.Result，提供更丰富的错误信息
 */
sealed class Outcome<out T> {
    /**
     * 操作成功
     * @param T 成功时返回的数据类型
     * @property data 成功返回的数据
     */
    data class Success<out T>(val data: T) : Outcome<T>()

    /**
     * 操作失败
     * @property exception 失败时的异常信息
     */
    data class Error(val exception: DomainException) : Outcome<Nothing>()
}
