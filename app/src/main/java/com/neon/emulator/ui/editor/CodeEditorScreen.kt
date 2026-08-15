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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeEditorScreen(
    activeFile: ProjectFile,
    projectName: String,
    onRenderUpdatedCode: (String) -> Unit
) {
    var codeContent by remember(activeFile.path) { mutableStateOf(activeFile.content) }
    var saveMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "✏️ Editando: ${activeFile.name}",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (saveMessage.isNotEmpty()) {
                    Text(text = saveMessage, color = Color(0xFF10B981), fontSize = 10.sp)
                }
            }

            Button(
                onClick = {
                    activeFile.content = codeContent
                    // 1. Guardar físicamente en el disco del teléfono (/sdcard/NeonEmulatorProjects/)
                    val savedFile = StorageManager.saveFileToDevice(projectName, activeFile.name, codeContent)
                    if (savedFile != null) {
                        saveMessage = "Guardado en /sdcard/NeonEmulatorProjects/"
                    }
                    // 2. Aplicar cambio en tiempo real al emulador
                    onRenderUpdatedCode(codeContent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Download, contentDescription = "Guardar", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar en Disco", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = codeContent,
            onValueChange = { codeContent = it },
            modifier = Modifier.fillMaxSize(),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155)
            )
        )
    }
}
