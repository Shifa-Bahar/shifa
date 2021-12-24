package com.lifepharmacy.application.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import com.kaopiz.kprogresshud.KProgressHUD
import com.lifepharmacy.application.R
import com.lifepharmacy.application.managers.AlertManager
import com.lifepharmacy.application.managers.AppManager
import com.lifepharmacy.application.utils.universal.ConstantsUtil
import com.lifepharmacy.application.utils.universal.ConstantsUtil.PERMISSION_REQUEST_CODE
import com.lifepharmacy.application.utils.universal.Logger
import java.util.*
import javax.inject.Inject

abstract class BaseActivity<DB : ViewDataBinding> : AppCompatActivity() {

  var Permisions = true

  lateinit var permissions: Array<String>
  var mainRequestCode: Int? = null

  private var dailog: KProgressHUD? = null
  lateinit var binding: DB
  var langauge: String = "en"
  @Inject
  lateinit var appManager: AppManager

  /**
   * Getting Layout Instance
   */
  @LayoutRes
  abstract fun getLayoutRes(): Int


  abstract fun getLoadingLayout(): ConstraintLayout


  //Getting Loading Layout Instance
//    abstract fun getProgressBar(): Int

  //Getting Status bar Color Instance
//    abstract fun getStatusColor(): Int

  //Getting Status bar Color Instance
//    abstract fun iconColor(): Boolean
  abstract fun permissionGranted(requestCode: Int)

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    langauge = appManager.persistenceManager.getLang()?:"non"
    if(langauge == "non"){
      langauge=  Locale.getDefault().language
    }
    Logger.d("language",langauge)
    val locale = Locale(langauge)
    Locale.setDefault(locale)
    val config = Configuration()
    config.locale = locale
    baseContext.resources
      .updateConfiguration(config, baseContext.resources.displayMetrics)
    val window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    window.statusBarColor = ContextCompat.getColor(
      this,
      R.color.white
    )
    if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermission()
    }
    binding = DataBindingUtil.setContentView(this, getLayoutRes())
    appManager.persistenceManager.saveLang("en")
    appManager.loadingState.getLoadingState().observe(this, Observer {
      it?.let {
        if (it) {
          showLoading()
//          getLoadingLayout().visibility = View.VISIBLE
        } else {
          stopLoading()
//          getLoadingLayout().visibility = View.GONE
        }
      }
    })

  }


  fun onExit() {
    finish()
  }

  open fun showLoading() {
    window.setFlags(
      WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
      WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    );
    getLoadingLayout().visibility = View.VISIBLE
//    showDailog()
  }

  open fun stopLoading() {
    try {
      window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
      getLoadingLayout().visibility = View.GONE
    } catch (e: Exception) {

    }

//    dismisDailog()
  }


  /////REQUEST PERMISSIONS///////////////////
  private fun requestPermission() {
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
        ActivityCompat.requestPermissions(
          this,
          ConstantsUtil.RequiredPermissionsAboveQ,
          PERMISSION_REQUEST_CODE
        )
      }
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
        ActivityCompat.requestPermissions(
          this,
          ConstantsUtil.RequiredPermissionsAboveP,
          PERMISSION_REQUEST_CODE
        )
      }
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
        ActivityCompat.requestPermissions(
          this,
          ConstantsUtil.RequiredPermissions,
          PERMISSION_REQUEST_CODE
        )
      }
    }
  }


  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String?>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    when (requestCode) {
      mainRequestCode -> {
        if (grantResults.isNotEmpty()) {
          var i = 0
          while (i < grantResults.size) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
              Permisions = false
              AlertManager.showErrorMessage(this,"Please Provide Permissions to use this feature")
            }
            i++
          }
          if (Permisions){
            permissionGranted(requestCode)
          }
        }
      }
    }
  }

  fun requestSpecificPermission(requestCode: Int?, permissions: Array<String>) {
    mainRequestCode = requestCode
    this.permissions = permissions

    if (mainRequestCode == ConstantsUtil.PERMISSION_LOCATIONS_REQUEST_CODE) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.permissions = ConstantsUtil.RequiredPermissionsLocationsAboveQ

      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this.permissions = ConstantsUtil.RequiredPermissionsLocationsAboveP

      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.permissions = ConstantsUtil.RequiredPermissionsLocations
      }
    }

    if (!checkPermissionStatus(permissions)) {
      requestCode?.let {
        requestPermissions(
          this.permissions,
          it
        )
      }
    } else {
      requestCode?.let {
        permissionGranted(it)
      }
    }


  }

  fun checkPermissionStatus(permissions: Array<String>): Boolean {
    var has = false
    var cases = 1
    for (item in permissions) {
      if ((ContextCompat.checkSelfPermission(
          this,
          item
        ) == PackageManager.PERMISSION_GRANTED)
      ) {
        cases++
      }
    }

    if (cases == permissions.size) {
      has = true
    }

    return has
  }

  fun showMessageOKCancel(
    message: String,
    okListener: (Any, Any) -> Unit,
    cancelListener: (Any, Any) -> Unit
  ) {
    AlertDialog.Builder(this@BaseActivity)
      .setMessage(message)
      .setPositiveButton("OK", okListener)
      .setNegativeButton("Cancel", cancelListener)
      .create()
      .show()
  }

  fun dismisDailog() {
    dailog?.dismiss()
  }

  fun showDailog() {
    dailog = KProgressHUD.create(this@BaseActivity)
      .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
      .setCancellable(false)
      .setAnimationSpeed(2)
      .setDimAmount(0.1f)
      .show()
  }
}