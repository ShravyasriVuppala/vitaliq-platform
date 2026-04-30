# VitalIQ MCP Server

Node.js MCP (Model Context Protocol) server that exposes VitalIQ fitness APIs to Claude Desktop.

## Architecture
Claude Desktop ←→ (stdio) ←→ Node.js MCP Server ←→ Spring Boot API

- **Transport:** stdio (no network overhead, fast startup)
- **Authentication:** API Key (X-API-Key header)
- **Base API:** http://localhost:8080

## Setup

### 1. Install Dependencies

```bash
cd mcp-server
npm install
```

### 2. Configure Environment

```bash
cp .env.example .env
```

Edit `.env` and add your VitalIQ API key:
VITALIQ_API_URL=http://localhost:8080
VITALIQ_API_KEY=vk_test_your_key_here

### 3. Generate API Key (if you don't have one)

Start your Spring Boot backend and use the API key generation endpoint:

```bash
POST http://localhost:8080/api/auth/api-keys/generate
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "Claude MCP Server",
  "scope": "workouts,nutrition"
}
```

Copy the returned `key` value to `.env`.

## Available Tools

### 1. `get_exercises`
List all available exercises with their IDs, categories, and equipment types.

**No parameters required.**

### 2. `log_workout`
Log a completed workout with exercises and performance data.

**Parameters:**
- `name` (string, required): Workout name
- `startTime` (ISO datetime, required): When workout started
- `endTime` (ISO datetime, required): When workout ended
- `exercises` (array, required): List of exercises with sets
- `notes` (string, optional): Overall workout notes

**Example:**
```json
{
  "name": "Chest and Triceps",
  "startTime": "2024-04-28T10:00:00",
  "endTime": "2024-04-28T11:00:00",
  "exercises": [
    {
      "exerciseId": "uuid-here",
      "sets": [
        {
          "type": "WEIGHTED",
          "reps": 10,
          "weightLbs": 185,
          "equipmentType": "BARBELL"
        }
      ]
    }
  ]
}
```

### 3. `search_workouts`
Retrieve your workout history.

**No parameters required.**

### 4. `get_dashboard`
Get your fitness dashboard with weekly and monthly statistics.

**No parameters required.**

### 5. `generate_nutrition_plan`
Generate a personalized nutrition plan based on your profile and goals.

**Parameters:**
- `planDate` (YYYY-MM-DD, optional): Date for the plan (defaults to today)

## Running the Server

### Manual Start (for development)

```bash
node src/index.js
```

You should see:

🚀 VitalIQ MCP Server starting...
📡 API URL: http://localhost:8080
✅ VitalIQ MCP Server is running!
🔐 Connected with API Key: vk_test_abc...
📡 Listening for Claude Desktop connections...

Stop with `Ctrl+C`.

### Auto-Start with Claude Desktop

Configure in `~/.config/claude/claude.json`:

```json
{
  "mcpServers": {
    "vitaliq": {
      "command": "node",
      "args": ["/Users/YOUR_USERNAME/Developer/vitaliq-platform/mcp-server/src/index.js"]
    }
  }
}
```

Replace `YOUR_USERNAME` with your actual macOS username.

Then:
- Open Claude Desktop → Server auto-starts
- Close Claude Desktop → Server auto-stops

## Testing

### 1. Start the Backend
```bash
cd ~/Developer/vitaliq-platform
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 2. Start the MCP Server (manually)
```bash
cd mcp-server
node src/index.js
```

### 3. Use in Claude Desktop

"What exercises are available?"
→ Uses get_exercises tool
"Log my chest workout: 3 sets of bench press, 185 lbs, 10 reps each"
→ Uses log_workout tool
"Show me my recent workouts"
→ Uses search_workouts tool
"What's my fitness summary?"
→ Uses get_dashboard tool
"Generate my nutrition plan"
→ Uses generate_nutrition_plan tool

## API Key Scopes

The API key used must have these scopes:
- `workouts` - Access to: log workout, search workouts, view dashboard
- `nutrition` - Access to: generate nutrition plan

Create an API key with both scopes:
```json
{
  "name": "Claude MCP Server",
  "scope": "workouts,nutrition"
}
```

## Troubleshooting

### "VITALIQ_API_KEY environment variable is not set"
- Check `.env` file exists in `mcp-server/` directory
- Verify `VITALIQ_API_KEY=...` line is present
- Restart the server after editing `.env`

### "Error: connect ECONNREFUSED 127.0.0.1:8080"
- Spring Boot backend is not running
- Start it with: `mvn spring-boot:run -Dspring-boot.run.profiles=local`

### "403 Forbidden" errors
- API key doesn't have required scope
- Regenerate with `workouts` and `nutrition` scopes

## Architecture Details

### Request Flow
Claude: "Log my workout"
↓
MCP Server receives: tools/call with log_workout
↓
handleLogWorkout() executes
↓
POST /api/workouts with X-API-Key header
↓
Spring Boot validates API key scope
↓
Workout logged in PostgreSQL
↓
Elasticsearch indexed (via Kafka)
↓
Response sent to Claude

### Files

- `src/index.js` - MCP server implementation
- `.env.example` - Example configuration
- `.env` - Actual configuration (gitignored)
- `package.json` - Dependencies

## References

- [MCP Specification](https://modelcontextprotocol.io/)
- [VitalIQ Backend](../backend/README.md)
- [Claude Desktop Docs](https://claude.ai/docs)