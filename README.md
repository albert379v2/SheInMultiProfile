# SheIn Multi-Profile Browser

APK Android para navegar en SheIn con multiples perfiles aislados.

## Caracteristicas
- Perfiles aislados con WebView.setDataDirectorySuffix()
- Proxy HTTP/SOCKS4/SOCKS5 por perfil
- User-Agent personalizado por perfil
- Persistencia de sesiones
- Diseno Material 3 oscuro

## Requisitos
- Android 9.0+ (API 28+)
- 8GB RAM recomendado

## Compilar con GitHub Actions
1. Sube este repo a GitHub
2. Ve a Actions > Build APK
3. Toca "Run workflow"
4. Espera 3-5 minutos
5. Descarga el APK desde Artifacts

## Uso
1. Crea un perfil (nombre, user-agent, proxy opcional)
2. Toca el perfil para abrir SheIn
3. Cada perfil tiene cookies y sesion independientes
4. Usa el menu para limpiar cookies de un perfil
