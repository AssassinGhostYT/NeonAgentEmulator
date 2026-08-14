# 🤖 Neon Agent Emulator (NAE)

Emulador de pruebas en tiempo real para Android diseñado para integrarse con la IA.

## 🚀 Características
- **Live Preview Canvas:** WebView con capacidad de recarga en tiempo real.
- **Servidor Ktor Embebido:** Escucha en el puerto `8080` para recibir comandos REST/HTTP.
- **GitHub Robot CI/CD:** Compilación automatizada de APK con GitHub Actions en cada commit.

## 🛠️ Endpoints API del Agente
- `GET /status`: Ver estado del servidor.
- `POST /api/render`: Enviar código HTML/JS/CSS para renderizar al instante.
- `POST /api/command`: Ejecutar comandos (`load_url:https://...`, `reload`, `eval_js:alert('hola')`).
