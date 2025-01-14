package org.wikipedia.compose

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ComposeStyles {

    val H1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    )

    val H1_AppBar = H1.copy(lineHeight = 24.sp)

    val H1_Article = H1.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal
    )

    val H2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )

    val H3 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val H3_Button = H3.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium
    )

    val H3_Article = H3.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal
    )

    val P = H3.copy(fontWeight = FontWeight.Normal)

    val P_Article = P.copy(lineHeight = 28.sp)

    val H4 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 24.sp
    )

    val List = H4.copy(
        fontWeight = FontWeight.Normal
    )

    val Chip = H4.copy(
        fontWeight = FontWeight.Medium
    )

    val H5 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )

    val Small = H5.copy(fontWeight = FontWeight.Medium)
}
