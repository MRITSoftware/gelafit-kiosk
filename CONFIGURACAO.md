# Guia de Configuração

## 1. Configurar Credenciais do Supabase

Edite o arquivo `app/src/main/java/com/gelafit/kioskcontroller/SupabaseConfig.kt`:

```kotlin
private const val SUPABASE_URL = "https://seu-projeto.supabase.co"
private const val SUPABASE_KEY = "sua-chave-anon-key"
```

**Onde encontrar:**
- Acesse o [Supabase Dashboard](https://app.supabase.com)
- Vá em **Settings** > **API**
- Copie a **URL** do projeto
- Copie a **anon/public key**

## 2. Build Local do APK

### Pré-requisitos
- Android Studio instalado
- JDK 17 ou superior
- Android SDK instalado

### Build via linha de comando

```bash
# Windows
gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

O APK estará em: `app/build/outputs/apk/release/app-release.apk`

### Build via Android Studio
1. Abra o projeto no Android Studio
2. Vá em **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**
3. Aguarde o build completar
4. Clique em **locate** para encontrar o APK

## 3. Build Automático via GitHub Actions

O repositório está configurado com GitHub Actions para build automático:

1. Faça push de qualquer commit para a branch `main`
2. O workflow será executado automaticamente
3. Após o build, vá em **Actions** > clique no workflow executado
4. Baixe o APK na seção **Artifacts**

## 4. Instalação no Dispositivo

### Via ADB
```bash
adb install app-release.apk
```

### Via transferência manual
1. Copie o APK para o dispositivo Android
2. Abra o arquivo no dispositivo
3. Permita instalação de fontes desconhecidas se necessário
4. Instale o app

## 5. Configuração Inicial no Dispositivo

1. Abra o app "Gelafit Kiosk Controller"
2. Selecione o aplicativo que será mantido em modo kiosk
3. Conceda as permissões necessárias:
   - **Apps sobrepostos** (Overlay)
   - **Uso de acesso** (Usage Access)
4. Inicie o monitoramento

## 6. Estrutura da Tabela `devices` no Supabase

Certifique-se de que sua tabela tenha esta estrutura:

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

## 7. Controle Remoto

Para ativar o modo kiosk remotamente:

```sql
UPDATE devices 
SET kiosk_mode = true 
WHERE device_id = 'device-id-do-android';
```

O app escolhido será aberto automaticamente em até 5 segundos!
