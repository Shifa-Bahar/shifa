package com.lifepharmacy.application.ui.products.fragment


import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.facebook.appevents.AppEventsConstants
import com.lifepharmacy.application.Constants
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseFragment
import com.lifepharmacy.application.databinding.FragmentProductBinding
import com.lifepharmacy.application.enums.AddressChanged
import com.lifepharmacy.application.enums.AddressSelection
import com.lifepharmacy.application.enums.ProductDetailSelectedState
import com.lifepharmacy.application.enums.ShipmentType
import com.lifepharmacy.application.interfaces.*
import com.lifepharmacy.application.managers.AlertManager
import com.lifepharmacy.application.managers.analytics.productScreenOpen
import com.lifepharmacy.application.managers.analytics.productViewed
import com.lifepharmacy.application.model.product.ProductDetail
import com.lifepharmacy.application.model.product.ProductDetails
import com.lifepharmacy.application.network.Result
import com.lifepharmacy.application.ui.address.AddressSelectionActivity
import com.lifepharmacy.application.ui.address.AddressViewModel
import com.lifepharmacy.application.ui.dashboard.adapter.*
import com.lifepharmacy.application.ui.home.viewModel.HomeViewModel
import com.lifepharmacy.application.ui.productList.ProductListFragment
import com.lifepharmacy.application.ui.products.adapter.*
import com.lifepharmacy.application.ui.products.viewmodel.ProductViewModel
import com.lifepharmacy.application.utils.PricesUtil
import com.lifepharmacy.application.utils.RatingUtil
import com.lifepharmacy.application.utils.universal.ConstantsUtil
import com.lifepharmacy.application.utils.universal.IntentAction
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class ProductFragment : BaseFragment<FragmentProductBinding>(),
  ClickProductFragment,
  ClickAddToCart, ClickSearchBarProduct, ClickHomeProduct, ClickDiscounts, Review, AddOnProduct,
  ClickProductReview, ClickLayoutDeliveryAddress {

//  private val args:Prod

  companion object {
    fun getBundle(productID: String, position: Int): Bundle {
      val bundle = Bundle()
      bundle.putString("id", productID)
      bundle.putInt("position", position)
      return bundle
    }
  }


  private val viewModel: ProductViewModel by activityViewModels()
  private val homeViewModel: HomeViewModel by activityViewModels()
  private val viewModelAddress: AddressViewModel by activityViewModels()
  private lateinit var discountAdapter: DiscountAdapter
  private lateinit var homeProductAdapter: HomeProductAdapter
  private lateinit var addOnProductAdapter: AddOnProductAdapter
  private lateinit var reviewAdapter: ReviewsAdapter
  private lateinit var ratingsAdapter: RatingsAdapter
  private var productID: String? = null
  private var slug: String? = null
  private var position: Int? = 0
  private var productDetailsGlobal: ProductDetail? = null
  private val addressContract =
    registerForActivityResult(AddressSelectionActivity.Contract()) { result ->
      result?.address?.let {
        viewModelAddress.deliveredAddressMut.value = it
        viewModelAddress.addressSelection = AddressSelection.NON
      }
    }


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    viewModel.appManager.analyticsManagers.productScreenOpen()

    arguments?.let {
      productID = it.getString("id")
      slug = it.getString("slug")
      position = it.getInt("position", 0)
      viewModel.appManager.analyticsManagers.productScreenOpen(productID.toString())
    }
    if (mView == null) {
      mView = super.onCreateView(inflater, container, savedInstanceState)
      viewModel.mainProductMut.value = null
      initUI()
      initRV()
      observers()

    }
    addressChangeObserver()
    return mView
  }

  fun initUI() {
    binding.click = this
    binding.viewModel = viewModel
    binding.layoutProduct.viewModel = viewModel
    binding.layoutProduct.lifecycleOwner = this
    binding.layoutOverview.viewModel = viewModel
    binding.layoutOverview.lifecycleOwner = this
    binding.lifecycleOwner = this
    binding.layoutAddToCart.click = this
    binding.layoutOverview.lyReviews.click = this
    binding.layoutProduct.click = this
    binding.layoutAddress.click = this
    binding.layoutSuper.txtViewViewAll.visibility = View.GONE
    binding.layoutSuper.title = getString(R.string.customer_also_viewed)
    binding.layoutSuper.showTitle = true
    binding.layoutAddress.mViewModel = viewModelAddress
    binding.layoutAddress.lifecycleOwner = this
    binding.layoutAddress.productViewModel = viewModel
    binding.layoutAddress.lifecycleOwner = this
    binding.layoutAddons.click = this
    binding.layoutSearch.click = this
    binding.layoutProduct.tvOrignalPrice.paintFlags =
      binding.layoutProduct.tvOrignalPrice.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
    binding.layoutAddons.amount = viewModel.totalAmount
    viewModel.selectedDetails.value = ProductDetailSelectedState.OVERVIEW
    binding.layoutSearch.isBackVisible = true
  }


  private fun initRV() {
    homeProductAdapter =
      HomeProductAdapter(
        requireActivity(),
        this,
        appManager = viewModel.appManager,
        viewModel.appManager.storageManagers.config.backOrder ?: "Pre-Order"
      )
    binding.layoutSuper.recyclerViewProducts.adapter = homeProductAdapter

//        discountAdapter = DiscountAdapter(requireActivity(), this)
//        binding.recyclerViewDiscounts.adapter = discountAdapter

    addOnProductAdapter = AddOnProductAdapter(requireActivity(), this)
    binding.layoutAddons.recyclerViewProducts.adapter = addOnProductAdapter

    reviewAdapter = ReviewsAdapter(
      requireActivity(),
      appManager.storageManagers.config.noCommentLable ?: "No Comment"
    )
    binding.layoutOverview.lyReviews.rvReviews.adapter = reviewAdapter
  }

  private fun updateUI(productData: ProductDetail) {
    productDetailsGlobal = productData
    viewModel.unitPrice =
      PricesUtil.getUnitPrice(productData.productDetails.prices, requireContext())
    binding.layoutAddToCart.qty = viewModel.qty
    binding.layoutAddress.qty = viewModel.qty

    viewModel.isInWishList.value =
      viewModel.appManager.wishListManager.checkIfItemExistInWishList(productData.productDetails)
    binding.layoutAddToCart.item = productData.productDetails
    viewModel.qty.set(1.0)
    binding.layoutAddons.price = productData.productDetails
    binding.layoutOverview.productDetails = productData
    binding.layoutAddress.productModel = productData.productDetails
    binding.layoutOverview.lyRating.productDetails = productData
    binding.layoutProduct.productModel = productData.productDetails
    binding.layoutProduct.price =
      PricesUtil.getRelativePrice(productData.productDetails.prices, requireContext())
    homeProductAdapter.setDataChanged(productData.relatedProducts)
    reviewAdapter.setDataChanged(productData.productReviews)
    setRatings(productData)
    productData.productDetails.shortDescription.let { updateProductDescription(it) }

    viewModel.initAmount(productData.relatedProducts)
    addOnProductAdapter.selectedItems = viewModel.addedProducts
    addOnProductAdapter.setDataChanged(productData.frequentlyBroughtTogether)

    binding.layoutProduct.imageCarouselProduct.addData(viewModel.getProductSliderList(productData.productDetails))
    binding.layoutProduct.customIndicator.let {
      binding.layoutProduct.imageCarouselProduct.setIndicator(
        it
      )
    }
    setAnalytics()
  }

  private fun setAnalytics() {
    productDetailsGlobal?.productDetails?.let {
      viewModel.appManager.analyticsManagers.productViewed(
        it,
        position ?: 0
      )
    }
  }

  private fun updateProductDescription(description: String?) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        binding.layoutOverview.lyTitle.tvOverviewTitle.text =
          Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
      } else {
        binding.layoutOverview.lyTitle.tvOverviewTitle.text = (Html.fromHtml(description))
      }
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
    }

  }

  @SuppressLint("SetTextI18n")
  private fun setRatings(productData: ProductDetail) {
    binding.layoutProduct.rating = productData.productRating
    if (productData.productRating.rating.toFloat() > 0) {
      binding.layoutOverview.lyRating.ratingBar.rating = productData.productRating.rating.toFloat()
    }
    binding.layoutOverview.lyRating.lyRatingChart.pgFive.progress =
      RatingUtil.calculateRatingFor5(productData.productRating)
    binding.layoutOverview.lyRating.lyRatingChart.progressBar4.progress =
      RatingUtil.calculateRatingFor4(productData.productRating)
    binding.layoutOverview.lyRating.lyRatingChart.progressBar3.progress =
      RatingUtil.calculateRatingFor3(productData.productRating)
    binding.layoutOverview.lyRating.lyRatingChart.progressBar2.progress =
      RatingUtil.calculateRatingFor2(productData.productRating)
    binding.layoutOverview.lyRating.lyRatingChart.progressBar1.progress =
      RatingUtil.calculateRatingFor1(productData.productRating)
    binding.layoutOverview.lyRating.lyRatingChart.tv5.text =
      RatingUtil.calculateRatingFor5(productData.productRating).toString() + " %"
    binding.layoutOverview.lyRating.lyRatingChart.tv4.text =
      RatingUtil.calculateRatingFor4(productData.productRating).toString() + " %"
    binding.layoutOverview.lyRating.lyRatingChart.tv3.text =
      RatingUtil.calculateRatingFor3(productData.productRating).toString() + " %"
    binding.layoutOverview.lyRating.lyRatingChart.tv2.text =
      RatingUtil.calculateRatingFor2(productData.productRating).toString() + " %"
    binding.layoutOverview.lyRating.lyRatingChart.tv1.text =
      RatingUtil.calculateRatingFor1(productData.productRating).toString() + " %"
  }


  private fun observers() {
    productObserver()
    viewModel.mainProductMut.observe(viewLifecycleOwner, Observer {
      it?.let {
        updateUI(it)
      }
    })

  }

  private fun addressChangeObserver() {
    viewModelAddress.addressChanged.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it) {
          AddressChanged.ADDRESS_CHANGED -> {
            productObserver()
          }
          AddressChanged.ADDRESS_UNCHANGED -> {

          }
        }
      }
    })
  }

  private fun productObserver() {
    productID?.let { it ->
      viewModel.getProductDetails(it).observe(viewLifecycleOwner, Observer { result ->
        result?.let {
          when (result.status) {
            Result.Status.SUCCESS -> {
              result.data?.data?.let { it1 ->
                viewModel.mainProductMut.value = it1
              }
              hideLoading()
            }
            Result.Status.ERROR -> {
              hideLoading()
              result.message?.let { it1 ->
                AlertManager.showErrorMessage(
                  requireActivity(),
                  it1
                )
              }
            }
            Result.Status.LOADING -> {
              showLoading()
            }
          }
        }
      })
    }
    slug?.let {
      viewModel.getProductDetails(it).observe(viewLifecycleOwner, Observer { result ->
        result?.let {
          when (result.status) {
            Result.Status.SUCCESS -> {
              result.data?.data?.let { it1 ->
                viewModel.mainProductMut.value = it1
              }
              hideLoading()
            }
            Result.Status.ERROR -> {
              hideLoading()
              result.message?.let { it1 ->
                AlertManager.showErrorMessage(
                  requireActivity(),
                  it1
                )
              }
            }
            Result.Status.LOADING -> {
              showLoading()
            }
          }
        }
      })
    }

  }


  override fun getLayoutRes(): Int {
    return R.layout.fragment_product
  }

  override fun permissionGranted(requestCode: Int) {
  }

  override fun onClickBacK() {
    findNavController().navigateUp()
  }

  override fun onClickSearch() {
    try {
      requireActivity().findNavController(R.id.fragment).navigate(R.id.nav_search)
    } catch (e: Exception) {

    }

  }

  override fun onClickClearFilter() {

  }

  override fun onCLickAdd() {
    viewModel.plusQTY()
    productDetailsGlobal?.productDetails?.let {
      viewModel.appManager.offersManagers.addCheckProduct(
        requireActivity(), it, viewModel.qty.get()?.toInt()!!
      )
    }

  }

  override fun onCLickMinus() {
    viewModel.minusQTY()
    productDetailsGlobal?.productDetails?.let {
      viewModel.appManager.offersManagers.addCheckProduct(
        requireActivity(), it, viewModel.qty.get()?.toInt()!!
      )
    }
  }

  override fun onCLickAddToCart() {
    productDetailsGlobal?.let {
      viewModel.appManager.offersManagers.addProduct(
        requireActivity(), it.productDetails, viewModel.qty.get()?.toInt()!!
      )
    }
  }

  override fun onCLickViewAllReviews() {
    viewModel.productId = productID.toString()
    findNavController().navigate(R.id.productReviewsSheet)
  }

  override fun onCLickAddAllToCart() {
    for (item in viewModel.addedProducts) {
      viewModel.appManager.offersManagers.addProduct(requireActivity(), item)
    }
  }

  override fun onClickAddToWishList() {
    productDetailsGlobal?.let {
      viewModel.appManager.wishListManager.selectUnselected(
        it.productDetails
      )
      viewModel.isInWishList.value = viewModel.isInWishList.value != true


    }
  }

  override fun onClickShare() {
    val productLink =
      ConstantsUtil.BASE_URL_WEB + "product/" + "${productDetailsGlobal?.productDetails?.slug}"
    IntentAction.sendTextToOtherApps(requireActivity(), productLink)
  }

  override fun onClickBrand() {
    requireActivity().findNavController(R.id.fragment).navigate(
      R.id.nav_product_listing,
      ProductListFragment.getProductListingBundle(
        productDetailsGlobal?.productDetails?.brand?.name,
        productDetailsGlobal?.productDetails?.brand?.id, "brand"
      )
    )
  }

  override fun onClickOrderOutOfStock() {
    viewModel.appManager.storageManagers.selectedOutOfstockProductItem =
      (productDetailsGlobal?.productDetails ?: return)
    findNavController().navigate(R.id.orderOutOFStockFragment)
  }

  override fun onClickNotify() {
    productDetailsGlobal?.productDetails?.let { notifyAboutProduct(it) }
  }

  override fun onClickOrderOutOfStock(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.storageManagers.selectedOutOfstockProductItem = productDetails
    findNavController().navigate(R.id.orderOutOFStockFragment)
  }

  override fun onClickChangeAddress() {
    try {
      if (appManager.persistenceManager.isLoggedIn()) {
        addressContract.launch(true)
      } else {
        findNavController().navigate(R.id.nav_login_sheet)
      }
    } catch (e: Exception) {
    }
  }

  override fun onClickInfoDeliverType() {
    var title = ""
    var description = ""
    when (productDetailsGlobal?.productDetails?.availability?.getAvailability(
      viewModel.qty.get()?.toInt() ?: 0
    )) {
      ShipmentType.INSTANT -> {
        title = getString(R.string.instant_delivery)
        description = viewModel.appManager.storageManagers.config.instantInfo ?: ""
        AlertManager.showInfoAlertDialog(requireActivity(), title, description)
      }
      ShipmentType.EXPRESS -> {
        title = getString(R.string.express_delivery)
        description = viewModel.appManager.storageManagers.config.expressInfo ?: ""
        AlertManager.showInfoAlertDialog(requireActivity(), title, description)
      }
      else -> {
      }
    }

  }

  override fun onProductClicked(productDetails: ProductDetails, position: Int) {
    viewModel.position = position
    viewModel.previewProductMut.value = productDetails
    findNavController().navigate(R.id.productPreviewBottomSheet)
//    findNavController().navigate(
//      R.id.productFragment,
//      getBundle(
//        productID = productDetails.id
//      )
//    )
  }

  override fun onClickAddProduct(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.offersManagers.addProduct(requireActivity(), productDetails, position)
  }

  override fun onClickMinus(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.offersManagers.removeProduct(requireActivity(), productDetails, position)
  }

  override fun onClickPlus(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.offersManagers.addProduct(requireActivity(), productDetails, position)
  }

  override fun onClickWishList(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.wishListManager.selectUnselected(productDetails)
  }

  override fun onClickOverview() {
    viewModel.selectedDetails.value = ProductDetailSelectedState.OVERVIEW
    productDetailsGlobal?.productDetails?.shortDescription?.let {
      updateProductDescription(
        it
      )
    }
  }

  override fun onClickDetails() {
    viewModel.selectedDetails.value = ProductDetailSelectedState.DETAIL
    productDetailsGlobal?.productDetails?.description?.let { updateProductDescription(it) }
  }

  override fun onClickMoreInfo() {
    viewModel.selectedDetails.value = ProductDetailSelectedState.MORE_INFO
    productDetailsGlobal?.productDetails?.inventory?.sku?.let {
      val string = "SKU: $it"
      updateProductDescription(string)
    }
  }

  override fun addAddOnProduct(position: Int, productDetails: ProductDetails) {
    viewModel.changAddOns(productDetails)
    addOnProductAdapter.setItems(position, viewModel.addedProducts)

  }

  override fun onClickNotify(productDetails: ProductDetails, position: Int) {
    notifyAboutProduct(productDetails)
  }

  private fun notifyAboutProduct(productDetails: ProductDetails) {
    viewModel.notifyProduct(productDetails).observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            hideLoading()
            it.data?.message?.let { it1 ->
              AlertManager.showSuccessMessage(
                requireActivity(),
                it1
              )
            }
          }
          Result.Status.ERROR -> {
            it.message?.let { it1 -> AlertManager.showErrorMessage(requireActivity(), it1) }
            hideLoading()
          }
          Result.Status.LOADING -> {
            showLoading()
          }
        }
      }
    })
  }


}
