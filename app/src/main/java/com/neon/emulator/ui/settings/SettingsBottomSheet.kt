package com.neon.emulator.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.emulator.model.PhoneModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    statusText: String,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    selectedModel: PhoneModel,
    onModelSelected: (PhoneModel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "⚙️ Configuración del Emulador & Dispositivo",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Nombre del Proyecto Activo:", color = Color.White, fontSize = 12.sp)
            OutlinedTextField(
                value = projectName,
                onValueChange = onProjectNameChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Servidor Agente (Antigravity IP):", color = Color.White, fontSize = 12.sp)
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Seleccionar Modelo de Dispositivo:", color = Color.White, fontSize = 12.sp)

            PhoneModel.values().forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelSelected(model) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedModel == model),
                        onClick = { onModelSelected(model) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0EA5E9))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = model.displayName, color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar y Aplicar", color = Color.White)
            }
        }
    }
}
