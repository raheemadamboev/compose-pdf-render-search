package xyz.teamgravity.compose_pdf_render_search

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import xyz.teamgravity.compose_pdf_render_search.ui.theme.ComposepdfrendersearchTheme

class MainActivity : ComponentActivity() {

    private val converter: PdfConverter by lazy { PdfConverter(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposepdfrendersearchTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { padding ->
                    val scope = rememberCoroutineScope()
                    val searchEnabled = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM }
                    var uri by remember { mutableStateOf<Uri?>(null) }
                    var pages by remember { mutableStateOf<ImmutableList<Bitmap>>(persistentListOf()) }
                    var searchText by remember { mutableStateOf("") }
                    var searchResult by remember { mutableStateOf<ImmutableMap<Int, ImmutableList<RectF>>>(persistentMapOf()) }

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent(),
                        onResult = { uri = it }
                    )

                    LaunchedEffect(
                        key1 = uri,
                        block = {
                            uri?.let { notNullUri ->
                                pages = converter.convert(notNullUri)
                            }
                        }
                    )

                    if (uri == null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            Button(
                                onClick = {
                                    launcher.launch("application/pdf")
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.choose_pdf)
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1F)
                                    .fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = pages
                                ) { index, page ->
                                    AsyncImage(
                                        model = page,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(page.width.toFloat() / page.height.toFloat())
                                            .drawWithContent {
                                                drawContent()

                                                val result = searchResult[index]
                                                if (!result.isNullOrEmpty()) {
                                                    val scaleFactorX = size.width / page.width
                                                    val scaleFactorY = size.height / page.height

                                                    result.forEach { rect ->
                                                        val processedRect = RectF(
                                                            rect.left * scaleFactorX,
                                                            rect.top * scaleFactorY,
                                                            rect.right * scaleFactorX,
                                                            rect.bottom * scaleFactorY
                                                        )

                                                        drawRoundRect(
                                                            color = Color.Yellow.copy(alpha = 0.5F),
                                                            topLeft = Offset(
                                                                x = processedRect.left,
                                                                y = processedRect.top
                                                            ),
                                                            size = Size(
                                                                width = processedRect.width(),
                                                                height = processedRect.height()
                                                            ),
                                                            cornerRadius = CornerRadius(5.dp.toPx())
                                                        )
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )
                            Button(
                                onClick = {
                                    launcher.launch("application/pdf")
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.choose_another_pdf)
                                )
                            }
                            if (searchEnabled) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { value ->
                                        searchText = value
                                        scope.launch {
                                            searchResult = converter.search(value)
                                        }
                                    },
                                    trailingIcon = {
                                        if (searchText.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    searchText = ""
                                                    searchResult = persistentMapOf()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Clear,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.search_text)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(0.8F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}