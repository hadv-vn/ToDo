package com.example.todo.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.todo.domain.model.CategoryAppearance

/** FR-22 (docs/Task_Category_ToDo.md mục 6.2) — 8 ô màu + 8 emoji cố định, chọn đúng 1 mỗi loại. */
@Composable
fun CategoryAppearancePicker(
    selectedColor: String,
    selectedIcon: String,
    onColorSelected: (String) -> Unit,
    onIconSelected: (String) -> Unit
) {
    Text("Màu:", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryAppearance.COLORS.forEach { colorHex ->
            val isSelected = colorHex == selectedColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color = Color(colorHex.toColorInt()), shape = CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelected(colorHex) }
            )
        }
    }
    Text("Icon:", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryAppearance.ICONS.forEach { icon ->
            val isSelected = icon == selectedIcon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onIconSelected(icon) },
                contentAlignment = Alignment.Center
            ) {
                Text(icon, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
