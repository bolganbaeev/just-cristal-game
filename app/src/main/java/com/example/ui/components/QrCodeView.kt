package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.util.QrCodeGenerator

@Composable
fun QrCodeView(
    text: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(text) {
        QrCodeGenerator.generateQrMatrix(text)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp)
            .aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numModules = matrix.size
            val moduleSize = size.width / numModules

            for (row in 0 until numModules) {
                for (col in 0 until numModules) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = Color(0xFF0A0E17),
                            topLeft = Offset(col * moduleSize, row * moduleSize),
                            size = Size(moduleSize + 0.5f, moduleSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
