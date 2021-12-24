package com.lifepharmacy.application.ui.outofstockorder.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseBottomWithLoading
import com.lifepharmacy.application.databinding.BottomSheetOrderSummaryBinding
import com.lifepharmacy.application.enums.PaymentType
import com.lifepharmacy.application.managers.AlertManager
import com.lifepharmacy.application.managers.analytics.*
import com.lifepharmacy.application.model.general.GeneralResponseModel
import com.lifepharmacy.application.model.orders.OrderResponseModel
import com.lifepharmacy.application.model.orders.PlaceOrderRequest
import com.lifepharmacy.application.model.payment.TransactionModel
import com.lifepharmacy.application.model.payment.Urls
import com.lifepharmacy.application.network.Result
import com.lifepharmacy.application.ui.address.AddressViewModel
import com.lifepharmacy.application.ui.cart.adapter.CartShipmentProductAdapter
import com.lifepharmacy.application.ui.cart.viewmodel.CartViewModel
import com.lifepharmacy.application.ui.pages.fragment.PageFragment
import com.lifepharmacy.application.ui.payment.WebViewPaymentActivity
import com.lifepharmacy.application.ui.wallet.viewmodels.TopViewModel
import com.lifepharmacy.application.ui.wallet.viewmodels.WalletViewModel
import com.lifepharmacy.application.utils.universal.ConstantsUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.math.roundToInt
import com.google.android.play.core.splitinstall.d
import com.lifepharmacy.application.databinding.BottomSheetOrderSummaryOutStockBinding
import com.lifepharmacy.application.model.orders.outofstock.OutOfStockRequestBody
import com.lifepharmacy.application.ui.address.AddressSelectionActivity
import com.lifepharmacy.application.ui.outofstockorder.OutOfStockOrderViewModel


/**
 * Created by Zahid Ali
 */
@AndroidEntryPoint
class OrderSummaryOutOfStockBottomSheet :
  BaseBottomWithLoading<BottomSheetOrderSummaryOutStockBinding>(),
  ClickOrderOutOfStockSummarySheet {
  private val viewModel: OutOfStockOrderViewModel by activityViewModels()
  private val addressViewModel: AddressViewModel by activityViewModels()
  private val walletVieModel: WalletViewModel by activityViewModels()
  private val topViewModel: TopViewModel by activityViewModels()

  private var animator: ValueAnimator? = null

  private var isTopUp = false
  private var isAnimationCanceled = false
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    isCancelable = true
    initLayout()
    observers()

  }

  companion object {
    var inView = false
  }

  private val addressContract =
    registerForActivityResult(AddressSelectionActivity.Contract()) { result ->
      result?.address.let {
        addressViewModel.deliveredAddressMut.value = it
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    inView = true
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog: Dialog = super.onCreateDialog(savedInstanceState)
    dialog.setOnShowListener(DialogInterface.OnShowListener { dialogInterface ->
      val bottomSheetDialog = dialogInterface as BottomSheetDialog
//      setupFullHeight(bottomSheetDialog)
      val bottomSheet =
        bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
      val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)
      behavior.state = BottomSheetBehavior.STATE_EXPANDED
    })
    return dialog
  }

  private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
    val bottomSheet = bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
    val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)
    val layoutParams = bottomSheet.layoutParams
    val windowHeight = getWindowHeight()
    if (layoutParams != null) {
      layoutParams.height = (windowHeight * 0.85).roundToInt()
    }
    bottomSheet.layoutParams = layoutParams
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
  }

  private fun getWindowHeight(): Int {
    // Calculate window height for fullscreen use
    val displayMetrics = DisplayMetrics()
    requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
  }

  private fun initLayout() {
    setTermsText()
    binding.click = this
    binding.viewModel = viewModel
    binding.addressViewModel = addressViewModel
    binding.lifecycleOwner = this
    binding.lyCard.item = walletVieModel.selectedCard
    viewModel.showLaodingOrderProgressBar.value = false

  }

  override fun onDismiss(dialog: DialogInterface) {
    cancelAnimation()
    super.onDismiss(dialog)
    inView = false
  }

  override fun onCancel(dialog: DialogInterface) {
    cancelAnimation()
    super.onCancel(dialog)
    inView = false
  }

  private fun setTermsText() {
    val text =
      "<font color=#707070>${getString(R.string.by_placing_order_terms)} </font> <font color=#365FC9> <b>  ${
        getString(
          R.string.terms_and_condition_small
        )
      }</b></font>"

    val html = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY);
    binding.tvTerms.text = html
  }

  private fun observers() {

  }

  override fun getLayoutRes(): Int {
    return R.layout.bottom_sheet_order_summary_out_stock
  }

  override fun permissionGranted(requestCode: Int) {
    if (requestCode == ConstantsUtil.PERMISSION_READ_SMS) {
    }
  }


  override fun getLoadingLayout(): ConstraintLayout {
    return binding.llLoading.clLoading
  }

  private fun placeOrderRequest() {
    if (addressViewModel.deliveredAddressMut.value?.isAddressValid() == true) {
      addressViewModel.deliveredAddressMut.value?.id?.let {
        viewModel.makeOutOfStockRequestBody(
          it
        )
      }?.let { placeOrderRequestOrder ->
        when {
          viewModel.orderId == 0 -> {
            when (viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)) {
              "wallet_with_less", "new_for_wallet" -> {
                isTopUp = true
                createOrder(placeOrderRequestOrder)
              }
              else -> {
                isTopUp = false
                createOrder(placeOrderRequestOrder)
              }
            }
          }
          isTopUp -> {
            createTopUpTransaction()
          }
          else -> {
            when (viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)) {
              "wallet_with_less", "new_for_wallet" -> {
                isTopUp = true
                createTopUpTransaction()
              }
              else -> {
                isTopUp = false
                createTransaction()
              }
            }

          }
        }
      }
    } else {
      MaterialAlertDialogBuilder(
        requireActivity(),
        R.style.ThemeOverlay_App_MaterialInfoDialog
      )
        .setTitle("Invalid Address")
        .setMessage("Some of the fields of selected address is not valid please edit and try again ")
        .setPositiveButton(getString(R.string.ok)) { dialog, which ->

          findNavController().navigate(R.id.nav_address)
        }
        .show()
    }
  }

  private fun createOrder(placeOrderRequestOrder: OutOfStockRequestBody) {
    viewModel.creatOutOfStockOrder(placeOrderRequestOrder)
      .observe(viewLifecycleOwner, {
        it?.let {
          when (it.status) {
            Result.Status.SUCCESS -> {
              hideLoading()
              it?.data?.data?.let { it1 -> handleCreateOrderResult(it1) }
            }
            Result.Status.ERROR -> {
              it.message?.let { it1 ->
                AlertManager.showErrorMessage(
                  requireActivity(),
                  it1
                )
              }
              hideLoading()
              viewModel.appManager.analyticsManagers.checkOutFailed()
            }
            Result.Status.LOADING -> {
              showLoading()
            }
          }
        }

      })
  }

  private fun handleCreateOrderResult(response: OrderResponseModel) {
    viewModel.appManager.analyticsManagers.orderCreated(response)
    response.orderId?.let { displayID ->
      viewModel.orderDisplayId = displayID
    }
    response.id?.let { orderId ->
      viewModel.orderId = orderId
      if (isTopUp) {
        viewModel.setWalletDifference(response.total)
        viewModel.totalAmount.value = response.total
        createTopUpTransaction()
      } else {
        createTransaction()
      }
    }

  }

  private fun createTopUpTransaction() {
    isTopUp = true
    topViewModel.amount.setValue(viewModel.walletDifference.value.toString())
    topViewModel.topUp(topViewModel.getTransactionModer())
      .observe(viewLifecycleOwner, {
        it?.let {
          when (it.status) {
            Result.Status.SUCCESS -> {
              hideLoading()
              it.data?.data?.let { it1 -> handleTopUpTransaction(it1) }
            }
            Result.Status.ERROR -> {
              hideLoading()
              it.message?.let { it1 ->
                AlertManager.showErrorMessage(
                  requireActivity(),
                  it1
                )
              }
              viewModel.appManager.analyticsManagers.transactionFailed()
            }
            Result.Status.LOADING -> {
              showLoading()
            }
          }
        }

      })
  }

  private fun createTransaction() {
    viewModel.createTransaction(viewModel.getTransactionModer(walletVieModel.selectedCard?.id))
      .observe(viewLifecycleOwner,
        {
          it?.let {
            when (it.status) {
              Result.Status.SUCCESS -> {
                hideLoading()
                it.data?.let { it1 ->
                  handleCreateTransactionForOrder(it1)
                }
              }
              Result.Status.ERROR -> {
                it.message?.let { it1 ->
                  AlertManager.showErrorMessage(
                    requireActivity(),
                    it1
                  )
                }
                viewModel.appManager.analyticsManagers.transactionFailed()
                hideLoading()
              }
              Result.Status.LOADING -> {
                showLoading()
              }
            }
          }

        })
  }

  private fun handleTopUpTransaction(result: TransactionModel) {
    isTopUp = false
    when (viewModel.selectedPaymentType.value) {
      PaymentType.CASH -> {
      }
      PaymentType.CARD, PaymentType.NEW_FOR_WALLET -> {
        if (topViewModel.viaNewCard) {
          result.urls?.let { it1 ->
            openActivityForPayment(it1)
          }
        } else {
          if (result?.status == 0) {
            AlertManager.showErrorMessage(
              requireActivity(),
              getString(R.string.transaction_failed)
            )
          } else {
            viewModel.selectedPaymentType.value = PaymentType.WALLET
            placeOrderRequest()
          }
        }
      }
      PaymentType.WALLET -> {
      }
      PaymentType.WALLET_WITH_LESS -> {
        if (result?.status == 0) {
          AlertManager.showErrorMessage(
            requireActivity(),
            getString(R.string.transaction_failed)
          )
        } else {
          viewModel.selectedPaymentType.value = PaymentType.WALLET
          placeOrderRequest()
        }
      }
      else -> {

      }
    }
  }

  private fun handleCreateTransactionForOrder(result: GeneralResponseModel<TransactionModel>) {
    result?.data?.let {
      when (viewModel.selectedPaymentType.value) {
        PaymentType.CASH -> {
          orderPlace(result.data)
        }
        PaymentType.CARD -> {
          if (it.status == 0) {
            AlertManager.showErrorMessage(
              requireActivity(),
              getString(R.string.transaction_failed)
            )
          } else {
            orderPlace(result.data)
          }
        }
        PaymentType.NEW -> {
          it.urls?.let { it1 -> openActivityForPayment(it1) }
        }
        PaymentType.WALLET -> {
          orderPlace(result.data)
        }

        else -> {
          if (it.status == 0) {
            result.message.let { it1 ->
              AlertManager.showErrorMessage(
                requireActivity(),
                it1
              )
            }
          } else {
            orderPlace(result.data)
          }
        }

      }
    }
  }


  private fun orderPlace(transactionModel: TransactionModel?) {

    //OrderPlaced
    findNavController().navigate(R.id.orderPlacedDialog)

  }

  private fun openActivityForPayment(urls: Urls) {
    val intent = Intent(requireActivity(), WebViewPaymentActivity::class.java)
    intent.putExtra("paymentURL", urls.paymentUrl)
    intent.putExtra("successURL", urls.successUrl)
    intent.putExtra("failURL", urls.failUrl)
    startActivityForResult(intent, ConstantsUtil.PAYMENT_ACTIVITY_REQUEST_CODE)
//    WebViewPaymentActivity.open(requireActivity(), urls)
  }

  @SuppressLint("Recycle")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      when (requestCode) {
        ConstantsUtil.PAYMENT_ACTIVITY_REQUEST_CODE -> {
          val status = (data ?: return).getIntExtra("status", 0)
          if (status == 1) {
            viewModel.appManager.analyticsManagers.checkOutCompleted()
            when (viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)) {
              "wallet_with_less", "new_for_wallet" -> {
                viewModel.selectedPaymentType.value = PaymentType.WALLET
                createTransaction()
              }
              else -> {
                orderPlace(null)
              }
            }
          } else {
            isTopUp = when (viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)) {
              "wallet_with_less", "new_for_wallet" -> {
                true
              }
              else -> {
                false
              }
            }
            viewModel.appManager.analyticsManagers.transactionFailed()
            AlertManager.showErrorMessage(
              requireActivity(),
              getString(R.string.payment_failed)
            )
          }
        }
      }
    }
  }

  private fun animateProgressBar() {
    animator = ValueAnimator.ofInt(0, binding.pbLoading.max)
    animator?.duration = 3000
    animator?.addUpdateListener { animation ->
      binding.pbLoading.progress = animation.animatedValue as Int
    }
    animator?.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        // start your activity here
        if (!isAnimationCanceled) {
          placeOrderRequest()
        }
      }
    })
    animator?.start()
  }

  private fun cancelAnimation() {
    viewModel.showLaodingOrderProgressBar.value = false
    isAnimationCanceled = true
    animator?.cancel()
  }

  override fun onClickProceed() {
    when (viewModel.selectedPaymentType.value) {
      PaymentType.CARD -> {
        animateProgressBar()
        isAnimationCanceled = false
        viewModel.showLaodingOrderProgressBar.value = true
      }
      else -> {
        placeOrderRequest()
      }
    }

  }

  override fun onClickTerms() {
    findNavController().navigate(
      R.id.pageFragment,
      PageFragment.getPageFragmentBundle("terms-and-conditions")
    )
  }

  override fun onClickCancel() {
    cancelAnimation()

  }

  override fun onClickClose() {
    findNavController().navigateUp()
  }

}