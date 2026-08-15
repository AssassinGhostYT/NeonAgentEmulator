package com.neon.emulator.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeEditorScreen(
    activeFile: ProjectFile,
    projectName: String,
    onRenderUpdatedCode: (String) -> Unit
) {
    val context = LocalContext.current
    var codeContent by remember(activeFile.path) { mutableStateOf(activeFile.content) }
    var saveMessage by remember { mutableStateOf("") }

    val lines = codeContent.split("\n")
    val lineCount = lines.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(8.dp)
    ) {
        // 📌 1. HEADER CON LA RUTA COMPLETA DEL ARCHIVO REAL Y BOTÓN DE GUARDADO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📄 ${activeFile.path}",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                if (saveMessage.isNotEmpty()) {
                    Text(text = saveMessage, color = Color(0xFF10B981), fontSize = 10.sp)
                }
            }

            Button(
                onClick = {
                    activeFile.content = codeContent
                    val savedFile = StorageManager.saveFileToDevice(context, projectName, activeFile.name, codeContent)
                    if (savedFile != null) {
                        saveMessage = "✔️ Guardado en Disco Privado"
                    }
                    onRenderUpdatedCode(codeContent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Guardar", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar Cambios", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 📝 2. ÁREA DE CÓDIGO CON NUMERACIÓN DE LÍNEAS DE ESTILO CODEASSIST
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {
            // Columna de Números de Línea (Estilo CodeAssist JetBrains Mono)
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF161B22))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount.coerceAtLeast(1)) {
                    Text(
                        text = "$i",
                        color = Color(0xFF6E7681),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Campo de Texto del Editor de Código
            OutlinedTextField(
                value = codeContent,
                onValueChange = { codeContent = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = LocalTextStyle.current.copy(
                    color = Color(0xFFE6EDE3),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF21262D)
                )
            )
        }
    }
}
