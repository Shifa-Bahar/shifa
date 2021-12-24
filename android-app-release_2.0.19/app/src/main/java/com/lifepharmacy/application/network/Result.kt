package com.lifepharmacy.application.network

import com.lifepharmacy.application.network.Result.Status.ERROR
import com.lifepharmacy.application.network.Result.Status.LOADING
import com.lifepharmacy.application.network.Result.Status.SUCCESS

data class Result<out T>(
  val status: Status,
  val data: T?,
  val message: String?
) {
  enum class Status {
    SUCCESS,
    ERROR,
    LOADING
  }

  companion object {
    fun <T> success(data: T): Result<T> {
      return Result(SUCCESS, data, null)
    }

    fun <T> error(
      message: String,
      data: T? = null
    ): Result<T> {
      return Result(ERROR, data, message)
    }

    fun <T> loading(data: T? = null): Result<T> {
      return Result(LOADING, data, null)
    }
  }
}