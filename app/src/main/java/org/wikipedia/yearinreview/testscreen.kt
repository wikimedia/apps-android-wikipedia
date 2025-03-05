package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Composable
fun TestScreen(){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Hello World",
            fontSize = 30.sp,
            modifier = Modifier.align(alignment=Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
fun PreviewTestScreen() {
    TestScreen()
}