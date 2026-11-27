package com.example.toastytraveltools.view.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii from the Figma mockups (see design-tokens.md).
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), // text fields
    small = RoundedCornerShape(10.dp),      // buttons, guidance boxes
    medium = RoundedCornerShape(14.dp),     // cards
    large = RoundedCornerShape(16.dp),      // confirmation panels, sheets
    extraLarge = RoundedCornerShape(20.dp), // dialogs
)
