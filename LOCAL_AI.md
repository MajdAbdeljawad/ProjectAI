# Local AI setup

FitCoach uses Ollama by default. No OpenAI API key is required.

## Install Ollama on Windows

```powershell
winget install Ollama.Ollama
```

Restart PowerShell after installation, then download the default model:

```powershell
ollama pull llama3.2
```

Ollama normally starts its local server automatically. Verify it:

```powershell
Invoke-RestMethod http://localhost:11434/api/tags
```

The backend uses:

- Provider: `ollama`
- Model: `llama3.2:1b`
- Endpoint: `http://localhost:11434/v1/chat/completions`

## Run the application

Start Docker Desktop, then from the repository root:

```powershell
docker compose up --build
```

Open `http://localhost:4200`, complete the questionnaire, and generate the plan.

## Use another local model

```powershell
ollama pull mistral
```

Set this in the root `.env` file:

```env
AI_PROVIDER=ollama
AI_MODEL=mistral
```

## Optional OpenAI mode

```env
AI_PROVIDER=openai
AI_API_KEY=your-key
AI_ENDPOINT=https://api.openai.com/v1/chat/completions
AI_MODEL=gpt-4o-mini
```
