package com.lifepharmacy.application.ui.wallet.viewmodels

import android.app.Application
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseViewModel
import com.lifepharmacy.application.managers.AppManager
import com.lifepharmacy.application.model.payment.TransactionModel
import com.lifepharmacy.application.model.payment.CardMainModel
import com.lifepharmacy.application.network.performNwOperation
import com.lifepharmacy.application.repository.WalletRepository
import com.lifepharmacy.application.utils.universal.Extensions.stringToNullSafeDouble
import com.lifepharmacy.application.utils.universal.InputEditTextValidator

/**
 * Created by Zahid Ali
 */
class TopViewModel
@ViewModelInject
constructor(
  private val appManager: AppManager,
  private val repository: WalletRepository,
  application: Application
) : BaseViewModel(application) {


  lateinit var amount: InputEditTextValidator
  var amountMut = MutableLiveData<String>()
  var cardMainModel = CardMainModel()
  var viaNewCard = false

  init {
    initializeValidations()
  }

  fun topUp(body: TransactionModel) =
    performNwOperation { repository.createTransaction(body) }

  fun getTransactionModer(): TransactionModel {
    val transactionModel =
      TransactionModel()
    transactionModel.type = "charge"
    transactionModel.purpose = "wallet_recharge"
    if (viaNewCard) {
      transactionModel.CardType = "new"
    } else {
      transactionModel.CardType = "saved"
      transactionModel.card_id = cardMainModel.id

    }
    transactionModel.method = "card"
    transactionModel.amount = amount.getValue().stringToNullSafeDouble()
    return transactionModel
  }

  private fun initializeValidations() {
    amount = InputEditTextValidator(
      InputEditTextValidator.InputEditTextValidationsEnum.AMOUNT,
      true,
      object : InputEditTextValidator.InputEditTextValidatorCallBack {
        override fun onValueChange(validator: InputEditTextValidator?) {
          if (validator != null) {
            amountMut.value = validator.getValue()
          }
        }
      },
      viewModelContext.getString(R.string.amount_error)
    )
  }
}