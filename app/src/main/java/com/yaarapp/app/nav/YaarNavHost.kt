package com.yaarapp.app.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaarapp.app.ui.components.YaarBottomBar
import com.yaarapp.app.ui.screens.AddProductScreen
import com.yaarapp.app.ui.screens.CartScreen
import com.yaarapp.app.ui.screens.ChatScreen
import com.yaarapp.app.ui.screens.CertifyShopScreen
import com.yaarapp.app.ui.screens.ConfigureAdCampaignScreen
import com.yaarapp.app.ui.screens.KkiapayCheckoutScreen
import com.yaarapp.app.ui.screens.LoginScreen
import com.yaarapp.app.ui.screens.MarketplaceScreen
import com.yaarapp.app.ui.screens.MyAdsScreen
import com.yaarapp.app.ui.screens.MyShopScreen
import com.yaarapp.app.ui.screens.NotificationsScreen
import com.yaarapp.app.ui.screens.OnboardingLocationScreen
import com.yaarapp.app.ui.screens.PlansScreen
import com.yaarapp.app.ui.screens.ProductDetailScreen
import com.yaarapp.app.ui.screens.ProfileScreen
import com.yaarapp.app.ui.screens.SearchScreen
import com.yaarapp.app.ui.screens.SecureAccountScreen
import com.yaarapp.app.ui.screens.SelectProductToPromoteScreen
import com.yaarapp.app.ui.screens.ShopPublicScreen
import com.yaarapp.app.ui.screens.SignUpScreen
import com.yaarapp.app.ui.screens.SplashScreen
import com.yaarapp.app.ui.screens.TermsAndConditionsScreen
import com.yaarapp.app.viewmodel.YaarViewModel
import com.yaarapp.app.viewmodel.YaarViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun YaarNavHost(viewModelFactory: YaarViewModelFactory) {
    val navController: NavHostController = rememberNavController()
    val viewModel: YaarViewModel = viewModel(factory = viewModelFactory)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()

    val mainTabs = listOf(Routes.PROFILE, Routes.MY_SHOP, Routes.MARKETPLACE)
    val showBottomBar = currentRoute in mainTabs

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                composable(Routes.SPLASH) {
                    SplashScreen(onFinished = {
                        val destination = if (!termsAccepted) {
                            Routes.TERMS
                        } else {
                            when {
                                viewModel.currentUser.value?.firebaseUid == null && viewModel.currentUser.value != null -> Routes.SECURE_ACCOUNT
                                viewModel.currentUserId.value != null -> Routes.MARKETPLACE
                                else -> Routes.LOGIN
                            }
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    })
                }

                composable(Routes.TERMS) {
                    TermsAndConditionsScreen(
                        viewModel = viewModel,
                        onAccepted = {
                            val destination = when {
                                viewModel.currentUser.value?.firebaseUid == null && viewModel.currentUser.value != null -> Routes.SECURE_ACCOUNT
                                viewModel.currentUserId.value != null -> Routes.MARKETPLACE
                                else -> Routes.LOGIN
                            }
                            navController.navigate(destination) {
                                popUpTo(Routes.TERMS) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoggedIn = {
                            navController.navigate(Routes.MARKETPLACE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onGoToSignUp = { navController.navigate(Routes.ONBOARDING_LOCATION) }
                    )
                }
                // Premier écran du parcours d'inscription : choix du pays puis de la ville.
                composable(Routes.ONBOARDING_LOCATION) {
                    OnboardingLocationScreen(
                        viewModel = viewModel,
                        onContinue = { navController.navigate(Routes.SIGNUP) }
                    )
                }
                composable(Routes.SECURE_ACCOUNT) {
                    SecureAccountScreen(
                        viewModel = viewModel,
                        onDone = {
                            navController.navigate(Routes.MARKETPLACE) {
                                popUpTo(Routes.SECURE_ACCOUNT) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.SIGNUP) {
                    SignUpScreen(
                        viewModel = viewModel,
                        onSignedUp = {
                            navController.navigate(Routes.MARKETPLACE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onGoToLogin = { navController.popBackStack(Routes.LOGIN, inclusive = false) },
                        onEditLocation = { navController.popBackStack() }
                    )
                }

                composable(Routes.MARKETPLACE) {
                    MarketplaceScreen(
                        viewModel = viewModel,
                        onProductClick = { product ->
                            navController.navigate(Routes.productDetail(product.id))
                        },
                        onCartClick = { navController.navigate(Routes.CART) },
                        onSearchClick = { navController.navigate(Routes.SEARCH) }
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onProductClick = { product ->
                            navController.navigate(Routes.productDetail(product.id))
                        }
                    )
                }
                composable(
                    route = Routes.PRODUCT_DETAIL,
                    arguments = listOf(navArgument("productId") { type = NavType.IntType })
                ) { entry ->
                    val productId = entry.arguments?.getInt("productId") ?: 0
                    ProductDetailScreen(
                        productId = productId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onViewShop = { shopId -> navController.navigate(Routes.shopPublic(shopId)) },
                        onChatSupplier = { navController.navigate(Routes.chat(productId)) }
                    )
                }
                composable(
                    route = Routes.CHAT,
                    arguments = listOf(navArgument("productId") { type = NavType.IntType })
                ) { entry ->
                    val productId = entry.arguments?.getInt("productId") ?: 0
                    val products by viewModel.allProducts.collectAsStateWithLifecycle()
                    val p = products.firstOrNull { it.id == productId }
                    var chatShop by remember(productId) { mutableStateOf<com.yaarapp.app.data.Shop?>(null) }
                    LaunchedEffect(p?.id) { chatShop = p?.let { viewModel.getShop(it.shopId) } }
                    if (p != null && chatShop != null) {
                        ChatScreen(product = p, shop = chatShop!!, viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                }
                composable(
                    route = Routes.SHOP_PUBLIC,
                    arguments = listOf(navArgument("shopId") { type = NavType.IntType })
                ) { entry ->
                    val shopId = entry.arguments?.getInt("shopId") ?: 0
                    ShopPublicScreen(
                        shopId = shopId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onProductClick = { product ->
                            navController.navigate(Routes.productDetail(product.id))
                        }
                    )
                }
                composable(Routes.CART) {
                    CartScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }

                composable(Routes.MY_SHOP) {
                    MyShopScreen(
                        viewModel = viewModel,
                        onAddProduct = { navController.navigate(Routes.ADD_PRODUCT) },
                        onSeePlans = { navController.navigate(Routes.PLANS) },
                        onPromoteProducts = { navController.navigate(Routes.SELECT_PRODUCT_TO_PROMOTE) },
                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
                    )
                }
                composable(Routes.ADD_PRODUCT) {
                    AddProductScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(Routes.PLANS) {
                    val shop by viewModel.myShop.collectAsStateWithLifecycle()
                    PlansScreen(
                        alreadyUpgraded = (shop?.extraProductSlots ?: 0) > 0,
                        onBack = { navController.popBackStack() },
                        onSubscribe = {
                            viewModel.requestProductCapacityUpgrade()
                            navController.navigate(Routes.KKIAPAY_CHECKOUT)
                        }
                    )
                }
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.SELECT_PRODUCT_TO_PROMOTE) {
                    SelectProductToPromoteScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onProductSelected = { navController.navigate(Routes.CONFIGURE_AD_CAMPAIGN) }
                    )
                }
                composable(Routes.CONFIGURE_AD_CAMPAIGN) {
                    ConfigureAdCampaignScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onConfirmed = { navController.navigate(Routes.KKIAPAY_CHECKOUT) }
                    )
                }
                composable(Routes.KKIAPAY_CHECKOUT) {
                    KkiapayCheckoutScreen(
                        viewModel = viewModel,
                        onCancel = {
                            viewModel.cancelPendingPayment()
                            navController.popBackStack(Routes.MY_SHOP, inclusive = false)
                        },
                        onSuccess = {
                            viewModel.onPaymentSuccess {
                                navController.popBackStack(Routes.MY_SHOP, inclusive = false)
                            }
                        }
                    )
                }
                composable(Routes.MY_ADS) {
                    MyAdsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.CERTIFY_SHOP) {
                    CertifyShopScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onContinueToPayment = { navController.navigate(Routes.KKIAPAY_CHECKOUT) }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onLoggedOut = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onCertifyShop = { navController.navigate(Routes.CERTIFY_SHOP) },
                        onMyAds = { navController.navigate(Routes.MY_ADS) }
                    )
                }
            }
        }
        if (showBottomBar) {
            YaarBottomBar(
                currentRoute = currentRoute,
                cartItemCount = cartItemCount,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.MARKETPLACE) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}
