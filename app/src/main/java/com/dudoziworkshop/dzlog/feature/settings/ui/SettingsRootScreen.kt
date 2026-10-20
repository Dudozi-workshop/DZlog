package com.dudoziworkshop.dzlog.feature.settings.ui

import android.app.Activity
import com.dudoziworkshop.dzlog.feature.table.policy.BillingEntitlementRuntime
import com.dudoziworkshop.dzlog.feature.table.policy.MonetizationRolloutConfig
import com.dudoziworkshop.dzlog.feature.table.policy.PlayPurchaseEvent
import com.dudoziworkshop.dzlog.feature.table.policy.SaveRuleTier
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.feature.settings.policy.SettingsAction
import com.dudoziworkshop.dzlog.feature.settings.policy.applySettingsAction
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenImport: () -> Unit = {},
) {
    var showPermissions by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (showPermissions) {
        com.dudoziworkshop.dzlog.ui.navigation.PermissionManagementScreen { showPermissions = false }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(initial = AppSettings.Default)
    var showCaptureFeedbackSettings by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(false)
    }
    if (showCaptureFeedbackSettings) {
        CaptureFeedbackSettingsScreen(onBack = { showCaptureFeedbackSettings = false })
        return
    }
    var isQualityExpanded by remember { mutableStateOf(false) }
    var isQualitySaving by remember { mutableStateOf(false) }
    val version = remember(context) { buildAppVersionLabel(context) }
    val proTier by BillingEntitlementRuntime.tier.collectAsState()
    val purchaseEvent by BillingEntitlementRuntime.event.collectAsState()


    fun selectQuality(mode: Int) {
        val selectedMode = PhotoQualityMode.from(mode)
        if (isQualitySaving) return
        if (settings.photoQualityMode == selectedMode) {
            isQualityExpanded = false
            return
        }
        isQualitySaving = true
        scope.launch {
            try {
                applySettingsAction(context, SettingsAction.PhotoQualityModeChanged(selectedMode))
                isQualityExpanded = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, "사진 품질을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                isQualitySaving = false
            }
        }
    }

    fun changeAssistShutter(enabled: Boolean) {
        scope.launch {
            try {
                applySettingsAction(context, SettingsAction.AssistShutterEnabledChanged(enabled))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, "보조 셔터 설정을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = isQualityExpanded) { isQualityExpanded = false }

    SettingsRootContent(
        onBack = onBack,
        qualityMode = settings.photoQualityMode.v,
        assistShutterEnabled = settings.assistShutterEnabled,
        undoSavePathWarningEnabled = settings.undoSavePathWarningEnabled,
        isQualityExpanded = isQualityExpanded, isQualitySaving = isQualitySaving, version = version,
        onToggleQuality = { isQualityExpanded = !isQualityExpanded },
        onSelectQuality = ::selectQuality,
        onChangeAssistShutter = ::changeAssistShutter,
        onChangeUndoWarning = { enabled -> scope.launch { AppSettingsStore.setUndoSavePathWarningEnabled(context, enabled) } },
        onOpenPermissions = { showPermissions = true },
        onOpenCaptureFeedback = { showCaptureFeedbackSettings = true },
        onOpenCredits = onOpenCredits, onOpenGuide = onOpenGuide,
        onOpenExport = onOpenExport, onOpenImport = onOpenImport,
        proSection = {
                if (MonetizationRolloutConfig.purchasesEnabled) {
                    SettingsSection(title = "DZlog Pro") {
                        SettingsGroup {
                            SettingsActionRow(
                                icon = Icons.Default.CheckCircle,
                                title = if (proTier == SaveRuleTier.VERIFIED_PRO)
                                    "Pro 이용 중" else "Pro 구매",
                                subtitle = if (proTier == SaveRuleTier.VERIFIED_PRO)
                                    "파일명·저장경로 최대 5개 사용 가능"
                                else (purchaseEvent as? PlayPurchaseEvent.ProductReady)
                                    ?.displayedPrice?.let { "$it · 한 번 구매" }
                                    ?: "Google Play에서 구매가격 확인",
                                onClick = {
                                    if (proTier != SaveRuleTier.VERIFIED_PRO &&
                                        !BillingEntitlementRuntime.startPurchase(context as Activity)) {
                                        Toast.makeText(context,
                                            "Pro 상품 연결을 확인해 주세요.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                trailing = {
                                    Icon(Icons.Default.ChevronRight, null, tint = DDZColor.IconMuted)
                                },
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Default.FileDownload,
                                title = "구매 복원",
                                subtitle = "기존 Google Play 구매 내역 다시 확인",
                                onClick = { BillingEntitlementRuntime.restorePurchases() },
                                trailing = {
                                    Icon(Icons.Default.ChevronRight, null, tint = DDZColor.IconMuted)
                                },
                            )
                        }
                    }
                }

        },
    )
}

private fun buildAppVersionLabel(context: android.content.Context): String {
    val pkg = context.packageManager
    val info = runCatching { pkg.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = info?.versionName ?: "-"
    val code = if (Build.VERSION.SDK_INT >= 28) info?.longVersionCode else {
        @Suppress("DEPRECATION")
        info?.versionCode?.toLong()
    }
    return "버전 $version (${code ?: "-"})"
}
