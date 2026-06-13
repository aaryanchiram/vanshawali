package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.viewmodel.FamilyViewModel

sealed class Screen(val route: String) {
    object Tree : Screen("tree")
    object Directory : Screen("directory")
    object Stats : Screen("stats")
    object AddEdit : Screen("add_edit/{memberId}?fatherId={fatherId}&motherId={motherId}&spouseId={spouseId}") {
        fun createRoute(memberId: Int, fatherId: Int? = null, motherId: Int? = null, spouseId: Int? = null): String {
            return "add_edit/$memberId" +
                    "?fatherId=${fatherId ?: ""}" +
                    "&motherId=${motherId ?: ""}" +
                    "&spouseId=${spouseId ?: ""}"
        }
    }
}

@Composable
fun MainNavigationView(
    viewModel: FamilyViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isHindi by viewModel.isHindi.collectAsState()

    // Observe backstack entry to hide navigation bar on the edit screen!
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Tree.route, Screen.Directory.route, Screen.Stats.route)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Tree Map Item
                    NavigationBarItem(
                        selected = currentRoute == Screen.Tree.route,
                        onClick = {
                            if (currentRoute != Screen.Tree.route) {
                                navController.navigate(Screen.Tree.route) {
                                    popUpTo(Screen.Tree.route) { inclusive = false }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "Tree") },
                        label = { Text(if (isHindi) "वृक्ष मानचित्र" else "Tree Map") },
                        modifier = Modifier.testTag("nav_tab_tree")
                    )

                    // Directory Item
                    NavigationBarItem(
                        selected = currentRoute == Screen.Directory.route,
                        onClick = {
                            if (currentRoute != Screen.Directory.route) {
                                navController.navigate(Screen.Directory.route) {
                                    popUpTo(Screen.Tree.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.ContactPage, contentDescription = "Directory") },
                        label = { Text(if (isHindi) "निर्देशिका" else "Directory") },
                        modifier = Modifier.testTag("nav_tab_directory")
                    )

                    // Stats Item
                    NavigationBarItem(
                        selected = currentRoute == Screen.Stats.route,
                        onClick = {
                            if (currentRoute != Screen.Stats.route) {
                                navController.navigate(Screen.Stats.route) {
                                    popUpTo(Screen.Tree.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Stats") },
                        label = { Text(if (isHindi) "आँकड़े" else "Insights") },
                        modifier = Modifier.testTag("nav_tab_stats")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tree.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Interactive Map screen
            composable(Screen.Tree.route) {
                TreeViewScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = { mid, fId, mId, sId ->
                        navController.navigate(Screen.AddEdit.createRoute(mid, fId, mId, sId))
                    }
                )
            }

            // Search Filter Search List
            composable(Screen.Directory.route) {
                DirectoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = { mid ->
                        navController.navigate(Screen.AddEdit.createRoute(mid))
                    },
                    onFocusInTree = {
                        // Switch immediately back to the first tab (Tree) which is now focused
                        navController.navigate(Screen.Tree.route) {
                            popUpTo(Screen.Tree.route) { inclusive = false }
                        }
                    }
                )
            }

            // Calculations/Insights screen
            composable(Screen.Stats.route) {
                StatsScreen(
                    viewModel = viewModel
                )
            }

            // Create or edit screen
            composable(
                route = Screen.AddEdit.route,
                arguments = listOf(
                    navArgument("memberId") { type = NavType.IntType },
                    navArgument("fatherId") { 
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument("motherId") { 
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument("spouseId") { 
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val mId = backStackEntry.arguments?.getInt("memberId") ?: 0
                
                val fIdStr = backStackEntry.arguments?.getString("fatherId")
                val fId = fIdStr?.toIntOrNull()
                
                val mIdStr = backStackEntry.arguments?.getString("motherId")
                val moId = mIdStr?.toIntOrNull()
                
                val sIdStr = backStackEntry.arguments?.getString("spouseId")
                val sId = sIdStr?.toIntOrNull()

                AddEditMemberScreen(
                    viewModel = viewModel,
                    memberId = mId,
                    initialFatherId = fId,
                    initialMotherId = moId,
                    initialSpouseId = sId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
