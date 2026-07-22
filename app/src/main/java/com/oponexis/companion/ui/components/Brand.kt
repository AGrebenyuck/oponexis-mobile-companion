package com.oponexis.companion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oponexis.companion.R

@Composable
fun OponexisMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.oponexis_logo),
        contentDescription = "Oponexis logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.clip(RoundedCornerShape(22)),
    )
}

@Composable
fun OponexisWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OponexisMark(Modifier.size(44.dp))
        Text(
            text = "OPONEXIS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
        )
        Spacer(Modifier.weight(1f))
    }
}
