# Diário Estoico

App Android de leitura diária baseado no livro *O Diário Estoico* de Ryan Holiday e Stephen Hanselman. Apresenta automaticamente a meditação do dia com uma UX inspirada em livros clássicos.

## Funcionalidades

- **Leitura diária** — abre direto na meditação do dia (366 entradas)
- **Navegação entre dias** — avance ou volte entre as meditações com setas e botão "Hoje"
- **Favoritos** — salve meditações completas ou selecione trechos do texto para guardar frases
- **Widget** — adicione à área de trabalho para ver a frase do dia sem abrir o app
- **Notificações** — configure um horário diário para receber um lembrete de leitura
- **Tema claro/escuro** — segue a preferência do sistema com paleta de cores otimizada para leitura

## Tecnologias

| Camada | Biblioteca |
|--------|-----------|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance API |
| Persistência | DataStore Preferences + Gson |
| Agendamento | WorkManager |
| Fonte | Lora (Google Fonts) |

## Requisitos

- Android 8.0+ (API 26)
- JDK 17
- Android SDK com `compileSdk = 35`

## Como compilar

```bash
# Clone o repositório
git clone https://github.com/tboschese/diarioestoico.git
cd diarioestoico

# Compile o APK de debug
JAVA_HOME=/caminho/para/jdk17 ./gradlew assembleDebug

# O APK gerado estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

## Estrutura do projeto

```
app/src/main/
├── assets/
│   └── entries.json          # 366 meditações extraídas do livro
├── java/com/diarioestoico/app/
│   ├── data/
│   │   ├── DailyEntry.kt
│   │   ├── EntryRepository.kt
│   │   ├── FavoritesRepository.kt
│   │   ├── NotificationPreferences.kt
│   │   └── SavedPhrase.kt
│   ├── notifications/
│   │   ├── BootReceiver.kt
│   │   ├── DailyNotificationWorker.kt
│   │   └── NotificationScheduler.kt
│   ├── ui/
│   │   ├── DailyReadingScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   ├── components/
│   │   │   ├── NotificationDialog.kt
│   │   │   └── StoicTextToolbar.kt
│   │   └── theme/
│   ├── widget/
│   │   ├── DailyWidget.kt
│   │   └── DailyWidgetReceiver.kt
│   └── MainActivity.kt
└── res/
    └── font/                 # Lora Regular, Italic, Bold
```

## Aviso

Este app foi desenvolvido para uso pessoal. O conteúdo das meditações pertence aos autores Ryan Holiday e Stephen Hanselman (*O Diário Estoico*, Portfolio-Penguin).

---

Desenvolvido por Thiago Boschese para uso pessoal.
