package com.lifepharmacy.application.model.review


import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RatingSubOrderRequest(
  @SerializedName("rating")
  var rating: Float? = 0F,
  @SerializedName("review")
  var review: String? = "",
  @SerializedName("sub_order_id")
  var subOrderId: Int? = 0,
  @SerializedName("tags")
  var tags: List<String>? = listOf()
) : Parcelable