package com.lifepharmacy.application.model.orders


import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import com.lifepharmacy.application.model.product.ProductDetails

@SuppressLint("ParcelCreator")
@Parcelize
data class OrderItem(
  var discount: Double = 0.0,
  var id: String = "",
  @SerializedName("line_total")
  var lineTotal: Double = 0.0,
  var price: Double = 0.0,
  @SerializedName("product_details")
  var productDetails: ProductDetails = ProductDetails(),
  @SerializedName("product_name")
  var productName: String = "",
  var qty: Int = 0,
  var sku: String = "",
  var tax: Double = 0.0,
  @SerializedName("gross_line_total")
  var grossLineTotal: Double = 0.0,
  @SerializedName("vat")
  var vat: Double = 0.0,
  @SerializedName("unit_price")
  var unitPrice: Double = 0.0,
  @SerializedName("net_line_total")
  var netLineTotal: Double = 0.0,
) : Parcelable