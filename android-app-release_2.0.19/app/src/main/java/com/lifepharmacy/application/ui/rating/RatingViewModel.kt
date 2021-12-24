package com.lifepharmacy.application.ui.rating

import android.app.Application
import androidx.databinding.ObservableField
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import com.lifepharmacy.application.base.BaseViewModel
import com.lifepharmacy.application.managers.AppManager
import com.lifepharmacy.application.managers.MediaManagerKT
import com.lifepharmacy.application.model.prescription.PrescriptionRequestBody
import com.lifepharmacy.application.network.performNwOperation
import com.lifepharmacy.application.repository.FileRepository
import com.lifepharmacy.application.repository.PrescriptionRepository
import com.lifepharmacy.application.enums.ImageUploadingType
import com.lifepharmacy.application.enums.RatingFragmentState
import com.lifepharmacy.application.model.orders.SubOrderItem
import com.lifepharmacy.application.repository.OrderRepository
import com.lifepharmacy.application.utils.universal.InputEditTextValidator
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import java.io.File

/**
 * Created by Zahid Ali
 */
class RatingViewModel
@ViewModelInject
constructor(
  val appManager: AppManager,
  private val repository: OrderRepository,
  application: Application
) : BaseViewModel(application) {
  var ratingState = MutableLiveData<RatingFragmentState>()
  var selectedSubOrderItem = MutableLiveData<SubOrderItem>()
  var mainRatingTag = MutableLiveData<String>()
  var showProducts = MutableLiveData<Boolean>()
  var isDriverRated = MutableLiveData<Boolean>()
  var isReviewDriver = MutableLiveData<Boolean>()

  var selectedTagPositions = ArrayList<Int>()

  var subOrderId: String? = ""
  var ratingMain: Float? = 1.0F


  fun getSubOrderDetail() =
    performNwOperation { repository.getSubOrderDetail(subOrderId ?: "") }

}
