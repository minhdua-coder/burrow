package com.burrow.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.burrow.app.auth.BiometricAuth
import com.burrow.app.ui.components.Toast
import com.burrow.app.ui.screens.BrowseScreen
import com.burrow.app.ui.screens.DeleteConfirmDialog
import com.burrow.app.ui.screens.LockScreen
import com.burrow.app.ui.screens.SearchScreen
import com.burrow.app.ui.screens.SheetContent
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurrowRoot(viewModel: BurrowViewModel) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    fun showBiometricPrompt() {
        if (activity != null && BiometricAuth.isAvailable(activity)) {
            BiometricAuth.authenticate(
                activity = activity,
                title = "Unlock Warren",
                onSuccess = { viewModel.onBiometricSuccess() },
            )
        }
    }

    LaunchedEffect(state.locked, state.biometricEnabled) {
        if (state.locked && state.biometricEnabled) showBiometricPrompt()
    }

    Box(Modifier.fillMaxSize()) {
        if (state.locked) {
            LockScreen(
                state = state,
                viewModel = viewModel,
                onBiometricClick = if (state.biometricEnabled) ::showBiometricPrompt else null,
            )
        } else {
            when (state.screen) {
                Screen.BROWSE -> BrowseScreen(state, viewModel)
                Screen.SEARCH -> SearchScreen(state, viewModel)
            }
        }
        Toast(state.toast)
    }

    if (state.sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Burrow.Bg,
        ) {
            SheetContent(state, viewModel)
        }
    }

    state.confirmDelete?.let { cd ->
        DeleteConfirmDialog(cd, viewModel)
    }
}
