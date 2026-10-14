package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.policy.CatalogStartupRoute
import com.dudoziworkshop.dzlog.feature.table.policy.OnboardingInstallProbe
import com.dudoziworkshop.dzlog.feature.table.policy.inspectTableTemplateCatalogStartup
import com.dudoziworkshop.dzlog.ui.navigation.RequiredPermissionsGate
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only eligibility check before introductions; recheck after permissions before any starter commit. */
@Composable
fun OnboardingEntryGate() {
    val context = LocalContext.current
    var route by remember { mutableStateOf<CatalogStartupRoute?>(null) }
    var introCompleted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(context) {
        route = try {
            withContext(Dispatchers.IO) {
                val existingMedia = OnboardingInstallProbe.existingDzlogImagesOrNull(context)
                inspectTableTemplateCatalogStartup(
                    context = context,
                    verifiedNoExistingMedia = existingMedia == false,
                    knownExistingMedia = existingMedia == true,
                    verifiedFreshInstall = OnboardingInstallProbe.isUnupdatedFirstInstall(context),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            CatalogStartupRoute.RECOVERY_REQUIRED
        }
    }

    when {
        route == null -> Column(
            Modifier.fillMaxSize().background(DDZColor.Background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = DDZColor.Primary)
            Text(
                "기록 정보를 확인하고 있어요",
                style = DDZTypography.Body,
                color = DDZColor.TextSecondary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        route == CatalogStartupRoute.OFFER_ONBOARDING && !introCompleted ->
            OnboardingFlowScreen(
                busy = false,
                onChoose = {},
                onSkip = {},
                introOnly = true,
                onIntroComplete = { introCompleted = true },
            )
        else -> RequiredPermissionsGate {
            // Permission grants may reveal pre-existing media from a prior install.
            // The startup route is independently re-evaluated before any template is created.
            OnboardingStartupGate(startWithStarterSelection = introCompleted)
        }
    }
}
