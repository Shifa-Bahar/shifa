package com.lifepharmacy.application.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.lifepharmacy.application.model.CartAll
import com.lifepharmacy.application.model.config.Config
import com.lifepharmacy.application.model.User
import com.lifepharmacy.application.model.WishListAll
import com.lifepharmacy.application.model.address.AddressMainModel
import com.lifepharmacy.application.model.cart.OffersAll
import com.lifepharmacy.application.model.category.CategoryMainModel
import com.lifepharmacy.application.model.category.RootCategoryMainModel
import com.lifepharmacy.application.model.home.HomeMainModel
import com.lifepharmacy.application.model.home.HomeResponseItem
import com.lifepharmacy.application.model.vouchers.VoucherMainResponse
import com.lifepharmacy.application.utils.universal.ConstantsUtil
import com.onesignal.OneSignal
import javax.inject.Inject

class PersistenceManager @Inject constructor(val application: Context) {
  var preference: SharedPreferences =
    application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)

  fun isLoggedIn(): Boolean {
    return preference.contains(ConstantsUtil.SH_USER)
  }

  fun isReviewBoxOpened(): Boolean {
    return preference.getBoolean(ConstantsUtil.SH_REVIEW_BOX, false)
  }

  fun setReviewBoxStatus(reviewBox: Boolean) {
    preference.edit().putBoolean(ConstantsUtil.SH_REVIEW_BOX, reviewBox).apply()
  }

//  fun saveFCMToken(token: String) {
//    preference.edit().putString(ConstantsUtil.SH_FCM_TOKEN, token).apply()
//  }

  fun getFCMToken(): String? {
    return preference.getString(ConstantsUtil.SH_FCM_TOKEN, "")
  }

  fun saveToken(token: String) {
    preference.edit().putString(ConstantsUtil.SH_AUTH_TOKEN, token).apply()
  }


  fun getCompaignId(): String? {
    return preference.getString(ConstantsUtil.SH_COMPAIN_ID, "")
  }

  fun saveCompianID(compaingId: String) {
    preference.edit().putString(ConstantsUtil.SH_COMPAIN_ID, compaingId).apply()
  }


  fun setState(boolean: Boolean) {
    preference.edit().putBoolean(ConstantsUtil.IS_MAIN_ACTIVITY_IN_FOREGROUND, boolean).commit()
  }

  fun isInForeground(): Boolean {
    return preference.getBoolean(ConstantsUtil.IS_MAIN_ACTIVITY_IN_FOREGROUND, false)
  }

  fun getUUID(): String? {
    return preference.getString(ConstantsUtil.SH_UUID, "")
  }

  fun saveUUID(uuid: String) {
    preference.edit().putString(ConstantsUtil.SH_UUID, uuid).apply()
  }

  fun isThereUUID(): Boolean {
    return preference.contains(ConstantsUtil.SH_UUID)
  }


  fun getToken(): String? {
    return preference.getString(ConstantsUtil.SH_AUTH_TOKEN, null)
  }

  fun saveLoggedInUser(user: User) {
    OneSignal.setExternalUserId("${user.id}")
    val gson = Gson()
    val jsonStr = gson.toJson(user)
    preference.edit().putString(ConstantsUtil.SH_USER, jsonStr).apply()
  }

  fun getLoggedInUser(): User? {
    val gson = Gson()
    return gson.fromJson(preference.getString(ConstantsUtil.SH_USER, null), User::class.java)
  }

  fun saveCartList(cart: CartAll) {
    val gson = Gson()
    val jsonStr = gson.toJson(cart)
    preference.edit().putString(ConstantsUtil.SH_CART, jsonStr).apply()
  }

  private fun isThereCartItems(): Boolean {
    return preference.contains(ConstantsUtil.SH_OFFERS_CART)
  }

  fun getCartList(): CartAll? {
    val gson = Gson()
    return gson.fromJson(preference.getString(ConstantsUtil.SH_CART, null), CartAll::class.java)
  }

  fun saveOffersCartList(cart: OffersAll) {
    val gson = Gson()
    val jsonStr = gson.toJson(cart)
    preference.edit().putString(ConstantsUtil.SH_OFFERS_CART, jsonStr).apply()
  }

  fun getOffersCartList(): OffersAll? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_OFFERS_CART, null),
      OffersAll::class.java
    )
  }

  fun saveWishList(cart: WishListAll) {
    val gson = Gson()
    val jsonStr = gson.toJson(cart)
    preference.edit().putString(ConstantsUtil.SH_WISH_LIST, jsonStr).apply()
  }

  fun getWishList(): WishListAll? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_WISH_LIST, null),
      WishListAll::class.java
    )
  }

  fun saveLatLong(latLng: LatLng) {
    preference.edit().putString(ConstantsUtil.SH_LAT, latLng.latitude.toString()).apply()
    preference.edit().putString(ConstantsUtil.SH_LONG, latLng.longitude.toString()).apply()
  }

  fun getLatLong(): String? {
    return preference.getString(
      ConstantsUtil.SH_LAT,
      "0.0"
    ) + "," + preference.getString(ConstantsUtil.SH_LONG, "0.0")
  }

  fun getLat(): String? {
    return preference.getString(
      ConstantsUtil.SH_LAT,
      "0.0"
    )
  }

  fun getLong(): String? {
    return preference.getString(
      ConstantsUtil.SH_LONG,
      "0.0"
    )
  }

  fun saveCountry(country: String?) {
    preference.edit().putString(ConstantsUtil.SH_Country, country ?: "ae").apply()
  }

  fun getCountry(): String {
    return preference.getString(ConstantsUtil.SH_Country, "ae").toString()
  }

  fun saveCartID(cartID: String) {
    preference.edit().putString(ConstantsUtil.SH_CART_ID, cartID).apply()
  }

  fun getCartID(): String {
    return preference.getString(ConstantsUtil.SH_CART_ID, "0").toString()
  }

  fun isThereCart(): Boolean {
    return preference.contains(ConstantsUtil.SH_CART_ID)
  }

  fun saveLastPayment(payment: String) {
    preference.edit().putString(ConstantsUtil.SH_LAST_PAYMENT, payment).apply()
  }

  fun getLastPayment(): String {
    return preference.getString(ConstantsUtil.SH_LAST_PAYMENT, "cash").toString()
  }

  fun isPaymentMethod(): Boolean {
    return preference.contains(ConstantsUtil.SH_LAST_PAYMENT)
  }

  fun saveCurrency(currency: String) {
    preference.edit().putString(ConstantsUtil.SH_CURRENCY, currency).apply()
  }

  fun getCurrency(): String {
    return preference.getString(ConstantsUtil.SH_CURRENCY, "AED") + ""
  }

  fun clearPrefs() {
    preference.edit().clear().apply()
  }

  fun clearUserData() {
    preference.edit().remove(ConstantsUtil.SH_USER).commit()
    preference.edit().remove(ConstantsUtil.SH_AUTH_TOKEN).commit()
  }

  fun clearCartID() {
    if (isThereCart()) {
      preference.edit().remove(ConstantsUtil.SH_CART_ID).commit()
    }
    if (isThereCartItems()) {
      preference.edit().remove(ConstantsUtil.SH_OFFERS_CART).commit()
    }
  }

  fun saveAddressList(address: AddressMainModel) {
    val gson = Gson()
    val jsonStr = gson.toJson(address)
    preference.edit().putString(ConstantsUtil.SH_ADDRESS, jsonStr).apply()
  }

  fun getAddressList(): AddressMainModel? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_ADDRESS, null),
      AddressMainModel::class.java
    )
  }

  fun saveLang(country: String) {
    preference.edit().putString(ConstantsUtil.SH_LANGUAGE, country).apply()
  }

  fun getLang(): String? {
    return preference.getString(ConstantsUtil.SH_LANGUAGE, null)
  }

  fun saveConfig(config: Config) {
    val gson = Gson()
    val jsonStr = gson.toJson(config)
    preference.edit().putString(ConstantsUtil.SH_CONFIG, jsonStr).apply()
  }

  fun getConfig(): Config? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_CONFIG, null),
      Config::class.java
    )
  }

  fun saveVouchers(data: VoucherMainResponse) {
    val gson = Gson()
    val jsonStr = gson.toJson(data)
    preference.edit().putString(ConstantsUtil.SH_VOUCHER, jsonStr).apply()
  }

  fun getVouchers(): VoucherMainResponse? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_VOUCHER, null),
      VoucherMainResponse::class.java
    )
  }

  fun saveHomeData(data: HomeMainModel) {
    val gson = Gson()
    val jsonStr = gson.toJson(data)
    preference.edit().putString(ConstantsUtil.SH_HOME, jsonStr).apply()
  }

  fun getHomeData(): HomeMainModel? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_HOME, null),
      HomeMainModel::class.java
    )
  }

  fun saveCategoryRoot(data: RootCategoryMainModel) {
    val gson = Gson()
    val jsonStr = gson.toJson(data)
    preference.edit().putString(ConstantsUtil.SH_ROOT_CAT, jsonStr).apply()
  }

  fun getCategoryRoot(): RootCategoryMainModel? {
    val gson = Gson()
    return gson.fromJson(
      preference.getString(ConstantsUtil.SH_ROOT_CAT, null),
      RootCategoryMainModel::class.java
    )
  }

  fun saveLottieURL(country: String) {
    preference.edit().putString(ConstantsUtil.SH_LOTTIE_URL, country).apply()
  }

  fun getLottieUrl(): String? {
    return preference.getString(ConstantsUtil.SH_LOTTIE_URL, "")
  }

  fun saveCurrentFileToken(country: String) {
    preference.edit().putString(ConstantsUtil.SH_LOTTIE_FILE_TOKE, country).apply()
  }

  fun getCurrentFileToken(): String? {
    return preference.getString(ConstantsUtil.SH_LOTTIE_FILE_TOKE, "")
  }


  fun saveAskPermissionOpenDateTime(dateTime: String?) {
    preference.edit().putString(ConstantsUtil.SH_ASK_PERMISSION_DATE_TIME, dateTime).apply()
  }

  fun getAskPermissionDateTime(): String? {
    return preference.getString(ConstantsUtil.SH_ASK_PERMISSION_DATE_TIME, "")
  }
}
