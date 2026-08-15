package com.neon.emulator.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.emulator.model.ProjectItem
import com.neon.emulator.ui.editor.ProjectFile
import com.neon.emulator.ui.editor.flattenFiles

@Composable
fun FileExplorerDrawer(
    projectsList: List<ProjectItem>,
    activeProject: ProjectItem?,
    onProjectSelected: (ProjectItem) -> Unit,
    onFileSelected: (ProjectFile) -> Unit,
    onDeleteFile: (ProjectItem, ProjectFile) -> Unit,
    onDeleteProject: (ProjectItem) -> Unit,
    onCreateNewProject: () -> Unit,
    onClose: () -> Unit
) {
    var expandedProjectIds by remember { mutableStateOf(setOf<String>()) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(285.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📦 Proyectos del Usuario", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Button(
                onClick = onCreateNewProject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Proyecto", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Nuevo Proyecto Vacio", fontSize = 11.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (projectsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay proyectos creados.\nToca '+ Nuevo Proyecto' arriba.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn {
                    items(projectsList) { proj ->
                        val isExpanded = expandedProjectIds.contains(proj.id)
                        val isCurrentActive = (activeProject != null && proj.id == activeProject.id)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // Fila del Proyecto
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onProjectSelected(proj)
                                        expandedProjectIds = if (isExpanded) {
                                            expandedProjectIds - proj.id
                                        } else {
                                            expandedProjectIds + proj.id
                                        }
                                    },
                                color = if (isCurrentActive) Color(0xFF1E293B) else Color(0xFF070A13),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isCurrentActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isCurrentActive) Color(0xFF38BDF8) else Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = proj.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 🗑️ Botón Eliminar Proyecto Completo
                                    IconButton(
                                        onClick = { onDeleteProject(proj) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Proyecto", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Archivos del Proyecto
                            if (isExpanded) {
                                Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                                    flattenFiles(proj.rootFolder).forEach { file ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!file.isDirectory) {
                                                        onFileSelected(file)
                                                    }
                                                }
                                                .padding(vertical = 4.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = if (file.isDirectory) Color(0xFF38BDF8) else Color(0xFFEC4899),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = file.name, color = Color.White, fontSize = 11.sp)
                                            }

                                            // 🗑️ Botón Eliminar Archivo Individual
                                            if (!file.isDirectory) {
                                                IconButton(
                                                    onClick = { onDeleteFile(proj, file) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar Archivo", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
