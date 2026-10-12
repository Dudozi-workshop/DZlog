package com.dudoziworkshop.dzlog.ui.onboarding

import android.widget.Toast
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.feature.table.policy.CatalogStartupRoute
import com.dudoziworkshop.dzlog.feature.table.policy.OnboardingInstallProbe
import com.dudoziworkshop.dzlog.feature.table.policy.OnboardingStarterCommitResult
import com.dudoziworkshop.dzlog.feature.table.policy.commitFirstOnboardingStarter
import com.dudoziworkshop.dzlog.feature.table.policy.inspectTableTemplateCatalogStartup
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.navigation.AppRoot
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Prevent the legacy default-template creation from running before startup eligibility is known. */
@Composable
fun OnboardingStartupGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkVersion by remember { mutableIntStateOf(0) }
    var route by remember { mutableStateOf<CatalogStartupRoute?>(null) }
    var firstCamera by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(checkVersion) {
        route = null
        route = try {
            withContext(Dispatchers.IO) {
                val existingImages = OnboardingInstallProbe.existingDzlogImagesOrNull(context)
                inspectTableTemplateCatalogStartup(
                    context = context,
                    verifiedNoExistingMedia = existingImages == false,
                    knownExistingMedia = existingImages == true,
                    verifiedFreshInstall = OnboardingInstallProbe.isUnupdatedFirstInstall(context),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CatalogStartupRoute.RECOVERY_REQUIRED
        }
    }

    when {
        firstCamera -> AppRoot(startOnCamera = true)
        route == CatalogStartupRoute.OFFER_ONBOARDING -> {
            fun apply(id: String) {
                if (busy) return
                busy = true
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            commitFirstOnboardingStarter(context, id, CatalogStartupRoute.OFFER_ONBOARDING)
                        }
                        when (result) {
                            is OnboardingStarterCommitResult.Created,
                            is OnboardingStarterCommitResult.AlreadyCompleted -> {
                                firstCamera = true
                            }
                            else -> {
                                Toast.makeText(context, "양식을 적용하지 못했습니다. 데이터를 확인해 주세요.",
                                    Toast.LENGTH_LONG).show()
                                checkVersion++
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        Toast.makeText(context, "양식 저장 중 문제가 발생했습니다.",
                            Toast.LENGTH_LONG).show()
                    } finally {
                        busy = false
                    }
                }
            }
            OnboardingFlowScreen(
                busy = busy, onChoose = ::apply,
                onSkip = { apply(OnboardingStarterCatalog.DEFAULT_ID) },
            )
        }
        route == CatalogStartupRoute.RECOVERY_REQUIRED -> Column(
            Modifier.fillMaxSize().background(DDZColor.Background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("기존 기록 정보를 안전하게 불러오지 못했습니다.",
                color = DDZColor.TextPrimary)
            Text("자동 초기화하지 않았습니다. 다시 시도하거나 백업 상태를 확인해 주세요.",
                color = DDZColor.TextSecondary, modifier = Modifier.padding(vertical = 20.dp))
            DDZButton(text = "다시 시도", onClick = { checkVersion++ },
                modifier = Modifier.fillMaxWidth())
        }
        route == null -> Column(
            Modifier.fillMaxSize().background(DDZColor.Background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = DDZColor.Primary)
            Text("기록 정보를 확인하고 있어요", modifier = Modifier.padding(top = 16.dp),
                color = DDZColor.TextSecondary)
        }
        else -> AppRoot()
    }
}
