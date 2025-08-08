package live.mehiz.mpvkt.ui.player.controls.components.panels

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.presentation.components.OutlinedNumericChooser
import live.mehiz.mpvkt.ui.player.controls.CARDS_MAX_WIDTH
import live.mehiz.mpvkt.ui.player.controls.panelCardsColors
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import kotlin.math.round
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

@Composable
fun SubtitleDelayPanel(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<SubtitlesPreferences>()

  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(MaterialTheme.spacing.medium),
  ) {
    val delayControlCard = createRef()

    var affectedSubtitle by remember { mutableStateOf(SubtitleDelayType.Primary) }
    var delay by remember { mutableIntStateOf((MPVLib.getPropertyDouble("sub-delay") * 1000).roundToInt()) }
    var secondaryDelay by remember {
      mutableIntStateOf((MPVLib.getPropertyDouble("secondary-sub-delay") * 1000).roundToInt())
    }
    var speed by remember { mutableFloatStateOf(MPVLib.getPropertyDouble("sub-speed").toFloat()) }
    LaunchedEffect(speed) {
      if (speed in 0.1f..1f) MPVLib.setPropertyDouble("sub-speed", speed.toDouble())
    }
    LaunchedEffect(delay, secondaryDelay) {
      val finalDelay = (if (affectedSubtitle == SubtitleDelayType.Secondary) secondaryDelay else delay) / 1000.0
      when (affectedSubtitle) {
        SubtitleDelayType.Primary -> MPVLib.setPropertyDouble("sub-delay", finalDelay)
        SubtitleDelayType.Secondary -> MPVLib.setPropertyDouble("secondary-sub-delay", finalDelay)
        else -> {
          MPVLib.setPropertyDouble("sub-delay", finalDelay)
          MPVLib.setPropertyDouble("secondary-sub-delay", finalDelay)
        }
      }
    }
    LaunchedEffect(affectedSubtitle) {
      secondaryDelay = (
        MPVLib.getPropertyDouble(
          if (affectedSubtitle == SubtitleDelayType.Both) "sub-delay" else "secondary-sub-delay"
        ) * 1000
        ).toInt()
      delay = (MPVLib.getPropertyDouble("sub-delay") * 1000).toInt()
    }
    SubtitleDelayCard(
      delay = if (affectedSubtitle == SubtitleDelayType.Secondary) secondaryDelay else delay,
      onDelayChange = {
        if (affectedSubtitle == SubtitleDelayType.Secondary) {
          secondaryDelay = it
        } else {
          delay = it
        }
      },
      speed = speed,
      onSpeedChange = { speed = round(it * 1000) / 1000f },
      affectedSubtitle = affectedSubtitle,
      onTypeChange = { affectedSubtitle = it },
      onApply = {
        preferences.defaultSubDelay.set(delay)
        if (speed in 0.1f..10f) preferences.defaultSubSpeed.set(speed)
      },
      onReset = {
        delay = 0
        secondaryDelay = 0
        speed = 1f
      },
      onClose = onDismissRequest,
      modifier = Modifier.constrainAs(delayControlCard) {
        linkTo(parent.top, parent.bottom, bias = 0.8f)
        end.linkTo(parent.end)
      },
    )
  }
}

@Composable
fun SubtitleDelayCard(
  delay: Int,
  onDelayChange: (Int) -> Unit,
  speed: Float,
  onSpeedChange: (Float) -> Unit,
  affectedSubtitle: SubtitleDelayType,
  onTypeChange: (SubtitleDelayType) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DelayCard(
    delay = delay,
    onDelayChange = onDelayChange,
    onApply = onApply,
    onReset = onReset,
    title = {
      SubtitleDelayTitle(
        affectedSubtitle = affectedSubtitle,
        onClose = onClose,
        onTypeChange = onTypeChange,
      )
    },
    extraSettings = {
      when (affectedSubtitle) {
        SubtitleDelayType.Primary -> {
          OutlinedNumericChooser(
            label = { Text(stringResource(R.string.player_sheets_sub_delay_card_speed)) },
            value = speed,
            onChange = onSpeedChange,
            max = 10f,
            step = .01f,
            min = .1f,
          )
        }

        else -> {}
      }
    },
    delayType = DelayType.Subtitle,
    modifier = modifier,
  )
}

enum class SubtitleDelayType(
  @StringRes val title: Int,
) {
  Primary(R.string.player_sheets_sub_delay_subtitle_type_primary),
  Secondary(R.string.player_sheets_sub_delay_subtitle_type_secondary),
  Both(R.string.player_sheets_sub_delay_subtitle_type_primary_and_secondary),
}

@Suppress("LambdaParameterInRestartableEffect")
@Composable
fun DelayCard(
  delay: Int,
  onDelayChange: (Int) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  title: @Composable () -> Unit,
  delayType: DelayType,
  modifier: Modifier = Modifier,
  extraSettings: @Composable ColumnScope.() -> Unit = {},
) {
  Card(
    modifier = modifier
      .widthIn(max = CARDS_MAX_WIDTH)
      .animateContentSize(),
    colors = panelCardsColors(),
  ) {
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.smaller,
        ),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      title()

      OutlinedNumericChooser(
        label = { Text(stringResource(R.string.player_sheets_sub_delay_card_delay)) },
        value = delay,
        onChange = onDelayChange,
        step = 50,
        min = Int.MIN_VALUE,
        max = Int.MAX_VALUE,
        suffix = { Text(stringResource(R.string.generic_unit_ms)) },
      )

      Column(
        modifier = Modifier.animateContentSize(),
      ) { extraSettings() }

      var isDirectionPositive by remember { mutableStateOf<Boolean?>(null) }

      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        var timerStart by remember { mutableStateOf<Long?>(null) }
        var finalDelay by remember { mutableIntStateOf(delay) }

        LaunchedEffect(isDirectionPositive) {
          if (isDirectionPositive == null) {
            onDelayChange(finalDelay)
            return@LaunchedEffect
          }
          finalDelay = delay
          timerStart = System.currentTimeMillis()
          val startingDelay: Int = finalDelay
          while (isDirectionPositive != null && timerStart != null) {
            val elapsed = System.currentTimeMillis() - timerStart!!
            finalDelay = startingDelay + (if (isDirectionPositive!!) elapsed else -elapsed).toInt()
            delay(20)
          }
        }

        Button(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) {
              delayType == DelayType.Audio
            } else {
              null
            }
          },
          modifier = Modifier
            .weight(1f)
            .focusable(),
          enabled = isDirectionPositive != (delayType == DelayType.Audio),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_audio_sound_heard
              } else {
                R.string.player_sheets_sub_delay_subtitle_voice_heard
              },
            ),
          )
        }

        Button(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) {
              delayType != DelayType.Audio
            } else {
              null
            }
          },
          modifier = Modifier
            .weight(1f)
            .focusable(),
          enabled = isDirectionPositive != (delayType == DelayType.Subtitle),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_sound_sound_spotted
              } else {
                R.string.player_sheets_sub_delay_subtitle_text_seen
              },
            ),
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        Button(
          onClick = onApply,
          modifier = Modifier
            .weight(1f)
            .focusable(),
          enabled = isDirectionPositive == null,
        ) {
          Text(stringResource(R.string.player_sheets_delay_set_as_default))
        }

        FilledIconButton(
          onClick = onReset,
          enabled = isDirectionPositive == null,
          modifier = Modifier.focusable(),
        ) {
          Icon(Icons.Default.Refresh, null)
        }
      }
    }
  }
}

@Composable
fun SubtitleDelayTitle(
  affectedSubtitle: SubtitleDelayType,
  onClose: () -> Unit,
  onTypeChange: (SubtitleDelayType) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      stringResource(R.string.player_sheets_sub_delay_card_title),
      style = MaterialTheme.typography.headlineMedium,
    )

    var showDropDownMenu by remember { mutableStateOf(false) }
    var isDropdownFocused by remember { mutableStateOf(false) }

    // Fixed dropdown with proper TV remote support
    Row(
      modifier = Modifier
        .focusable()
        .onFocusChanged { focusState ->
          isDropdownFocused = focusState.isFocused
        }
        .onKeyEvent { keyEvent ->
          if ((keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) &&
            keyEvent.type == KeyEventType.KeyUp
          ) {
            showDropDownMenu = true
            true
          } else {
            false
          }
        }
        .clickable { showDropDownMenu = true }
        .then(
          if (isDropdownFocused) {
            Modifier.background(
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
              RoundedCornerShape(4.dp)
            )
          } else {
            Modifier
          }
        )
        .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
      Text(
        stringResource(affectedSubtitle.title),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      Icon(Icons.Default.ArrowDropDown, null)

      DropdownMenu(
        expanded = showDropDownMenu,
        onDismissRequest = { showDropDownMenu = false },
      ) {
        SubtitleDelayType.entries.forEach { delayType ->
          DropdownMenuItem(
            text = { Text(stringResource(delayType.title)) },
            onClick = {
              onTypeChange(delayType)
              showDropDownMenu = false
            },
          )
        }
      }
    }

    Spacer(Modifier.weight(1f))

    IconButton(
      onClick = onClose,
      modifier = Modifier.focusable()
    ) {
      Icon(
        Icons.Default.Close,
        null,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

enum class DelayType {
  Audio, Subtitle
}
