# Gelafit Kiosk Controller

App Android que monitora e controla o modo kiosk de qualquer aplicativo através da tabela `devices` no Supabase.

## Funcionalidades

- ✅ **Seleção de aplicativo**: Escolha qualquer app instalado para modo kiosk
- ✅ **Monitoramento remoto**: Monitora o campo `kiosk_mode` da tabela `devices` no Supabase
- ✅ **Controle automático**: Quando `kiosk_mode = true`, mantém o app selecionado fixo na tela
- ✅ **Abertura automática**: Quando você setar `kiosk_mode = TRUE` no banco, o app escolhido abre automaticamente
- ✅ **Restauração automática**: Restaura automaticamente o app se ele for minimizado ou fechado
- ✅ **Funciona em background**: Roda continuamente mesmo com o app fechado
- ✅ **Inicia no boot**: Inicia automaticamente quando o dispositivo é ligado
- ✅ **Verificação de permissões**: Mostra quais permissões são necessárias e guia o usuário a concedê-las
- ✅ **Controle remoto**: Ative/desative o modo kiosk remotamente via banco de dados
- ✅ **Serviço em foreground**: Roda continuamente em background com notificação
- ✅ **WorkManager**: Backup caso o serviço seja morto pelo sistema

## Como Funciona

1. **Seleção do aplicativo**: O usuário escolhe qual app será mantido em modo kiosk
2. **Verificação de permissões**: O app verifica e guia o usuário a conceder as permissões necessárias
3. **Registro do dispositivo**: O app obtém o `device_id` único do Android (ANDROID_ID) e registra na tabela `devices` do Supabase
4. **Monitoramento em background**: 
   - Verifica periodicamente (a cada 5 segundos) o campo `kiosk_mode` para o `device_id`
   - Funciona mesmo com o app fechado (serviço em foreground)
   - Inicia automaticamente quando o dispositivo é ligado (boot)
5. **Detecção de mudança**: Quando `kiosk_mode` muda de `false` para `true`:
   - **Abre automaticamente o app escolhido** (mesmo que não esteja rodando)
   - Traz o app para o foreground
6. **Controle contínuo**: Se `kiosk_mode = true`:
   - Mantém o app sempre em primeiro plano
   - Se o app for minimizado ou fechado, restaura automaticamente
   - Verifica a cada 5 segundos
7. **Liberação**: Se `kiosk_mode = false`, permite uso normal do dispositivo

## Configuração

### 1. Configurar Credenciais do Supabase

Edite o arquivo `app/src/main/java/com/gelafit/kioskcontroller/SupabaseConfig.kt`:

```kotlin
private const val SUPABASE_URL = "https://seu-projeto.supabase.co"
private const val SUPABASE_KEY = "sua-chave-anon-key"
```

### 2. Selecionar o Aplicativo

**Não é mais necessário configurar o package name!** O app agora permite escolher qualquer aplicativo instalado através da interface.

### 3. Estrutura da Tabela `devices`

Certifique-se de que sua tabela `devices` no Supabase tenha a seguinte estrutura:

```sql
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL UNIQUE,
    unit_name TEXT,
    registered_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT now(),
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    kiosk_mode BOOLEAN DEFAULT false
);
```

## Instalação

### 1. Build do APK

```bash
./gradlew assembleRelease
```

O APK estará em: `app/build/outputs/apk/release/app-release.apk`

### 2. Instalação no Dispositivo Android

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 3. Permissões Necessárias

O app precisa das seguintes permissões:
- **Internet** - Para conectar ao Supabase (concedida automaticamente)
- **Foreground Service** - Para rodar o serviço de monitoramento (concedida automaticamente)
- **Apps sobrepostos** (Overlay) - Para trazer apps para o foreground ⚠️ **Requer permissão manual**
- **Uso de acesso** (Usage Access) - Para verificar apps em execução ⚠️ **Requer permissão manual**

**O app possui uma tela dedicada que mostra o status de cada permissão e guia o usuário a concedê-las!**

## Uso

1. **Abra o app** "Gelafit Kiosk Controller"
2. **Selecione o aplicativo**: Clique em "Selecionar Aplicativo" e escolha qual app será mantido em modo kiosk
3. **Verifique as permissões**: Clique em "Verificar Permissões" e conceda todas as permissões necessárias
4. **Inicie o monitoramento**: Clique em "Iniciar Monitoramento"
5. O serviço começará a monitorar o campo `kiosk_mode` no Supabase para o seu `device_id`
6. **Pronto!** O serviço roda em background e:
   - Funciona mesmo com o app fechado
   - Inicia automaticamente quando o dispositivo é ligado
   - Abre o app escolhido automaticamente quando você setar `kiosk_mode = TRUE`

### Controle Remoto

O sistema funciona totalmente em background. Você pode controlar remotamente:

**Para ativar o modo kiosk e abrir o app automaticamente:**

```sql
UPDATE devices 
SET kiosk_mode = true 
WHERE device_id = 'device-id-do-android';
```

Quando você setar `kiosk_mode = TRUE`, o sistema:
- Detecta a mudança automaticamente (em até 5 segundos)
- Abre o app escolhido automaticamente
- Mantém o app fixo na tela

**Para desativar:**

```sql
UPDATE devices 
SET kiosk_mode = false 
WHERE device_id = 'device-id-do-android';
```

**Importante:**
- O serviço roda em background mesmo com o app fechado
- Funciona após reiniciar o dispositivo (inicia automaticamente no boot)
- Não precisa manter o app aberto
- Verifica o banco de dados a cada 5 segundos

## Limitações

- O app precisa estar rodando em foreground para funcionar corretamente
- Algumas versões do Android podem ter restrições mais rígidas
- Para um controle mais absoluto, considere usar **Device Owner** ou **Lock Task Mode** (requer configuração adicional)

## Troubleshooting

### O app selecionado não está sendo mantido fixo

1. Verifique se um app foi selecionado na tela principal
2. Verifique se o app selecionado está instalado
3. Verifique se todas as permissões foram concedidas (use a tela "Verificar Permissões")
4. Verifique os logs: `adb logcat | grep KioskMonitorService`

### O serviço para de funcionar

- O serviço é configurado como `START_STICKY`, então deve reiniciar automaticamente
- O WorkManager também funciona como backup
- Verifique se a bateria não está otimizada para o app

## Desenvolvimento

### Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/gelafit/kioskcontroller/
│   │   ├── MainActivity.kt          # Tela principal
│   │   ├── SupabaseConfig.kt        # Configuração do Supabase
│   │   ├── service/
│   │   │   └── KioskMonitorService.kt  # Serviço de monitoramento
│   │   └── util/
│   │       └── DeviceUtils.kt       # Utilitários do dispositivo
│   └── res/
│       ├── layout/
│       │   └── activity_main.xml
│       └── values/
│           ├── strings.xml
│           └── colors.xml
```

## Licença

Este projeto é privado e de uso exclusivo da Gelafit.
