package pl.kijewski.synbee.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import pl.kijewski.synbee.R

// Material Symbols, the same icons as the hub's web interface. Names are the font's ligatures.
private val Symbols = FontFamily(Font(R.font.material_symbols))
private val NAME = Regex("^[a-z0-9_]{1,40}$")

@Composable
fun Symbol(name: String, modifier: Modifier = Modifier, size: TextUnit = 24.sp, color: Color = LocalContentColor.current) {
    Text(if (NAME.matches(name)) name else "bolt", modifier = modifier, fontFamily = Symbols, fontSize = size, color = color, maxLines = 1)
}

/** A colour from the hub ("#rrggbb"), or a calm blue when it isn't one. */
fun hubColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF7AA7E0))
