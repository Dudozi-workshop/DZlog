package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_TUTORIAL_OFFER_PENDING
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.navigation.AppRoot
import com.dudoziworkshop.dzlog.ui.navigation.RequiredPermissionsGate
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Introduction and permission status are independent entry decisions. */
@Composable
fun OnboardingEntryGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<OnboardingInstallStage?>(null) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var changing by remember { mutableStateOf(false) }
    var completedCameraDestination by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(retry) {
        error = false
        stage = null
        try {
            stage = withContext(Dispatchers.IO) {
                OnboardingInstallState.loadOrInitialize(context)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        }
    }

    when {
        error -> Column(
            Modifier.fillMaxSize().background(DDZColor.Background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("시작 정보를 확인할 수 없어요.", color = DDZColor.TextPrimary,
                style = DDZTypography.ScreenTitle)
            Text("사용자 기록은 변경하지 않았어요. 다시 시도해 주세요.",
                color = DDZColor.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
            DDZButton("다시 시도", onClick = { retry++ }, modifier = Modifier.fillMaxWidth())
        }
        stage == null -> Column(
            Modifier.fillMaxSize().background(DDZColor.Background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = DDZColor.Primary)
            Text("앱 시작 정보를 확인하고 있어요", style = DDZTypography.Body,
                color = DDZColor.TextSecondary, modifier = Modifier.padding(top = 16.dp))
        }
        stage == OnboardingInstallStage.INTRO -> OnboardingFlowScreen(
            busy = changing,
            onChoose = {},
            onSkip = {},
            introOnly = true,
            onIntroComplete = {
                if (!changing) {
                    changing = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                OnboardingInstallState.save(context, OnboardingInstallStage.SETUP)
                            }
                            stage = OnboardingInstallStage.SETUP
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            changing = false
                        }
                    }
                }
            },
        )
        stage == OnboardingInstallStage.SETUP -> RequiredPermissionsGate(onboarding = true) {
            OnboardingStartupGate(
                startWithStarterSelection = true,
                offerOnboarding = true,
                onInitialSetupComplete = { openCamera ->
                    withContext(Dispatchers.IO) {
                        // Mark only newly completed onboarding; updates and returning
                        // installations must never receive this invitation.
                        context.dataStore.edit { it[KEY_TUTORIAL_OFFER_PENDING] = true }
                        OnboardingInstallState.save(context, OnboardingInstallStage.COMPLETE)
                    }
                    completedCameraDestination = false
                    stage = OnboardingInstallStage.COMPLETE
                },
            )
        }
        else -> RequiredPermissionsGate(onboarding = false) {
            if (completedCameraDestination != null) {
                AppRoot(startOnCamera = completedCameraDestination == true)
            } else {
                // Returning users never restart the introduction or starter wizard.
                OnboardingStartupGate(offerOnboarding = false)
            }
        }
    }
}
